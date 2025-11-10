#!/bin/bash

# Monitoring & Observability Exploration Script
# This script helps you explore the monitoring features of the Spring Boot Enterprise Starter

BASE_URL="${1:-http://localhost:8080}"
COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
echo -e "${COLOR_BLUE}Monitoring & Observability Explorer${COLOR_RESET}"
echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
echo -e "Base URL: ${COLOR_GREEN}$BASE_URL${COLOR_RESET}"
echo ""

# Check if jq is available
HAS_JQ=false
if command -v jq &> /dev/null; then
    HAS_JQ=true
    echo -e "${COLOR_GREEN}✓ jq found - JSON output will be formatted${COLOR_RESET}"
else
    echo -e "${COLOR_YELLOW}⚠ jq not found - Install for formatted JSON output${COLOR_RESET}"
    echo -e "  Install: ${COLOR_BLUE}sudo apt-get install jq${COLOR_RESET} (Linux) or ${COLOR_BLUE}brew install jq${COLOR_RESET} (macOS)"
fi
echo ""

# Function to make requests with optional JSON formatting
make_request() {
    local endpoint=$1
    local description=$2
    local url="$BASE_URL$endpoint"
    
    echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
    echo -e "${COLOR_BLUE}$description${COLOR_RESET}"
    echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
    echo -e "GET ${COLOR_GREEN}$url${COLOR_RESET}"
    echo ""
    
    response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$url")
    http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
    body=$(echo "$response" | sed '/HTTP_STATUS/d')
    
    if [ "$http_status" != "200" ]; then
        echo -e "${COLOR_RED}✗ Error: HTTP $http_status${COLOR_RESET}"
        echo "$body"
    else
        if [ "$HAS_JQ" = true ] && echo "$body" | jq . > /dev/null 2>&1; then
            echo "$body" | jq .
        else
            echo "$body"
        fi
    fi
    echo ""
    sleep 1
}

# Function to check if service is running
check_service() {
    echo -e "${COLOR_YELLOW}Checking if service is running...${COLOR_RESET}"
    if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${COLOR_GREEN}✓ Service is running${COLOR_RESET}"
        echo ""
        return 0
    else
        echo -e "${COLOR_RED}✗ Service is not running or not accessible at $BASE_URL${COLOR_RESET}"
        echo -e "${COLOR_YELLOW}Please start the service first:${COLOR_RESET}"
        echo -e "  ${COLOR_BLUE}cd sample-service && mvn spring-boot:run${COLOR_RESET}"
        echo ""
        return 1
    fi
}

# Check if service is running
if ! check_service; then
    exit 1
fi

# 1. Health Check
make_request "/actuator/health" "1. Health Check"

# 2. Application Info
make_request "/actuator/info" "2. Application Info"

# 3. List Available Metrics
make_request "/actuator/metrics" "3. Available Metrics"

# 4. HTTP Request Metrics (if available)
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "${COLOR_BLUE}4. HTTP Request Metrics${COLOR_RESET}"
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "GET ${COLOR_GREEN}$BASE_URL/actuator/metrics/http.request.count${COLOR_RESET}"
echo ""
response=$(curl -s "$BASE_URL/actuator/metrics/http.request.count")
if [ "$HAS_JQ" = true ] && echo "$response" | jq . > /dev/null 2>&1; then
    echo "$response" | jq .
else
    echo "$response"
fi
echo ""

# 5. HTTP Request Duration
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "${COLOR_BLUE}5. HTTP Request Duration${COLOR_RESET}"
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "GET ${COLOR_GREEN}$BASE_URL/actuator/metrics/http.request.duration${COLOR_RESET}"
echo ""
response=$(curl -s "$BASE_URL/actuator/metrics/http.request.duration")
if [ "$HAS_JQ" = true ] && echo "$response" | jq . > /dev/null 2>&1; then
    echo "$response" | jq .
else
    echo "$response"
fi
echo ""

# 6. Application Errors
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "${COLOR_BLUE}6. Application Errors${COLOR_RESET}"
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "GET ${COLOR_GREEN}$BASE_URL/actuator/metrics/application.errors${COLOR_RESET}"
echo ""
response=$(curl -s "$BASE_URL/actuator/metrics/application.errors")
if [ "$HAS_JQ" = true ] && echo "$response" | jq . > /dev/null 2>&1; then
    echo "$response" | jq .
else
    echo "$response"
fi
echo ""

# 7. Prometheus Metrics (first 30 lines)
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "${COLOR_BLUE}7. Prometheus Metrics (first 30 lines)${COLOR_RESET}"
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "GET ${COLOR_GREEN}$BASE_URL/actuator/prometheus${COLOR_RESET}"
echo ""
curl -s "$BASE_URL/actuator/prometheus" | head -30
echo ""
echo -e "${COLOR_YELLOW}... (showing first 30 lines, use curl directly for full output)${COLOR_RESET}"
echo ""

# 8. JVM Metrics
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "${COLOR_BLUE}8. JVM Metrics${COLOR_RESET}"
echo -e "${COLOR_BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${COLOR_RESET}"
echo -e "GET ${COLOR_GREEN}$BASE_URL/actuator/metrics/jvm.memory.used${COLOR_RESET}"
echo ""
response=$(curl -s "$BASE_URL/actuator/metrics/jvm.memory.used")
if [ "$HAS_JQ" = true ] && echo "$response" | jq . > /dev/null 2>&1; then
    echo "$response" | jq .
else
    echo "$response"
fi
echo ""

# Summary
echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
echo -e "${COLOR_BLUE}Exploration Complete!${COLOR_RESET}"
echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
echo ""
echo -e "${COLOR_GREEN}Next Steps:${COLOR_RESET}"
echo "1. Make some API calls to generate metrics:"
echo -e "   ${COLOR_BLUE}curl $BASE_URL/api/public/health${COLOR_RESET}"
echo ""
echo "2. View full Prometheus metrics:"
echo -e "   ${COLOR_BLUE}curl $BASE_URL/actuator/prometheus${COLOR_RESET}"
echo ""
echo "3. Set up Prometheus server to scrape metrics (see MONITORING_GUIDE.md)"
echo ""
echo "4. Create custom metrics using MetricsService (see MONITORING_GUIDE.md)"
echo ""

