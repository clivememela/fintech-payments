#!/bin/bash

# Build script for Fintech Payments System
set -e

echo "🏗️  Building Fintech Payments System Docker Images"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
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

# Build Ledger Service
print_status "Building Ledger Service..."
docker build -t fintech-payments-ledger-service:latest ./fintech-payments-ledger-service/

if [ $? -eq 0 ]; then
    print_status "✅ Ledger Service built successfully"
else
    print_error "❌ Failed to build Ledger Service"
    exit 1
fi

# Build Transfer Service
print_status "Building Transfer Service..."
docker build -t fintech-payments-transfer-service:latest ./fintech-payments-transfer-service/

if [ $? -eq 0 ]; then
    print_status "✅ Transfer Service built successfully"
else
    print_error "❌ Failed to build Transfer Service"
    exit 1
fi

# List built images
print_status "Built images:"
docker images | grep fintech-payments

print_status "🎉 All services built successfully!"
print_status "Run 'docker-compose up' to start the services"