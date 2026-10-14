#!/usr/bin/env bash
# macOS Bash 3 can push; service management runs on the Linux EC2 host.
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$ROOT_DIR/deployment"
DEPLOY_HOST="${SWIFT_DEPLOY_HOST:-16.4.23.48}"
DEPLOY_USER="${SWIFT_DEPLOY_USER:-ubuntu}"
SSH_KEY="${SWIFT_DEPLOY_SSH_KEY:-$ROOT_DIR/swift_apppem.pem}"
REMOTE_DIR="${SWIFT_DEPLOY_REMOTE_DIR:-/home/ubuntu/swiftapp_backend}"
DOMAIN="${SWIFT_DEPLOY_DOMAIN:-swift-app.duckdns.org}"
READY_TIMEOUT="${SWIFT_DEPLOY_READY_TIMEOUT:-180}"
REGISTRY_TIMEOUT="${SWIFT_DEPLOY_REGISTRY_TIMEOUT:-120}"

SERVICE_ORDER=(eurekaserver authservice userservice driverservice locationservice
    matchingservice riderservice paymentservice websocketservice chatservice
    noticationservice ratingservice analyticalservice apigateway)
TARGETS=()
ROLLBACK_ACTIVE=0
PREVIOUS_RELEASE=""
NEW_RELEASE=""

say() { printf '%s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; return 1; }

usage() {
    cat <<'EOF'
Usage:
  ./deploy.sh push [all|SERVICE]   Sync source, then bootstrap/deploy on EC2
  ./deploy.sh all                 Build and deploy all 14 services on EC2
  ./deploy.sh deploy [all|SERVICE]
  ./deploy.sh SERVICE             Build and deploy one service
  ./deploy.sh start [all|SERVICE]  Start the currently staged JARs; no build
  ./deploy.sh status [all|SERVICE]
  ./deploy.sh stop [all|SERVICE]   Stop JVM services; keep infrastructure/data

Push overrides: SWIFT_DEPLOY_HOST, SWIFT_DEPLOY_USER, SWIFT_DEPLOY_SSH_KEY,
SWIFT_DEPLOY_REMOTE_DIR, SWIFT_DEPLOY_DOMAIN.

Each release retains all service JARs. Failed readiness restores the previous
JAR release; database upgrades are additive and are not rolled back.
EOF
}

validate_service() {
    local candidate="$1" service
    for service in "${SERVICE_ORDER[@]}"; do
        [[ "$candidate" != "$service" ]] || return 0
    done
    fail "Unknown service '$candidate'. aiservice is an unconfigured scaffold and is not deployed."
}

select_targets() {
    local selection="${1:-all}"
    if [[ "$selection" == all ]]; then
        TARGETS=("${SERVICE_ORDER[@]}")
    else
        validate_service "$selection" || return 1
        TARGETS=("$selection")
    fi
}

service_port() {
    case "$1" in
        eurekaserver) printf '8761' ;; authservice) printf '8081' ;;
        userservice) printf '8082' ;; driverservice) printf '8096' ;;
        locationservice) printf '8087' ;; matchingservice) printf '9005' ;;
        riderservice) printf '8083' ;; paymentservice) printf '9910' ;;
        websocketservice) printf '8085' ;; chatservice) printf '9004' ;;
        noticationservice) printf '8088' ;; ratingservice) printf '8086' ;;
        analyticalservice) printf '8089' ;; apigateway) printf '8080' ;;
        *) return 1 ;;
    esac
}

require_command() { command -v "$1" >/dev/null 2>&1 || fail "Required command missing: $1"; }

require_linux() {
    [[ "$(uname -s)" == Linux ]] || fail 'Run service commands on Linux EC2. Use ./deploy.sh push from macOS.'
}

require_runtime() {
    require_linux || return 1
    local command
    for command in java docker sudo systemctl curl flock gzip grep; do
        require_command "$command" || return 1
    done
    [[ "$READY_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || fail 'Invalid SWIFT_DEPLOY_READY_TIMEOUT.' || return 1
    [[ "$REGISTRY_TIMEOUT" =~ ^[1-9][0-9]*$ ]] || fail 'Invalid SWIFT_DEPLOY_REGISTRY_TIMEOUT.' || return 1
    java -version 2>&1 | grep -Eq 'version "21([.\"]|$)' || fail 'Java 21 is required.' || return 1
    sudo -n true || fail 'Passwordless sudo is required for systemd management.' || return 1
    docker info >/dev/null 2>&1 || fail 'Docker is not accessible to the deployment user.' || return 1
    docker compose version >/dev/null 2>&1 || fail 'Docker Compose v2 is required.' || return 1
    [[ -r "$DEPLOY_DIR/compose.ec2.yml" ]] || fail 'Missing deployment/compose.ec2.yml.' || return 1
    [[ -r "$DEPLOY_DIR/runtime/service.env" ]] || fail 'Missing deployment/runtime/service.env; run the host setup first.' || return 1
    sudo -n systemctl cat swiftapp@.service >/dev/null 2>&1 || fail 'Install swiftapp@.service before deploying.' || return 1
}

lock_deployment() {
    mkdir -p "$DEPLOY_DIR/runtime" "$DEPLOY_DIR/releases" "$DEPLOY_DIR/backups"
    chmod 700 "$DEPLOY_DIR/runtime" "$DEPLOY_DIR/backups"
    exec 9>"$DEPLOY_DIR/runtime/deploy.lock"
    flock -n 9 || fail 'Another deployment is running on this host.'
}

compose() { docker compose --env-file "$DEPLOY_DIR/runtime/service.env" -f "$ROOT_DIR/docker-compose.yml" -f "$DEPLOY_DIR/compose.ec2.yml" "$@"; }

postgres_ready() {
    docker exec postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d postgres' >/dev/null 2>&1
}

wait_postgres() {
    local elapsed=0
    until postgres_ready; do
        (( elapsed < READY_TIMEOUT )) || fail 'PostgreSQL did not become ready.' || return 1
        sleep 2
        elapsed=$((elapsed + 2))
    done
}

backup_database() {
    # Never recreate/remove volumes. An existing stopped container is started
    # only to back up its current data before any Compose/schema changes.
    if ! docker inspect postgres >/dev/null 2>&1; then
        say 'No existing PostgreSQL container; this is a fresh database installation.'
        return 0
    fi
    if [[ "$(docker inspect -f '{{.State.Running}}' postgres)" != true ]]; then
        docker start postgres >/dev/null || return 1
    fi
    wait_postgres || return 1
    local backup temporary
    backup="$DEPLOY_DIR/backups/postgres-$(date -u +%Y%m%dT%H%M%SZ)-$$.sql.gz"
    temporary="$backup.partial"
    say 'Backing up the existing PostgreSQL databases.'
    if (umask 077; docker exec postgres sh -c 'exec pg_dumpall -U "$POSTGRES_USER"' | gzip > "$temporary"); then
        gzip -t "$temporary" || { rm -f "$temporary"; return 1; }
        mv "$temporary" "$backup"
        chmod 600 "$backup"
        say "Protected database backup: $backup"
    else
        rm -f "$temporary"
        fail 'Database backup failed; deployment stopped before infrastructure/schema changes.'
    fi
}

ensure_infrastructure() {
    compose up -d postgres redis kafka || return 1
    wait_postgres || return 1
    # Credentials stay inside the container, never in arguments or output.
    docker exec -i postgres sh -c 'exec psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1' \
        < "$ROOT_DIR/docker/postgres/init-databases.sql" >/dev/null || return 1
    local elapsed=0
    until docker exec redis redis-cli ping >/dev/null 2>&1; do
        (( elapsed < READY_TIMEOUT )) || fail 'Redis did not become ready.' || return 1
        sleep 2
        elapsed=$((elapsed + 2))
    done
    elapsed=0
    until docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null 2>&1; do
        (( elapsed < READY_TIMEOUT )) || fail 'Kafka did not become ready.' || return 1
        sleep 2
        elapsed=$((elapsed + 2))
    done
    say 'PostgreSQL, Redis, Kafka and all service databases are ready.'
}

build_service() {
    local service="$1"
    [[ -f "$ROOT_DIR/$service/pom.xml" && -f "$ROOT_DIR/$service/mvnw" ]] || fail "Missing Maven module: $service" || return 1
    say "Building $service before changing running services."
    (cd "$ROOT_DIR/$service"; bash ./mvnw clean package -DskipTests) || return 1
}

find_jar() {
    local service="$1" jar count=0 selected=""
    for jar in "$ROOT_DIR/$service/target/"*.jar; do
        [[ -f "$jar" ]] || continue
        case "$jar" in *-sources.jar|*-javadoc.jar|*-tests.jar) continue ;; esac
        selected="$jar"
        count=$((count + 1))
    done
    [[ "$count" == 1 ]] || fail "Expected exactly one executable JAR for $service; found $count." || return 1
    printf '%s\n' "$selected"
}

validate_release() {
    local release="$1" service
    [[ -d "$release" ]] || fail "Missing release directory: $release" || return 1
    for service in "${SERVICE_ORDER[@]}"; do
        [[ -s "$release/$service.jar" ]] || fail "Release is missing $service.jar." || return 1
    done
}

stage_release() {
    local service jar stage
    PREVIOUS_RELEASE=""
    if [[ -L "$DEPLOY_DIR/current" ]]; then
        PREVIOUS_RELEASE="$(readlink -f "$DEPLOY_DIR/current")"
        validate_release "$PREVIOUS_RELEASE" || return 1
    elif [[ -e "$DEPLOY_DIR/current" ]]; then
        fail 'deployment/current must be a managed release symlink.'
        return 1
    fi
    if [[ "${#TARGETS[@]}" != "${#SERVICE_ORDER[@]}" && -z "$PREVIOUS_RELEASE" ]]; then
        fail 'The first deployment must include all services.'
        return 1
    fi
    # Finish every build before activating a JAR or touching services.
    for service in "${TARGETS[@]}"; do
        build_service "$service" || return 1
    done
    NEW_RELEASE="$DEPLOY_DIR/releases/$(date -u +%Y%m%dT%H%M%SZ)-$$"
    stage="$NEW_RELEASE.staging"
    mkdir "$stage" || return 1
    for service in "${SERVICE_ORDER[@]}"; do
        if [[ " ${TARGETS[*]} " == *" $service "* ]]; then
            jar="$(find_jar "$service")" || return 1
        else
            jar="$PREVIOUS_RELEASE/$service.jar"
        fi
        cp "$jar" "$stage/$service.jar" || return 1
        chmod 444 "$stage/$service.jar" || return 1
    done
    validate_release "$stage" || return 1
    chmod 555 "$stage"
    mv "$stage" "$NEW_RELEASE" || return 1
    say "Prepared immutable release: $NEW_RELEASE"
}

activate_release() {
    local release="$1" temporary="$DEPLOY_DIR/.current-$$"
    ln -s "$release" "$temporary" || return 1
    # GNU mv -T replaces the link itself, without following its directory target.
    mv -Tf "$temporary" "$DEPLOY_DIR/current" || { rm -f "$temporary"; return 1; }
}

wait_service() {
    local service="$1" port elapsed=0
    port="$(service_port "$service")" || return 1
    until sudo -n systemctl is-active --quiet "swiftapp@$service.service" \
        && (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; do
        (( elapsed < READY_TIMEOUT )) || {
            fail "$service did not become ready on 127.0.0.1:$port. Inspect: sudo journalctl -u swiftapp@$service -n 100"
            return 1
        }
        sleep 2
        elapsed=$((elapsed + 2))
    done
    say "$service is active and accepting connections on $port."
}

start_targets() {
    local service
    for service in "${TARGETS[@]}"; do
        sudo -n systemctl enable "swiftapp@$service.service" >/dev/null || return 1
        sudo -n systemctl restart "swiftapp@$service.service" || return 1
        wait_service "$service" || return 1
    done
}

stop_targets() {
    local index failed=0
    for ((index=${#TARGETS[@]}-1; index>=0; index--)); do
        sudo -n systemctl stop "swiftapp@${TARGETS[$index]}.service" || failed=1
    done
    return "$failed"
}

wait_gateway() {
    local elapsed=0 payload
    while (( elapsed < READY_TIMEOUT )); do
        payload="$(curl --silent --show-error --fail --max-time 5 http://127.0.0.1:8080/actuator/health 2>/dev/null)" || payload=""
        if printf '%s' "$payload" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
            say 'API gateway health is UP.'
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    fail 'API gateway health did not become UP.'
}

wait_registry() {
    local elapsed=0 payload service name missing
    while (( elapsed < REGISTRY_TIMEOUT )); do
        payload="$(curl --silent --fail --max-time 5 -H 'Accept: application/xml' http://127.0.0.1:8761/eureka/apps 2>/dev/null)" || payload=""
        missing=""
        for service in "${TARGETS[@]}"; do
            case "$service" in
                eurekaserver) continue ;; apigateway) name=API-GATEWAY ;;
                authservice) name=AUTH-SERVICE ;; userservice) name=USER-SERVICE ;;
                driverservice) name=DRIVER-SERVICE ;; locationservice) name=LOCATION-SERVICE ;;
                matchingservice) name=MATCHING-SERVICE ;; riderservice) name=RIDER-SERVICE ;;
                paymentservice) name=PAYMENT-SERVICE ;; websocketservice) name=WEBSOCKET-SERVICE ;;
                chatservice) name=CHAT-SERVICE ;; noticationservice) name=NOTIFICATION-SERVICE ;;
                ratingservice) name=RATING-SERVICE ;; analyticalservice) name=ANALYTICAL-SERVICE ;;
            esac
            if ! printf '%s' "$payload" | grep -Fq "<name>$name</name>"; then missing="$missing $name"; fi
        done
        if [[ -z "$missing" && -n "$payload" ]]; then
            say 'Deployed services are registered in Eureka.'
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    fail "Eureka registration timed out:$missing"
}

check_targets() {
    wait_registry || return 1
    wait_gateway
}

rollback_release() {
    ROLLBACK_ACTIVE=0
    say 'Deployment failed readiness; restoring the previous JAR release.'
    stop_targets || true
    if [[ -n "$PREVIOUS_RELEASE" ]]; then
        activate_release "$PREVIOUS_RELEASE" || return 1
        start_targets || return 1
        check_targets || return 1
        say "Restored previous release: $PREVIOUS_RELEASE"
    else
        rm -f "$DEPLOY_DIR/current"
        say 'First deployment failed; new JVM services stopped. Database data and backup remain intact.'
    fi
}

handle_exit() {
    local result="$1"
    trap - EXIT INT TERM
    if [[ "$result" != 0 && "$ROLLBACK_ACTIVE" == 1 ]]; then
        set +e
        rollback_release || say 'Automatic JAR rollback also needs attention. Inspect systemd service journals.'
    fi
    exit "$result"
}

deploy_release() {
    require_runtime
    lock_deployment
    stage_release
    backup_database
    ensure_infrastructure
    sudo -n systemctl daemon-reload
    ROLLBACK_ACTIVE=1
    activate_release "$NEW_RELEASE"
    start_targets
    check_targets
    ROLLBACK_ACTIVE=0
    say "Deployment ready: https://$DOMAIN (TLS/Nginx managed by the host setup)."
}

show_status() {
    local service port state
    say 'JVM services:'
    for service in "${TARGETS[@]}"; do
        port="$(service_port "$service")"
        state="$(sudo -n systemctl is-active "swiftapp@$service.service" 2>/dev/null)" || true
        printf '%-22s %-12s 127.0.0.1:%s\n' "$service" "${state:-unknown}" "$port"
    done
    say 'Infrastructure:'
    compose ps
    if [[ -L "$DEPLOY_DIR/current" ]]; then say "Release: $(readlink -f "$DEPLOY_DIR/current")"; fi
}

push_remote() {
    local selection="${1:-all}" ssh_command remote_command arg firebase
    select_targets "$selection" || return 1
    require_command ssh || return 1
    require_command rsync || return 1
    [[ -r "$SSH_KEY" ]] || fail "Cannot read SSH key: $SSH_KEY" || return 1
    [[ "$DEPLOY_HOST" =~ ^[a-zA-Z0-9.-]+$ ]] || fail 'Invalid SWIFT_DEPLOY_HOST.' || return 1
    [[ "$DEPLOY_USER" =~ ^[a-zA-Z_][a-zA-Z0-9_-]*$ ]] || fail 'Invalid SWIFT_DEPLOY_USER.' || return 1
    [[ "$REMOTE_DIR" =~ ^/[a-zA-Z0-9_./-]+$ ]] || fail 'SWIFT_DEPLOY_REMOTE_DIR must be an absolute path without shell metacharacters.' || return 1
    local ssh_options=(-i "$SSH_KEY" -o BatchMode=yes -o IdentitiesOnly=yes
        -o StrictHostKeyChecking=accept-new -o ConnectTimeout=15 -o ServerAliveInterval=15)
    ssh "${ssh_options[@]}" "$DEPLOY_USER@$DEPLOY_HOST" "umask 077; mkdir -p '$REMOTE_DIR'; chmod 700 '$REMOTE_DIR'" || return 1
    # rsync -e takes a shell command; Bash %q protects key paths with spaces.
    ssh_command=ssh
    for arg in "${ssh_options[@]}"; do
        printf -v arg '%q' "$arg"
        ssh_command="$ssh_command $arg"
    done
    say "Syncing source to $DEPLOY_USER@$DEPLOY_HOST:$REMOTE_DIR. Persistent runtime data stays on EC2."
    rsync -az --delete-after --safe-links --chmod=Du=rwx,Dgo=,Fu=rw,Fgo= -e "$ssh_command" \
        --exclude='.git/' --exclude='.idea/' --exclude='.DS_Store' \
        --exclude='*.pem' --exclude='*.key' --exclude='id_rsa*' --exclude='id_ed25519*' \
        --exclude='.env' --exclude='.env.*' --exclude='*service-account*.json' \
        --exclude='target/' --exclude='logs/' --exclude='/build/' \
        --exclude='/deployment/releases/' --exclude='/deployment/current' \
        --exclude='/deployment/backups/' --exclude='/deployment/runtime/' \
        --exclude='/deployment/pids/' --exclude='/deployment/.current-*' \
        "$ROOT_DIR/" "$DEPLOY_USER@$DEPLOY_HOST:$REMOTE_DIR/" || return 1
    # macOS openrsync accepts symbolic --chmod but may ignore it. Enforce source
    # permissions remotely without changing immutable releases or runtime data.
    remote_command="find '$REMOTE_DIR' \\( -path '$REMOTE_DIR/deployment/releases' -o -path '$REMOTE_DIR/deployment/current' -o -path '$REMOTE_DIR/deployment/backups' -o -path '$REMOTE_DIR/deployment/runtime' -o -path '$REMOTE_DIR/deployment/pids' -o -path '$REMOTE_DIR/logs' -o -name target -o -name .git -o -name .idea \\) -prune -o -type d -exec chmod 700 '{}' + -o -type f -exec chmod 600 '{}' +"
    ssh "${ssh_options[@]}" "$DEPLOY_USER@$DEPLOY_HOST" "$remote_command" || return 1
    ssh "${ssh_options[@]}" "$DEPLOY_USER@$DEPLOY_HOST" "umask 077; mkdir -p '$REMOTE_DIR/deployment/runtime'; chmod 700 '$REMOTE_DIR/deployment' '$REMOTE_DIR/deployment/runtime' '$REMOTE_DIR/deploy.sh' '$REMOTE_DIR/deployment/bootstrap.sh'" || return 1
    firebase="$ROOT_DIR/noticationservice/src/main/resources/static/notification/swift-app-service-account.json"
    if [[ -f "$firebase" ]]; then
        # Transfer only this known credential, directly to protected runtime
        # storage. Keep a private backup if an existing credential changes.
        rsync -az --copy-links --checksum --backup --suffix=".$(date -u +%Y%m%dT%H%M%SZ).bak" \
            --chmod=Fu=rw,Fgo= -e "$ssh_command" "$firebase" \
            "$DEPLOY_USER@$DEPLOY_HOST:$REMOTE_DIR/deployment/runtime/firebase-service-account.json" || return 1
        ssh "${ssh_options[@]}" "$DEPLOY_USER@$DEPLOY_HOST" "chmod 600 '$REMOTE_DIR/deployment/runtime/firebase-service-account.json'" || return 1
    fi
    printf -v remote_command 'cd %q && SWIFT_DOMAIN=%q SWIFT_DEPLOY_USER=%q bash deployment/bootstrap.sh && SWIFT_DEPLOY_DOMAIN=%q bash ./deploy.sh deploy %q' "$REMOTE_DIR" "$DOMAIN" "$DEPLOY_USER" "$DOMAIN" "$selection"
    ssh "${ssh_options[@]}" "$DEPLOY_USER@$DEPLOY_HOST" "$remote_command"
}

main() {
    local action="${1:-help}" selection="${2:-all}"
    [[ "$#" -le 2 ]] || { usage; return 2; }
    case "$action" in
        -h|--help|help) usage; return 0 ;;
        push) push_remote "$selection"; return ;;
        all) [[ "$#" -le 1 ]] || { usage; return 2; }; select_targets all ;;
        deploy|start|status|stop) select_targets "$selection" ;;
        *) [[ "$#" -le 1 ]] || { usage; return 2; }; select_targets "$action"; action=deploy ;;
    esac
    trap 'handle_exit $?' EXIT
    trap 'exit 130' INT
    trap 'exit 143' TERM
    case "$action" in
        all|deploy) deploy_release ;;
        start)
            require_runtime
            lock_deployment
            validate_release "$DEPLOY_DIR/current"
            backup_database
            ensure_infrastructure
            start_targets
            check_targets
            ;;
        stop) require_linux; require_command sudo; lock_deployment; stop_targets ;;
        status) require_linux; require_command sudo; require_command docker; show_status ;;
    esac
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then main "$@"; fi
