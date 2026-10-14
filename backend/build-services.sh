#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [[ -f "$repo_dir/.env" ]]; then
    set -a
    source "$repo_dir/.env"
    set +a
fi
if [[ "${1:-}" == "--skip-tests" ]]; then
    set -- -DskipTests
elif [[ "$#" -gt 0 ]]; then
    printf 'Usage: %s [--skip-tests]\n' "$0" >&2
    exit 2
fi

services=(eurekaserver apigateway authservice userservice driverservice
    locationservice matchingservice riderservice paymentservice websocketservice chatservice
    noticationservice ratingservice analyticalservice)

for service in "${services[@]}"; do
    printf 'Building %s\n' "$service"
    (
        cd "$repo_dir/$service"
        ./mvnw clean package "$@"
    )
done
