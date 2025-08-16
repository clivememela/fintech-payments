#!/bin/bash

# Quick Start Script for Fintech Payments System
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

print_header() {
    echo -e "${BLUE}$1${NC}"
}

# Check prerequisites
check_prerequisites() {
    print_header "🔍 Checking Prerequisites"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker Desktop."
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed."
        exit 1
    fi
    
    # Check if Docker is running
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker Desktop."
        exit 1
    fi
    
    print_status "✅ All prerequisites met"
}

# Show menu
show_menu() {
    print_header "🚀 Fintech Payments System - Quick Start"
    echo
    echo "Choose your setup option:"
    echo "1) Docker Compose (Recommended) - Start all services with Docker"
    echo "2) Local Development - Start services locally with Gradle"
    echo "3) Infrastructure Only - Start only PostgreSQL and Redis"
    echo "4) Test Existing Setup - Test if services are already running"
    echo "5) Stop All Services - Stop and cleanup"
    echo "6) View Service Status - Check what's running"
    echo "7) Exit"
    echo
    read -p "Enter your choice (1-7): " choice
}

# Docker Compose setup
docker_setup() {
    print_header "🐳 Starting with Docker Compose"
    
    print_status "Building Docker images..."
    ./docker-scripts/build.sh
    
    print_status "Starting all services..."
    docker-compose up -d
    
    print_status "Waiting for services to start..."
    sleep 30
    
    print_status "Testing setup..."
    ./test-docker-setup.sh
    
    show_service_urls
}

# Local development setup
local_setup() {
    print_header "💻 Local Development Setup"
    
    print_status "Starting infrastructure services..."
    docker-compose up -d postgres redis
    
    print_status "Waiting for infrastructure to be ready..."
    sleep 15
    
    print_status "Infrastructure is ready!"
    print_warning "Now you need to start the services manually:"
    echo
    echo "Terminal 1 - Ledger Service:"
    echo "  cd fintech-payments-ledger-service"
    echo "  ./gradlew bootRun"
    echo
    echo "Terminal 2 - Transfer Service:"
    echo "  cd fintech-payments-transfer-service"
    echo "  ./gradlew bootRun"
    echo
    print_status "Or use your IDE to run the main application classes."
}

# Infrastructure only
infrastructure_setup() {
    print_header "🏗️ Infrastructure Only Setup"
    
    print_status "Starting PostgreSQL and Redis..."
    docker-compose up -d postgres redis
    
    print_status "Waiting for services to be ready..."
    sleep 15
    
    print_status "Infrastructure is ready!"
    echo "PostgreSQL: localhost:5433"
    echo "Redis: localhost:6379"
}

# Test existing setup
test_setup() {
    print_header "🧪 Testing Existing Setup"
    
    if ./test-docker-setup.sh; then
        print_status "✅ All services are working correctly!"
        show_service_urls
    else
        print_error "❌ Some services are not working properly."
        print_warning "Try running option 1 (Docker Compose) to reset everything."
    fi
}

# Stop all services
stop_services() {
    print_header "🛑 Stopping All Services"
    
    print_status "Stopping services..."
    ./docker-scripts/stop.sh
    
    print_status "✅ All services stopped."
}

# View service status
view_status() {
    print_header "📊 Service Status"
    
    echo "Docker Compose Services:"
    docker-compose ps
    echo
    
    echo "Service Health Checks:"
    
    # Check Transfer Service
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        print_status "✅ Transfer Service: http://localhost:8080"
    else
        print_error "❌ Transfer Service: Not responding"
    fi
    
    # Check Ledger Service
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        print_status "✅ Ledger Service: http://localhost:8081"
    else
        print_error "❌ Ledger Service: Not responding"
    fi
    
    # Check PostgreSQL
    if docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
        print_status "✅ PostgreSQL: localhost:5433"
    else
        print_error "❌ PostgreSQL: Not responding"
    fi
    
    # Check Redis
    if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
        print_status "✅ Redis: localhost:6379"
    else
        print_error "❌ Redis: Not responding"
    fi
}

# Show service URLs
show_service_urls() {
    print_header "🌐 Service URLs"
    echo
    echo "📡 APIs:"
    echo "  Transfer Service API: http://localhost:8080/api"
    echo "  Ledger Service API:   http://localhost:8081/api"
    echo
    echo "📚 API Documentation (Development Only):"
    echo "  Transfer Service Swagger: http://localhost:8080/swagger-ui.html"
    echo "  Ledger Service Swagger:   http://localhost:8081/swagger-ui.html"
    echo
    echo "🏥 Health Checks:"
    echo "  Transfer Service: http://localhost:8080/actuator/health"
    echo "  Ledger Service:   http://localhost:8081/actuator/health"
    echo
    echo "📊 Metrics:"
    echo "  Transfer Service: http://localhost:8080/actuator/prometheus"
    echo "  Ledger Service:   http://localhost:8081/actuator/prometheus"
    echo
    echo "🧪 Quick API Test:"
    echo '  curl -X POST http://localhost:8080/api/transfers \'
    echo '    -H "Content-Type: application/json" \'
    echo '    -H "Idempotency-Key: test-$(date +%s)" \'
    echo '    -d '"'"'{"fromAccountId": 123, "toAccountId": 456, "amount": 100.00}'"'"
    echo
}

# Main execution
main() {
    check_prerequisites
    
    while true; do
        show_menu
        
        case $choice in
            1)
                docker_setup
                break
                ;;
            2)
                local_setup
                break
                ;;
            3)
                infrastructure_setup
                break
                ;;
            4)
                test_setup
                ;;
            5)
                stop_services
                ;;
            6)
                view_status
                ;;
            7)
                print_status "Goodbye! 👋"
                exit 0
                ;;
            *)
                print_error "Invalid option. Please choose 1-7."
                ;;
        esac
        
        echo
        read -p "Press Enter to continue..."
        echo
    done
}

# Run main function
main "$@"