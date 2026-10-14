#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Stopping SwiftApp Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Stop all microservices
if [ -d "logs" ]; then
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            service_name=$(basename "$pidfile" .pid)
            
            if kill -0 $pid 2>/dev/null; then
                echo -e "${YELLOW}Stopping $service_name (PID: $pid)...${NC}"
                kill $pid
                sleep 2
                
                if kill -0 $pid 2>/dev/null; then
                    echo -e "${RED}Force stopping $service_name...${NC}"
                    kill -9 $pid
                fi
                
                echo -e "${GREEN}✓ $service_name stopped${NC}"
            else
                echo -e "${YELLOW}$service_name is not running${NC}"
            fi
            
            rm "$pidfile"
        fi
    done
else
    echo -e "${YELLOW}No running services found${NC}"
fi

echo ""
echo -e "${BLUE}Stopping infrastructure (Docker)...${NC}"
docker compose down

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  All Services Stopped${NC}"
echo -e "${GREEN}========================================${NC}"
