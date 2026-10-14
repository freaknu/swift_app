#!/usr/bin/env bash
# Local, disposable smoke tests. Never contacts SSH, Docker, or systemd.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_SCRIPT="$SCRIPT_DIR/../deploy.sh"
TEST_DIR="$(mktemp -d "${TMPDIR:-/tmp}/swift-deploy-test.XXXXXX")"
cleanup() { chmod -R u+w "$TEST_DIR"; rm -rf "$TEST_DIR"; }
trap cleanup EXIT
export DEPLOY_SCRIPT TEST_DIR

cat > "$TEST_DIR/harness.sh" <<'BASH'
#!/usr/bin/env bash
set -euo pipefail
scenario="$1"
source "$DEPLOY_SCRIPT"
ROOT_DIR="$TEST_DIR/$scenario"
DEPLOY_DIR="$ROOT_DIR/deployment"
EVENTS="$ROOT_DIR/events"
mkdir -p "$DEPLOY_DIR/releases" "$DEPLOY_DIR/runtime" "$DEPLOY_DIR/backups"
touch "$EVENTS"
require_runtime() { :; }
lock_deployment() { :; }
backup_database() { printf 'backup\n' >> "$EVENTS"; }
ensure_infrastructure() { printf 'infrastructure\n' >> "$EVENTS"; }
sudo() { printf 'systemctl %s\n' "$*" >> "$EVENTS"; }
readlink() {
    if [[ "${1:-}" == -f ]]; then
        python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$2"
    else command readlink "$@"; fi
}
mv() {
    if [[ "${1:-}" == -Tf ]]; then
        python3 -c 'import os,sys; os.replace(sys.argv[1],sys.argv[2])' "$2" "$3"
    else command mv "$@"; fi
}
build_service() {
    printf 'build %s\n' "$1" >> "$EVENTS"
    if [[ "$scenario" == build-failure && "$1" == userservice ]]; then return 1; fi
    mkdir -p "$ROOT_DIR/$1/target"
    printf 'new-%s\n' "$1" > "$ROOT_DIR/$1/target/app.jar"
}
wait_service() {
    printf 'ready %s\n' "$1" >> "$EVENTS"
    if [[ "$scenario" == readiness-failure || "$scenario" == first-failure ]]; then
        if [[ "$1" == paymentservice && ! -f "$ROOT_DIR/failed-once" ]]; then
            touch "$ROOT_DIR/failed-once"
            return 1
        fi
    fi
}
wait_registry() { printf 'registry\n' >> "$EVENTS"; }
wait_gateway() { printf 'gateway\n' >> "$EVENTS"; }

case "$scenario" in
    initial|first-failure|first-single) ;;
    *)
        mkdir "$DEPLOY_DIR/releases/previous"
        for service in "${SERVICE_ORDER[@]}"; do
            printf 'old-%s\n' "$service" > "$DEPLOY_DIR/releases/previous/$service.jar"
        done
        ln -s "$DEPLOY_DIR/releases/previous" "$DEPLOY_DIR/current"
        ;;
esac
case "$scenario" in
    single) main paymentservice ;;
    first-single) main paymentservice ;;
    *) main all ;;
esac
BASH

fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
assert_contains() { grep -Fq "$2" "$1" || fail "Missing '$2' in $1"; }

/bin/bash "$TEST_DIR/harness.sh" initial >/dev/null
initial="$TEST_DIR/initial/deployment/current"
[[ -L "$initial" && $(find -L "$initial" -name '*.jar' | wc -l | tr -d ' ') == 14 ]] || fail 'Initial deployment did not install 14 JARs.'
python3 - "$TEST_DIR/initial/events" <<'PY'
import sys
events=open(sys.argv[1]).read().splitlines()
assert sum(x.startswith('build ') for x in events)==14
assert events.index('backup')==14, events
assert events.index('infrastructure')>events.index('backup')
assert all(not x.startswith('systemctl') for x in events[:events.index('infrastructure')])
PY
printf 'PASS: all builds finish before infrastructure or systemd changes\n'

if /bin/bash "$TEST_DIR/harness.sh" build-failure >/dev/null 2>&1; then fail 'Build failure succeeded.'; fi
[[ $(readlink "$TEST_DIR/build-failure/deployment/current") == */previous ]] || fail 'Build failure changed current release.'
! grep -Eq 'backup|infrastructure|systemctl' "$TEST_DIR/build-failure/events" || fail 'Build failure touched running infrastructure.'
printf 'PASS: failed build leaves the previous release untouched\n'

if /bin/bash "$TEST_DIR/harness.sh" readiness-failure >/dev/null 2>&1; then fail 'Readiness failure succeeded.'; fi
[[ $(readlink "$TEST_DIR/readiness-failure/deployment/current") == */previous ]] || fail 'Readiness failure did not restore previous release.'
assert_contains "$TEST_DIR/readiness-failure/events" 'systemctl -n systemctl stop swiftapp@paymentservice.service'
assert_contains "$TEST_DIR/readiness-failure/events" 'gateway'
printf 'PASS: failed readiness restores and checks the previous JAR release\n'

if /bin/bash "$TEST_DIR/harness.sh" first-failure >/dev/null 2>&1; then fail 'First readiness failure succeeded.'; fi
[[ ! -e "$TEST_DIR/first-failure/deployment/current" && ! -L "$TEST_DIR/first-failure/deployment/current" ]] || fail 'Failed first deployment kept current link.'
assert_contains "$TEST_DIR/first-failure/events" 'systemctl -n systemctl stop swiftapp@eurekaserver.service'
printf 'PASS: a failed first deployment stops JVMs without deleting release/data\n'

/bin/bash "$TEST_DIR/harness.sh" single >/dev/null
assert_contains "$TEST_DIR/single/deployment/current/paymentservice.jar" new-paymentservice
assert_contains "$TEST_DIR/single/deployment/current/authservice.jar" old-authservice
[[ $(grep -c '^build ' "$TEST_DIR/single/events") == 1 ]] || fail 'Single deploy rebuilt siblings.'
[[ $(grep -c 'systemctl restart' "$TEST_DIR/single/events") == 1 ]] || fail 'Single deploy restarted siblings.'
printf 'PASS: a single-service release preserves all 13 sibling JARs\n'

if /bin/bash "$TEST_DIR/harness.sh" first-single >/dev/null 2>&1; then fail 'First single-service deploy succeeded.'; fi
[[ ! -s "$TEST_DIR/first-single/events" ]] || fail 'Rejected first single deploy built or changed services.'
printf 'PASS: first deployment requires the complete service set\n'

# Exercise the real local rsync implementation and mock only its SSH transport.
cat > "$TEST_DIR/push-harness.sh" <<'BASH'
#!/usr/bin/env bash
set -euo pipefail
source "$DEPLOY_SCRIPT"
ROOT_DIR="$TEST_DIR/push-source"
DEPLOY_DIR="$ROOT_DIR/deployment"
REMOTE_DIR=/home/ubuntu/swiftapp_backend
DESTINATION="$TEST_DIR/push-destination"
DEPLOY_HOST=16.4.23.48
DEPLOY_USER=ubuntu
SSH_KEY="$ROOT_DIR/key with spaces.pem"
mkdir -p "$ROOT_DIR/noticationservice/src/main/resources/static/notification" "$ROOT_DIR/deployment" "$ROOT_DIR/service" \
    "$DESTINATION/deployment/runtime" "$DESTINATION/deployment/releases/old"
printf 'fake-key' > "$SSH_KEY"
printf 'fake-credential' > "$ROOT_DIR/noticationservice/src/main/resources/static/notification/swift-app-service-account.json"
printf 'source' > "$ROOT_DIR/service/application.yaml"
printf 'script' > "$ROOT_DIR/deploy.sh"
printf 'script' > "$ROOT_DIR/deployment/bootstrap.sh"
printf 'old-config' > "$DESTINATION/deployment/runtime/service.env"
printf 'old-jar' > "$DESTINATION/deployment/releases/old/authservice.jar"
printf 'stale-source' > "$DESTINATION/stale.java"
ssh() {
    local arg remote_command=""
    printf '%s\n' "$*" >> "$TEST_DIR/push-ssh.log"
    for arg in "$@"; do remote_command="$arg"; done
    # Execute real directory/permission commands against disposable fixtures.
    # Never execute the bootstrap/deployment command.
    if [[ "$remote_command" != *'bash deployment/bootstrap.sh'* ]]; then
        /bin/bash -c "${remote_command//$REMOTE_DIR/$DESTINATION}"
    fi
}
rsync() {
    local arg args=()
    for arg in "$@"; do
        case "$arg" in
            "$DEPLOY_USER@$DEPLOY_HOST:$REMOTE_DIR/"*) arg="$DESTINATION/${arg#"$DEPLOY_USER@$DEPLOY_HOST:$REMOTE_DIR/"}" ;;
        esac
        args[${#args[@]}]="$arg"
    done
    command rsync "${args[@]}"
}
push_remote paymentservice
BASH
/bin/bash "$TEST_DIR/push-harness.sh" >/dev/null
destination="$TEST_DIR/push-destination"
assert_contains "$destination/deployment/runtime/service.env" old-config
assert_contains "$destination/deployment/releases/old/authservice.jar" old-jar
assert_contains "$destination/deployment/runtime/firebase-service-account.json" fake-credential
[[ ! -e "$destination/key with spaces.pem" && ! -e "$destination/stale.java" ]] || fail 'Push copied SSH key or retained stale source.'
[[ ! -e "$destination/noticationservice/src/main/resources/static/notification/swift-app-service-account.json" ]] || fail 'Push put credentials in the source tree.'
python3 - "$destination" <<'PY'
import os,stat,sys
root=sys.argv[1]
for path, mode in [('service',0o700),('service/application.yaml',0o600),('deployment/runtime/firebase-service-account.json',0o600)]:
    actual=stat.S_IMODE(os.stat(root+'/'+path).st_mode)
    assert actual==mode, (path, oct(actual), oct(mode))
PY
assert_contains "$TEST_DIR/push-ssh.log" 'bash deployment/bootstrap.sh'
assert_contains "$TEST_DIR/push-ssh.log" 'bash ./deploy.sh deploy paymentservice'
printf 'PASS: Bash 3 push protects runtime files, keys, credential placement and permissions\n'

if /bin/bash "$DEPLOY_SCRIPT" aiservice >/dev/null 2>&1; then fail 'Unconfigured AI service was accepted.'; fi
printf 'PASS: unconfigured AI scaffold cannot be deployed\n'
printf 'All 8 deployment smoke checks passed.\n'
