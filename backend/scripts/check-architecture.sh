#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if ! command -v rg >/dev/null 2>&1; then
    printf 'Architecture check requires ripgrep (rg), but it is not available.\n' >&2
    exit 2
fi

main_roots=()
for source_root in "$repo_root"/*/src/main/java; do
    [[ -d "$source_root" ]] && main_roots+=("$source_root")
done

if [[ "${#main_roots[@]}" -eq 0 ]]; then
    printf 'No production Java source roots were found.\n' >&2
    exit 2
fi

violations=0

report_matches() {
    local message="$1"
    shift
    local matches status

    set +e
    matches="$("$@" 2>&1)"
    status=$?
    set -e

    if [[ "$status" -gt 1 ]]; then
        printf 'Architecture checker command failed: %s\n%s\n' "$*" "$matches" >&2
        exit 2
    fi
    if [[ "$status" -eq 0 && -n "$matches" ]]; then
        printf '%s\n%s\n' "$message" "$matches"
        violations=1
    fi
}

report_matches \
    "Application code must not import infrastructure:" \
    rg -n '^import com\.swiftapp_backend\..*\.infrastructure\.' \
    "${main_roots[@]}" --glob '**/application/**/*.java'

report_matches \
    "Domain code must not import API, application, or infrastructure code:" \
    rg -n '^import com\.swiftapp_backend\..*\.(api|application|infrastructure)\.' \
    "${main_roots[@]}" --glob '**/domain/**/*.java'

report_matches \
    "Domain models must be persistence-framework neutral:" \
    rg -n '^import (jakarta\.persistence|org\.hibernate|org\.springframework\.data\.annotation)\.' \
    "${main_roots[@]}" --glob '**/domain/**/*.java'

report_matches \
    "API adapters must not import infrastructure implementations:" \
    rg -n '^import com\.swiftapp_backend\..*\.infrastructure\.' \
    "${main_roots[@]}" --glob '**/api/**/*.java'

report_matches \
    "Application code must use ports instead of Kafka, Redis, or mail clients:" \
    rg -n '^import org\.springframework\.(kafka|data\.redis|mail)\.' \
    "${main_roots[@]}" --glob '**/application/**/*.java'

report_matches \
    "Shared code must not depend on infrastructure implementations:" \
    rg -n '^import com\.swiftapp_backend\..*\.infrastructure\.' \
    "${main_roots[@]}" --glob '**/shared/**/*.java'

set +e
jpa_repository_files="$(rg -l \
    'extends[[:space:]]+([^<{]+,[[:space:]]*)*(JpaRepository|CrudRepository|ListCrudRepository|PagingAndSortingRepository|ReactiveCrudRepository|MongoRepository)[[:space:]]*<' \
    "${main_roots[@]}" --glob '*.java' 2>&1)"
jpa_status=$?
set -e
if [[ "$jpa_status" -gt 1 ]]; then
    printf 'Unable to scan Spring Data repositories:\n%s\n' "$jpa_repository_files" >&2
    exit 2
fi
if [[ "$jpa_status" -eq 0 ]]; then
    while IFS= read -r repository_file; do
        [[ -z "$repository_file" ]] && continue
        if [[ "$repository_file" != */infrastructure/persistence/repository/* ]]; then
            printf 'Spring Data repository is outside infrastructure/persistence/repository: %s\n' \
                "$repository_file"
            violations=1
        fi
    done <<< "$jpa_repository_files"
fi

required_business_layers=(api application domain infrastructure)
business_features=(
    'authservice:authentication'
    'userservice:user'
    'driverservice:driver'
    'riderservice:ride'
    'paymentservice:payment'
    'ratingservice:rating'
    'locationservice:location'
    'noticationservice:notification'
    'websocketservice:realtime'
)
for mapping in "${business_features[@]}"; do
    service="${mapping%%:*}"
    feature="${mapping#*:}"
    feature_root="$repo_root/$service/src/main/java/com/swiftapp_backend/$service/$feature"
    for layer in "${required_business_layers[@]}"; do
        if [[ ! -d "$feature_root/$layer" ]]; then
            printf 'Missing %s layer for %s feature: %s\n' "$layer" "$feature" "$feature_root/$layer"
            violations=1
        fi
    done
done

platform_layers=(
    'analyticalservice:analytics:application'
    'analyticalservice:analytics:domain'
    'analyticalservice:analytics:infrastructure'
    'apigateway:gateway:api'
    'apigateway:gateway:application'
    'apigateway:gateway:infrastructure'
    'eurekaserver:discovery:infrastructure'
)
for mapping in "${platform_layers[@]}"; do
    service="${mapping%%:*}"
    remainder="${mapping#*:}"
    feature="${remainder%%:*}"
    layer="${remainder#*:}"
    layer_root="$repo_root/$service/src/main/java/com/swiftapp_backend/$service/$feature/$layer"
    if [[ ! -d "$layer_root" ]]; then
        printf 'Missing %s layer for %s feature: %s\n' "$layer" "$feature" "$layer_root"
        violations=1
    fi
done

while IFS= read -r java_file; do
    relative="${java_file#*/src/main/java/}"
    expected_package="${relative%/*}"
    expected_package="${expected_package//\//.}"
    actual_package="$(sed -n 's/^package \([^;]*\);/\1/p' "$java_file" | head -n 1)"
    if [[ "$actual_package" != "$expected_package" ]]; then
        printf 'Package/path mismatch: %s (expected %s, found %s)\n' \
            "$java_file" "$expected_package" "${actual_package:-<none>}"
        violations=1
    fi

    public_type="$(sed -nE \
        's/^public[[:space:]]+((abstract|final|sealed|non-sealed)[[:space:]]+)?(class|interface|record|enum)[[:space:]]+([A-Za-z_$][A-Za-z0-9_$]*).*/\4/p' \
        "$java_file" | head -n 1)"
    expected_type="$(basename "$java_file" .java)"
    if [[ -n "$public_type" && "$public_type" != "$expected_type" ]]; then
        printf 'Public type/filename mismatch: %s (expected %s, found %s)\n' \
            "$java_file" "$expected_type" "$public_type"
        violations=1
    fi
done < <(find "${main_roots[@]}" -type f -name '*.java' | sort)

while IFS= read -r controller_file; do
    if ! rg -q '^import com\.swiftapp_backend\..*\.application\.' "$controller_file"; then
        printf 'API controller bypasses the application layer: %s\n' "$controller_file"
        violations=1
    fi
done < <(find "${main_roots[@]}" -type f -path '*/api/controller/*.java' | sort)

while IFS= read -r listener_file; do
    if rg -q '^[[:space:]]*@KafkaListener' "$listener_file" \
            && ! rg -q '^import com\.swiftapp_backend\..*\.application\.' "$listener_file"; then
        printf 'Kafka listener bypasses the application layer: %s\n' "$listener_file"
        violations=1
    fi
done < <(find "${main_roots[@]}" -type f -path '*/infrastructure/messaging/kafka/*.java' | sort)

if [[ "$violations" -ne 0 ]]; then
    exit 1
fi

printf 'Architecture boundary checks passed.\n'
