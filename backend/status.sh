#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Color codes for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  SwiftApp System Status${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check infrastructure
echo -e "${CYAN}Infrastructure Status:${NC}"
if docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "postgres|kafka|redis" > /dev/null 2>&1; then
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "postgres|kafka|redis|NAMES"
else
    echo -e "${YELLOW}Infrastructure is not running${NC}"
fi

echo ""
echo -e "${CYAN}Microservices Status:${NC}"

# Check if logs directory exists
if [ -d "logs" ]; then
    services_running=0
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            service_name=$(basename "$pidfile" .pid)
            
            if kill -0 $pid 2>/dev/null; then
                echo -e "${GREEN}✓${NC} $service_name is running (PID: $pid)"
                services_running=$((services_running + 1))
            else
                echo -e "${YELLOW}✗${NC} $service_name is not running"
            fi
        fi
    done
    
    if [ $services_running -eq 0 ]; then
        echo -e "${YELLOW}No microservices are running${NC}"
    fi
else
    echo -e "${YELLOW}No microservices have been started yet${NC}"
fi

echo ""
echo -e "${BLUE}========================================${NC}"
