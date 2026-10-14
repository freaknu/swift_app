#!/usr/bin/env python3
"""Run persisted fault/concurrency tests in disposable PostgreSQL; never reads service secrets."""
import json, os, secrets, subprocess, time, uuid
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build' / 'reliability-qa'
WORK.mkdir(parents=True, exist_ok=True)
name = 'swiftapp-refactor-qa-' + uuid.uuid4().hex[:10]
password = secrets.token_hex(24)
container = None
try:
    container = subprocess.check_output([
        'docker', 'run', '--detach', '--rm', '--name', name,
        '--label', 'swiftapp.qa=refactor-reliability', '--publish', '127.0.0.1::5432',
        '--env', 'POSTGRES_USER=swiftqa', '--env', 'POSTGRES_DB=swiftqa',
        '--env', 'POSTGRES_PASSWORD=' + password, 'postgres:16-alpine'
    ], text=True).strip()
    for _ in range(30):
        ready = subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'swiftqa', '-d', 'swiftqa'], capture_output=True)
        if ready.returncode == 0: break
        time.sleep(1)
    else: raise RuntimeError('Disposable PostgreSQL did not become ready')
    address = subprocess.check_output(['docker', 'port', container, '5432/tcp'], text=True).strip()
    port = address.rsplit(':', 1)[1]
    env = dict(os.environ)
    results = {}
    for service, tests in (
        ('paymentservice', 'PaymentProviderPostgresTest,PaymentOutboxPostgresTest'),
        ('riderservice', 'RideCompletionOutboxPostgresTest')
    ):
        log = WORK / ('refactor-postgres-' + service + '.log')
        with log.open('w') as output:
            result = subprocess.run([
                './mvnw', '-q', '-Dtest=' + tests,
                '-Dswift.qa.postgres.url=jdbc:postgresql://127.0.0.1:' + port + '/swiftqa',
                '-Dswift.qa.postgres.username=swiftqa', '-Dswift.qa.postgres.password=' + password, 'test'
            ], cwd=ROOT/service, env=env, stdout=output, stderr=subprocess.STDOUT)
        suites = []
        for test_name in tests.split(','):
            matches = list((ROOT/service/'target/surefire-reports').glob('TEST-*.' + test_name + '.xml'))
            if len(matches) != 1: raise RuntimeError('Expected a report for ' + test_name)
            suite = ET.parse(matches[0]).getroot()
            suites.append({'name': suite.attrib['name'], **{
                key: int(suite.attrib.get(key, 0)) for key in ('tests', 'failures', 'errors', 'skipped')
            }})
        executed = sum(s['tests'] - s['skipped'] for s in suites)
        gate_ok = result.returncode == 0 and executed > 0 and all(
            s['tests'] > 0 and s['skipped'] == 0 and s['failures'] == 0 and s['errors'] == 0 for s in suites)
        results[service] = {'exitCode': 0 if gate_ok else 1, 'log': str(log), 'suites': suites}
        print(service + ': ' + (str(executed) + ' tests passed' if gate_ok else 'FAILED'), flush=True)
        if not gate_ok: break
    (WORK/'refactor-postgres-results.json').write_text(json.dumps(results, indent=2)+'\n')
    raise SystemExit(0 if all(result['exitCode'] == 0 for result in results.values()) else 1)
finally:
    if container:
        subprocess.run(['docker', 'rm', '--force', container], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print('Disposable PostgreSQL cleaned up', flush=True)
