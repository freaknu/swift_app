#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Starting SwiftApp Infrastructure${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}✗ Docker is not running!${NC}"
    echo -e "${YELLOW}Please start Docker Desktop and try again.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker is running${NC}"

# Start infrastructure services
echo -e "${BLUE}Starting PostgreSQL, Redis and Kafka (KRaft mode)...${NC}"
docker compose up -d postgres redis kafka

# Wait for PostgreSQL to be ready
echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
timeout=60
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker compose exec -T postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d postgres' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
    echo -n "."
done

if [ $elapsed -ge $timeout ]; then
    echo -e "\n${RED}✗ PostgreSQL failed to start within ${timeout}s${NC}"
    exit 1
fi

# Docker's init directory runs only on a fresh volume. Reapply the same
# idempotent psql script so existing volumes also gain newly added databases.
echo -e "${YELLOW}Ensuring service databases exist...${NC}"
if ! docker compose exec -T postgres sh -c \
    'psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1' \
    < docker/postgres/init-databases.sql > /dev/null; then
    echo -e "${RED}✗ Could not ensure service databases${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Service databases are ready${NC}"

# Wait for Redis to be ready
echo -e "${YELLOW}Waiting for Redis to be ready...${NC}"
timeout=60
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker exec redis redis-cli ping > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Redis is ready${NC}"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
    echo -n "."
done

if [ $elapsed -ge $timeout ]; then
    echo -e "\n${RED}✗ Redis failed to start within ${timeout}s${NC}"
    exit 1
fi

# Wait for Kafka to be ready
echo -e "${YELLOW}Waiting for Kafka (KRaft) to be ready...${NC}"
timeout=60
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Kafka is ready${NC}"
        break
    fi
    sleep 2
    elapsed=$((elapsed + 2))
    echo -n "."
done

if [ $elapsed -ge $timeout ]; then
    echo -e "\n${RED}✗ Kafka failed to start within ${timeout}s${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Infrastructure Started Successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Services running:${NC}"
echo -e "  • PostgreSQL: ${GREEN}localhost:5432${NC}"
echo -e "  • Redis:      ${GREEN}localhost:6379${NC}"
echo -e "  • Kafka:      ${GREEN}localhost:9092${NC} (KRaft mode - no Zookeeper!)"
echo ""
echo -e "${YELLOW}Next step: Run ./start-services.sh to start all microservices${NC}"
