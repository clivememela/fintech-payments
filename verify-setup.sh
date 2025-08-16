#!/bin/bash

# Fintech Payments System - Setup Verification Script
# This script verifies that the system is properly set up and running

set -e

echo "🚀 Fintech Payments System - Setup Verification"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    if [ $2 -eq 0 ]; then
        echo -e "${GREEN}✅ $1${NC}"
    else
        echo -e "${RED}❌ $1${NC}"
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "ℹ️  $1"
}

# Check if Docker is running
echo "Checking Docker setup..."
if docker --version > /dev/null 2>&1; then
    print_status "Docker is installed" 0
    if docker ps > /dev/null 2>&1; then
        print_status "Docker daemon is running" 0
    else
        print_status "Docker daemon is not running" 1
        print_warning "Please start Docker Desktop"
        exit 1
    fi
else
    print_status "Docker is not installed" 1
    print_warning "Please install Docker Desktop"
    exit 1
fi

# Check if Docker Compose is available
if docker-compose --version > /dev/null 2>&1; then
    print_status "Docker Compose is available" 0
else
    print_status "Docker Compose is not available" 1
    exit 1
fi

# Check Java versions
echo -e "\nChecking Java setup..."
if java -version > /dev/null 2>&1; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        print_status "Java $JAVA_VERSION is installed (required: 17+)" 0
    else
        print_status "Java version is too old (found: $JAVA_VERSION, required: 17+)" 1
    fi
else
    print_status "Java is not installed" 1
    print_warning "Please install Java 17 or later"
fi

# Check if Gradle wrapper exists
echo -e "\nChecking build tools..."
if [ -f "./gradlew" ]; then
    print_status "Gradle wrapper found" 0
    chmod +x ./gradlew
else
    print_status "Gradle wrapper not found" 1
fi

# Check project structure
echo -e "\nChecking project structure..."
if [ -d "fintech-payments-ledger-service" ]; then
    print_status "Ledger Service directory exists" 0
else
    print_status "Ledger Service directory missing" 1
fi

if [ -d "fintech-payments-transfer-service" ]; then
    print_status "Transfer Service directory exists" 0
else
    print_status "Transfer Service directory missing" 1
fi

if [ -f "docker-compose.yml" ]; then
    print_status "Docker Compose configuration found" 0
else
    print_status "Docker Compose configuration missing" 1
fi

# Test build (optional)
echo -e "\nTesting build process..."
print_info "Running gradle build (this may take a few minutes)..."
if ./gradlew build -x test > /dev/null 2>&1; then
    print_status "Gradle build successful" 0
else
    print_status "Gradle build failed" 1
    print_warning "Run './gradlew build' manually to see detailed errors"
fi

# Test Docker build (optional)
echo -e "\nTesting Docker setup..."
print_info "Pulling required Docker images..."
if docker pull eclipse-temurin:21-jre-alpine > /dev/null 2>&1; then
    print_status "Java 21 runtime image available" 0
else
    print_status "Failed to pull Java 21 runtime image" 1
fi

if docker pull eclipse-temurin:17-jre-alpine > /dev/null 2>&1; then
    print_status "Java 17 runtime image available" 0
else
    print_status "Failed to pull Java 17 runtime image" 1
fi

if docker pull postgres:15-alpine > /dev/null 2>&1; then
    print_status "PostgreSQL image available" 0
else
    print_status "Failed to pull PostgreSQL image" 1
fi

# Summary
echo -e "\n📋 Setup Summary"
echo "=================="
print_info "Project Root: $(pwd)"
print_info "Documentation: README.md, SOLUTION.md"
print_info "Services:"
print_info "  - Ledger Service (Java 21): Port 8080"
print_info "  - Transfer Service (Java 17): Port 8081"
print_info "  - PostgreSQL Database: Port 5433"
print_info "  - Redis Cache: Port 6379"

echo -e "\n🚀 Quick Start Commands"
echo "======================="
echo "# Start all services with Docker:"
echo "docker-compose up --build"
echo ""
echo "# Or start individual services locally:"
echo "cd fintech-payments-ledger-service && ./gradlew bootRun"
echo "cd fintech-payments-transfer-service && ./gradlew bootRun"
echo ""
echo "# Run tests:"
echo "./gradlew test"
echo ""
echo "# Access APIs:"
echo "Ledger Service: http://localhost:8080/swagger-ui/index.html"
echo "Transfer Service: http://localhost:8081/swagger-ui/index.html"

echo -e "\n✨ Setup verification complete!"