#!/bin/bash

set -u
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

if [[ -f "$SCRIPT_DIR/.env" ]]; then
    set -a
    source "$SCRIPT_DIR/.env"
    set +a
fi
: "${DB_PASSWORD:?Run bash scripts/configure-local.sh first}"
: "${JWT_SECRET_KEY:?Run bash scripts/configure-local.sh first}"

# Capture development email locally when testing the complete backend.
if [[ "${SWIFTAPP_CAPTURE_EMAILS:-0}" == "1" ]]; then
    docker compose --profile development-mail up -d mailpit || exit 1
    export MAIL_HOST=127.0.0.1 MAIL_PORT=1025
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=false
    export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=false
    printf 'Development email inbox: http://localhost:8025\n'
fi

if ! command -v nc >/dev/null 2>&1; then
    printf 'Starting services requires nc (netcat) for readiness checks.\n' >&2
    exit 1
fi

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Starting SwiftApp Microservices${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if infrastructure is running
if ! docker ps | grep -q "postgres"; then
    echo -e "${RED}✗ PostgreSQL is not running!${NC}"
    echo -e "${YELLOW}Run ./start-infrastructure.sh first${NC}"
    exit 1
fi

if ! docker ps | grep -q "kafka"; then
    echo -e "${RED}✗ Kafka is not running!${NC}"
    echo -e "${YELLOW}Run ./start-infrastructure.sh first${NC}"
    exit 1
fi

if ! docker ps | grep -q "redis"; then
    echo -e "${RED}✗ Redis is not running!${NC}"
    echo -e "${YELLOW}Run ./start-infrastructure.sh first${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Infrastructure is running${NC}"
echo ""

# Define service startup order
SERVICES=(
    "eurekaserver:8761:Eureka Server"
    "authservice:8081:Auth Service"
    "userservice:8082:User Service"
    "driverservice:8096:Driver Service"
    "locationservice:8087:Location Service"
    "matchingservice:9005:Matching Service"
    "riderservice:8083:Rider Service"
    "paymentservice:9910:Payment Service"
    "websocketservice:8085:WebSocket Service"
    "chatservice:9004:Chat Service"
    "noticationservice:8088:Notification Service"
    "ratingservice:8086:Rating Service"
    "analyticalservice:8089:Analytics Service"
    "apigateway:8080:API Gateway"
)

# Create logs directory
mkdir -p logs

# Function to start a service
start_service() {
    local service_dir=$1
    local port=$2
    local service_name=$3
    
    echo -e "${YELLOW}Starting $service_name on port $port...${NC}"

    local existing_pid=""
    if [ -f "logs/$service_dir.pid" ]; then
        existing_pid=$(cat "logs/$service_dir.pid")
    fi
    if [ -n "$existing_pid" ] && kill -0 "$existing_pid" 2>/dev/null \
        && [[ "$(ps -p "$existing_pid" -o command=)" == *"$service_dir"* ]]; then
        echo -e "${GREEN}✓ $service_name is already running (PID: $existing_pid)${NC}"
        return 0
    fi
    if nc -z 127.0.0.1 "$port" >/dev/null 2>&1; then
        echo -e "${RED}✗ Port $port is occupied by another process${NC}"
        return 1
    fi
    
    local jar_file
    jar_file=$(find "$service_dir/target" -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' \
        ! -name '*.original' -print -quit 2>/dev/null)
    if [ -z "$jar_file" ]; then
        echo -e "${RED}✗ $service_name has no built JAR. Build it with ./mvnw package first.${NC}"
        return 1
    fi

    local java_options=()
    read -r -a java_options <<< "${SWIFTAPP_JAVA_OPTS:--Xms64m -Xmx384m}"
    local pid
    if command -v python3 >/dev/null 2>&1; then
        # A separate session survives the launcher/terminal command ending.
        # nohup alone only ignores SIGHUP; it does not detach the process group.
        pid=$(python3 - "$jar_file" "logs/$service_dir.log" "${java_options[@]}" <<'PY'
import subprocess
import sys

with open(sys.argv[2], 'w') as log:
    process = subprocess.Popen(
        ['java', *sys.argv[3:], '-jar', sys.argv[1]],
        stdin=subprocess.DEVNULL,
        stdout=log,
        stderr=subprocess.STDOUT,
        start_new_session=True,
    )
print(process.pid)
PY
        ) || return 1
    else
        nohup java "${java_options[@]}" -jar "$jar_file" > "logs/$service_dir.log" 2>&1 &
        pid=$!
    fi
    echo "$pid" > "logs/$service_dir.pid"
    
    # Wait for service to start
    local attempt
    for attempt in {1..60}; do
        if ! kill -0 "$pid" 2>/dev/null; then
            break
        fi
        if nc -z 127.0.0.1 "$port" >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done

    if kill -0 "$pid" 2>/dev/null && nc -z 127.0.0.1 "$port" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ $service_name started (PID: $pid)${NC}"
        return 0
    else
        echo -e "${RED}✗ $service_name failed to become ready on port $port${NC}"
        return 1
    fi
}

# Start all services
failed_services=()
for service_info in "${SERVICES[@]}"; do
    IFS=':' read -r service_dir port service_name <<< "$service_info"
    if ! start_service "$service_dir" "$port" "$service_name"; then
        failed_services+=("$service_name")
    fi
    
    # Special wait time for Eureka Server
    if [ "$service_dir" == "eurekaserver" ]; then
        echo -e "${CYAN}Waiting for Eureka Server to fully initialize...${NC}"
        sleep 10
    fi
    
    echo ""
done

if [ ${#failed_services[@]} -gt 0 ]; then
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}  Some Services Failed To Start${NC}"
    echo -e "${RED}========================================${NC}"
    printf '%s\n' "${failed_services[@]}"
    echo ""
    echo -e "${YELLOW}Check logs in the 'logs' directory.${NC}"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  All Services Started Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Service Endpoints (with /v1 API versioning):${NC}"
echo -e "  • Eureka Server:    ${GREEN}http://localhost:8761${NC}"
echo -e "  • API Gateway:      ${GREEN}http://localhost:8080${NC}"
echo -e "  • Auth Service:     ${GREEN}http://localhost:8080/api/v1/auth${NC}"
echo -e "  • User Service:     ${GREEN}http://localhost:8080/api/v1/user${NC}"
echo -e "  • Driver Service:   ${GREEN}http://localhost:8080/api/v1/driver${NC}"
echo -e "  • Rider Service:    ${GREEN}http://localhost:8080/api/v1/ride${NC}"
echo -e "  • Payment Service:  ${GREEN}http://localhost:8080/api/v1/payments${NC}"
echo -e "  • Location Service: ${GREEN}http://localhost:8080/api/v1/location${NC}"
echo -e "  • Rating Service:   ${GREEN}http://localhost:8080/api/v1/rating${NC}"
echo ""
echo -e "${YELLOW}Logs are available in the 'logs' directory${NC}"
echo -e "${YELLOW}To stop all services, run: ./stop-services.sh${NC}"
