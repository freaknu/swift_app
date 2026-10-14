#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$repo_dir"

docker compose config --quiet
for script in build-services.sh start-infrastructure.sh start-services.sh stop-services.sh status.sh; do
    bash -n "$script"
done

for service in authservice apigateway driverservice riderservice paymentservice locationservice matchingservice websocketservice; do
    printf 'Testing %s\n' "$service"
    (
        cd "$repo_dir/$service"
        ./mvnw test
    )
done

printf 'SwiftApp ride-flow validation passed.\n'
