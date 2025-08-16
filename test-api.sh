#!/bin/bash

# Fintech Payments System - API Testing Script
# This script demonstrates the API functionality

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
LEDGER_URL="http://localhost:8080"
TRANSFER_URL="http://localhost:8081"

echo -e "${BLUE}🧪 Fintech Payments System - API Testing${NC}"
echo "=========================================="

# Function to check if service is running
check_service() {
    local service_name=$1
    local url=$2
    
    echo -n "Checking $service_name... "
    if curl -s "$url/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Running${NC}"
        return 0
    else
        echo -e "${RED}❌ Not running${NC}"
        return 1
    fi
}

# Function to make API call and display result
api_call() {
    local method=$1
    local url=$2
    local data=$3
    local description=$4
    
    echo -e "\n${YELLOW}📡 $description${NC}"
    echo "Request: $method $url"
    
    if [ -n "$data" ]; then
        echo "Data: $data"
        response=$(curl -s -X "$method" -H "Content-Type: application/json" -d "$data" "$url" 2>/dev/null || echo "ERROR")
    else
        response=$(curl -s -X "$method" "$url" 2>/dev/null || echo "ERROR")
    fi
    
    if [ "$response" = "ERROR" ]; then
        echo -e "${RED}❌ Request failed${NC}"
    else
        echo -e "${GREEN}✅ Response:${NC}"
        echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    fi
}

# Check if services are running
echo "Checking service availability..."
if ! check_service "Ledger Service" "$LEDGER_URL"; then
    echo -e "${RED}❌ Ledger Service is not running. Please start it first.${NC}"
    echo "Run: docker-compose up ledger-service"
    exit 1
fi

if ! check_service "Transfer Service" "$TRANSFER_URL"; then
    echo -e "${RED}❌ Transfer Service is not running. Please start it first.${NC}"
    echo "Run: docker-compose up transfer-service"
    exit 1
fi

echo -e "\n${GREEN}✅ All services are running!${NC}"

# Test Ledger Service APIs
echo -e "\n${BLUE}🏦 Testing Ledger Service APIs${NC}"
echo "================================"

# Create test accounts
api_call "POST" "$LEDGER_URL/api/accounts" \
    '{"accountNumber":"ACC001","accountHolderName":"John Doe","initialBalance":1000.00}' \
    "Creating Account 1"

api_call "POST" "$LEDGER_URL/api/accounts" \
    '{"accountNumber":"ACC002","accountHolderName":"Jane Smith","initialBalance":500.00}' \
    "Creating Account 2"

# Get account details
api_call "GET" "$LEDGER_URL/api/accounts/ACC001" "" \
    "Getting Account 1 Details"

api_call "GET" "$LEDGER_URL/api/accounts/ACC002" "" \
    "Getting Account 2 Details"

# Get all accounts
api_call "GET" "$LEDGER_URL/api/accounts" "" \
    "Getting All Accounts"

# Test Transfer Service APIs
echo -e "\n${BLUE}💸 Testing Transfer Service APIs${NC}"
echo "================================="

# Create a transfer
api_call "POST" "$TRANSFER_URL/api/transfers" \
    '{"fromAccount":"ACC001","toAccount":"ACC002","amount":100.00,"description":"Test transfer"}' \
    "Creating Transfer"

# Get transfer status (you might need to adjust the transfer ID)
api_call "GET" "$TRANSFER_URL/api/transfers/status/1" "" \
    "Getting Transfer Status"

# Get circuit breaker metrics
api_call "GET" "$TRANSFER_URL/api/transfers/circuit-breaker/metrics" "" \
    "Getting Circuit Breaker Metrics"

# Verify account balances after transfer
echo -e "\n${BLUE}🔍 Verifying Account Balances After Transfer${NC}"
echo "============================================="

api_call "GET" "$LEDGER_URL/api/accounts/ACC001" "" \
    "Account 1 Balance (should be 900.00)"

api_call "GET" "$LEDGER_URL/api/accounts/ACC002" "" \
    "Account 2 Balance (should be 600.00)"

# Test error scenarios
echo -e "\n${BLUE}⚠️  Testing Error Scenarios${NC}"
echo "============================"

# Try to transfer from non-existent account
api_call "POST" "$TRANSFER_URL/api/transfers" \
    '{"fromAccount":"NONEXISTENT","toAccount":"ACC002","amount":50.00,"description":"Error test"}' \
    "Transfer from Non-existent Account (should fail)"

# Try to transfer more than available balance
api_call "POST" "$TRANSFER_URL/api/transfers" \
    '{"fromAccount":"ACC001","toAccount":"ACC002","amount":10000.00,"description":"Insufficient funds test"}' \
    "Transfer with Insufficient Funds (should fail)"

# Health checks
echo -e "\n${BLUE}🏥 Health Checks${NC}"
echo "================"

api_call "GET" "$LEDGER_URL/actuator/health" "" \
    "Ledger Service Health"

api_call "GET" "$TRANSFER_URL/actuator/health" "" \
    "Transfer Service Health"

# API Documentation links
echo -e "\n${BLUE}📚 API Documentation${NC}"
echo "===================="
echo -e "Ledger Service Swagger UI: ${GREEN}$LEDGER_URL/swagger-ui/index.html${NC}"
echo -e "Transfer Service Swagger UI: ${GREEN}$TRANSFER_URL/swagger-ui/index.html${NC}"
echo -e "Ledger Service OpenAPI Spec: ${GREEN}$LEDGER_URL/v3/api-docs${NC}"
echo -e "Transfer Service OpenAPI Spec: ${GREEN}$TRANSFER_URL/v3/api-docs${NC}"

echo -e "\n${GREEN}✨ API testing complete!${NC}"
echo -e "\n${YELLOW}💡 Tips:${NC}"
echo "- Use the Swagger UI for interactive API testing"
echo "- Check the logs with: docker-compose logs -f <service-name>"
echo "- Monitor metrics at: /actuator/metrics endpoints"
echo "- View health status at: /actuator/health endpoints"