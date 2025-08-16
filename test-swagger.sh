#!/bin/bash

# Test Swagger Documentation Availability
# This script checks if both services are running and Swagger UI is accessible

echo "🔍 Testing Swagger Documentation Availability"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check service health
check_service() {
    local service_name=$1
    local port=$2
    local url="http://localhost:${port}"
    
    echo -e "\n${BLUE}Testing ${service_name}...${NC}"
    
    # Check if service is running
    if curl -s "${url}/actuator/health" > /dev/null 2>&1; then
        echo -e "  ✅ Service is ${GREEN}running${NC} on port ${port}"
        
        # Check Swagger UI
        if curl -s "${url}/swagger-ui.html" | grep -q "swagger-ui"; then
            echo -e "  ✅ Swagger UI is ${GREEN}accessible${NC}: ${url}/swagger-ui.html"
        else
            echo -e "  ❌ Swagger UI is ${RED}not accessible${NC}"
            return 1
        fi
        
        # Check OpenAPI JSON
        if curl -s "${url}/v3/api-docs" | grep -q "openapi"; then
            echo -e "  ✅ OpenAPI JSON is ${GREEN}available${NC}: ${url}/v3/api-docs"
        else
            echo -e "  ❌ OpenAPI JSON is ${RED}not available${NC}"
            return 1
        fi
        
        # Get API info
        local api_title=$(curl -s "${url}/v3/api-docs" | grep -o '"title":"[^"]*"' | cut -d'"' -f4)
        local api_version=$(curl -s "${url}/v3/api-docs" | grep -o '"version":"[^"]*"' | cut -d'"' -f4)
        echo -e "  📋 API Title: ${YELLOW}${api_title}${NC}"
        echo -e "  📋 API Version: ${YELLOW}${api_version}${NC}"
        
        return 0
    else
        echo -e "  ❌ Service is ${RED}not running${NC} on port ${port}"
        echo -e "  💡 Start with: ${YELLOW}docker-compose up -d${NC} or ${YELLOW}./gradlew bootRun${NC}"
        return 1
    fi
}

# Function to test API endpoints
test_endpoints() {
    local service_name=$1
    local port=$2
    local url="http://localhost:${port}"
    
    echo -e "\n${BLUE}Testing ${service_name} API Endpoints...${NC}"
    
    # Get endpoint count
    local endpoint_count=$(curl -s "${url}/v3/api-docs" | grep -o '"paths":{[^}]*}' | grep -o '"/[^"]*"' | wc -l)
    echo -e "  📊 Total endpoints: ${YELLOW}${endpoint_count}${NC}"
    
    # List main endpoints
    echo -e "  📋 Available endpoints:"
    curl -s "${url}/v3/api-docs" | grep -o '"/api/[^"]*"' | sort | while read endpoint; do
        echo -e "    • ${endpoint//\"/}"
    done
}

# Main execution
echo -e "${BLUE}Starting Swagger Documentation Tests...${NC}"

# Test Transfer Service
if check_service "Transfer Service" "8080"; then
    test_endpoints "Transfer Service" "8080"
    TRANSFER_OK=true
else
    TRANSFER_OK=false
fi

# Test Ledger Service  
if check_service "Ledger Service" "8081"; then
    test_endpoints "Ledger Service" "8081"
    LEDGER_OK=true
else
    LEDGER_OK=false
fi

# Summary
echo -e "\n${BLUE}Summary${NC}"
echo "========"

if [ "$LEDGER_OK" = true ] && [ "$TRANSFER_OK" = true ]; then
    echo -e "✅ ${GREEN}All services are running with Swagger documentation!${NC}"
    echo ""
    echo -e "🌐 ${YELLOW}Quick Access Links:${NC}"
    echo -e "   • Transfer Service: http://localhost:8080/swagger-ui.html"
    echo -e "   • Ledger Service:   http://localhost:8081/swagger-ui.html"
    echo -e "   • Navigation Page:  file://$(pwd)/swagger-index.html"
    echo ""
    echo -e "📚 ${YELLOW}Documentation:${NC}"
    echo -e "   • Guide: ./SWAGGER_DOCUMENTATION.md"
    echo -e "   • Transfer OpenAPI: http://localhost:8080/v3/api-docs"
    echo -e "   • Ledger OpenAPI: http://localhost:8081/v3/api-docs"
    
    # Open browser if available
    if command -v open >/dev/null 2>&1; then
        echo ""
        echo -e "🚀 ${YELLOW}Opening Swagger UI in browser...${NC}"
        open "file://$(pwd)/swagger-index.html"
    elif command -v xdg-open >/dev/null 2>&1; then
        echo ""
        echo -e "🚀 ${YELLOW}Opening Swagger UI in browser...${NC}"
        xdg-open "file://$(pwd)/swagger-index.html"
    fi
    
    exit 0
else
    echo -e "❌ ${RED}Some services are not running or Swagger is not accessible${NC}"
    echo ""
    echo -e "💡 ${YELLOW}To start services:${NC}"
    echo -e "   • Docker: ${YELLOW}docker-compose up -d${NC}"
    echo -e "   • Gradle: ${YELLOW}./gradlew bootRun${NC}"
    echo ""
    echo -e "🔧 ${YELLOW}Troubleshooting:${NC}"
    echo -e "   • Check if ports 8080 and 8081 are available"
    echo -e "   • Verify services are not running in production profile"
    echo -e "   • Check application logs for errors"
    
    exit 1
fi