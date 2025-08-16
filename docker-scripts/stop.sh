#!/bin/bash

# Stop script for Fintech Payments System
set -e

echo "🛑 Stopping Fintech Payments System"
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

# Parse command line arguments
REMOVE_VOLUMES=false
REMOVE_IMAGES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --volumes)
            REMOVE_VOLUMES=true
            shift
            ;;
        --images)
            REMOVE_IMAGES=true
            shift
            ;;
        --all)
            REMOVE_VOLUMES=true
            REMOVE_IMAGES=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--volumes] [--images] [--all]"
            echo "  --volumes: Remove volumes (data will be lost)"
            echo "  --images:  Remove built images"
            echo "  --all:     Remove both volumes and images"
            exit 1
            ;;
    esac
done

# Stop services
print_status "Stopping services..."
docker-compose down

if [ "$REMOVE_VOLUMES" = true ]; then
    print_warning "Removing volumes (data will be lost)..."
    docker-compose down -v
    docker volume prune -f
fi

if [ "$REMOVE_IMAGES" = true ]; then
    print_status "Removing built images..."
    docker rmi fintech-payments-ledger-service:latest 2>/dev/null || true
    docker rmi fintech-payments-transfer-service:latest 2>/dev/null || true
    docker image prune -f
fi

# Clean up dangling containers
print_status "Cleaning up..."
docker container prune -f

print_status "✅ Fintech Payments System stopped successfully"

if [ "$REMOVE_VOLUMES" = true ]; then
    print_warning "⚠️  Database data has been removed"
fi

if [ "$REMOVE_IMAGES" = true ]; then
    print_status "🗑️  Built images have been removed"
fi