#!/bin/bash

# Test script for Docker setup
set -e

echo "🧪 Testing Fintech Payments Docker Setup"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

print_status "Building Docker images..."
./docker-scripts/build.sh

if [ $? -ne 0 ]; then
    print_error "Failed to build Docker images"
    exit 1
fi

print_status "Starting services with Docker Compose..."
docker-compose up -d

print_status "Waiting for services to start..."
sleep 30

# Test API endpoints
print_status "Testing Transfer API..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-$(date +%s)" \
  -d '{
    "fromAccountId": "acc-123",
    "toAccountId": "acc-456",
    "amount": 100.00
  }')

if [ $? -eq 0 ]; then
    print_status "✅ Transfer API is working"
    echo "Response: $RESPONSE"
else
    print_error "❌ Transfer API test failed"
    exit 1
fi

print_status "🎉 All tests passed! Docker setup is working correctly."