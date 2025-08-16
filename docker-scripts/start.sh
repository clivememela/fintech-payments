#!/bin/bash

# Start script for Fintech Payments System
set -e

echo "🚀 Starting Fintech Payments System"
echo "==================================="

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

# Start services
print_status "Starting services with Docker Compose..."
docker-compose up -d

# Wait for services to be ready
print_status "Waiting for services to start..."
sleep 10

# Check service health
print_status "Checking service health..."

# Check PostgreSQL
print_status "Checking PostgreSQL..."
for i in {1..30}; do
    if docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
        print_status "✅ PostgreSQL is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "❌ PostgreSQL failed to start"
        exit 1
    fi
    sleep 2
done

# Check Redis
print_status "Checking Redis..."
for i in {1..30}; do
    if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        print_status "✅ Redis is ready"
        break
    fi
    if [ $i -eq 30 ]; then
        print_error "❌ Redis failed to start"
        exit 1
    fi
    sleep 2
done

# Check Ledger Service
print_status "Checking Ledger Service..."
for i in {1..60}; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        print_status "✅ Ledger Service is ready"
        break
    fi
    if [ $i -eq 60 ]; then
        print_error "❌ Ledger Service failed to start"
        docker-compose logs ledger-service
        exit 1
    fi
    sleep 2
done

# Check Transfer Service
print_status "Checking Transfer Service..."
for i in {1..60}; do
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_status "✅ Transfer Service is ready"
        break
    fi
    if [ $i -eq 60 ]; then
        print_error "❌ Transfer Service failed to start"
        docker-compose logs transfer-service
        exit 1
    fi
    sleep 2
done

print_status "🎉 All services are running!"
print_status ""
print_status "Service URLs:"
print_status "  Transfer Service: http://localhost:8080"
print_status "  Ledger Service:   http://localhost:8081"
print_status "  PostgreSQL:       localhost:5433"
print_status "  Redis:            localhost:6379"
print_status "  Nginx:            http://localhost:80"
print_status ""
print_status "API Documentation:"
print_status "  Transfer Service: http://localhost:8080/api-docs"
print_status "  Ledger Service:   http://localhost:8081/api-docs"
print_status ""
print_status "Health Checks:"
print_status "  Transfer Service: http://localhost:8080/actuator/health"
print_status "  Ledger Service:   http://localhost:8081/actuator/health"