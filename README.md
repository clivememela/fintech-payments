# Fintech Payments System

A robust, microservices-based payment processing system built with Spring Boot, featuring circuit breaker patterns, comprehensive API documentation, and containerized deployment.

## 🏗️ Architecture Overview

The system consists of two main microservices:

- **Ledger Service** (Port 8080): Manages account balances and transaction records
- **Transfer Service** (Port 8081): Handles payment transfers with resilience patterns

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (for Transfer Service)
- **Java 21+** (for Ledger Service)
- **Docker & Docker Compose**
- **PostgreSQL 15+** (if running locally without Docker)
- **Redis 7+** (optional, for caching)

### Option 1: Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd fintech-payments

# Start all services with Docker Compose
docker-compose up --build

# Or run in background
docker-compose up -d --build
```

### Option 2: Local Development

#### 1. Setup Database

```bash
# Start PostgreSQL (using Docker)
docker run --name fintech-postgres \
  -e POSTGRES_DB=fintechpayments \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 -d postgres:15-alpine

# Initialize database schema
psql -h localhost -U postgres -d fintechpayments -f init-db.sql
```

#### 2. Start Ledger Service

```bash
cd fintech-payments-ledger-service
./gradlew bootRun
```

#### 3. Start Transfer Service

```bash
cd fintech-payments-transfer-service
./gradlew bootRun
```

## 🧪 Testing

### Run All Tests

```bash
# From project root
./gradlew test

# Or test individual services
cd fintech-payments-ledger-service && ./gradlew test
cd fintech-payments-transfer-service && ./gradlew test
```

### Test Coverage

```bash
# Generate test coverage reports
./gradlew jacocoTestReport

# View reports at:
# - fintech-payments-ledger-service/build/reports/jacoco/test/html/index.html
# - fintech-payments-transfer-service/build/reports/jacoco/test/html/index.html
```

## 📚 API Documentation

### Swagger UI (Interactive Documentation)

- **Ledger Service**: http://localhost:8080/swagger-ui/index.html
- **Transfer Service**: http://localhost:8081/swagger-ui/index.html

### OpenAPI Specifications

- **Ledger Service**: http://localhost:8080/v3/api-docs
- **Transfer Service**: http://localhost:8081/v3/api-docs

## 🔍 Health Monitoring

### Health Checks

- **Ledger Service**: http://localhost:8080/actuator/health
- **Transfer Service**: http://localhost:8081/actuator/health

### Metrics

- **Ledger Service**: http://localhost:8080/actuator/metrics
- **Transfer Service**: http://localhost:8081/actuator/metrics

## 🛠️ Development

### Building the Applications

```bash
# Build all services
./gradlew build

# Build individual services
cd fintech-payments-ledger-service && ./gradlew build
cd fintech-payments-transfer-service && ./gradlew build
```

### Running Tests with Different Profiles

```bash
# Run with test profile
./gradlew test -Dspring.profiles.active=test

# Run integration tests only
./gradlew integrationTest
```

### Code Quality

```bash
# Run static analysis
./gradlew check

# Format code
./gradlew spotlessApply
```

## 🐳 Docker Commands

### Individual Service Management

```bash
# Build specific service
docker-compose build ledger-service
docker-compose build transfer-service

# Run specific service
docker-compose up ledger-service
docker-compose up transfer-service

# View logs
docker-compose logs -f ledger-service
docker-compose logs -f transfer-service
```

### Database Management

```bash
# Access PostgreSQL
docker-compose exec postgres psql -U postgres -d fintechpayments

# Access Redis
docker-compose exec redis redis-cli
```

## 🔧 Configuration

### Environment Variables

#### Ledger Service
```bash
SPRING_PROFILES_ACTIVE=dev|test|docker
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fintechpayments
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SERVER_PORT=8080
```

#### Transfer Service
```bash
SPRING_PROFILES_ACTIVE=dev|test|docker
LEDGER_SERVICE_URL=http://localhost:8080
SERVER_PORT=8081
```

### Application Profiles

- **`dev`**: Development with H2 database
- **`test`**: Testing with in-memory database
- **`docker`**: Production-like setup with PostgreSQL

## 📊 Key Features

### Ledger Service
- ✅ Account balance management
- ✅ Transaction history tracking
- ✅ RESTful API endpoints
- ✅ Database persistence with JPA/Hibernate
- ✅ Comprehensive API documentation

### Transfer Service
- ✅ Payment transfer processing
- ✅ Circuit breaker pattern (Resilience4j)
- ✅ Retry mechanisms
- ✅ Rate limiting
- ✅ Structured logging
- ✅ Integration with Ledger Service

### Infrastructure
- ✅ Docker containerization
- ✅ Docker Compose orchestration
- ✅ PostgreSQL database
- ✅ Redis caching (optional)
- ✅ Nginx load balancer
- ✅ Health checks and monitoring

## 🚨 Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check if ports are in use
lsof -i :8080
lsof -i :8081

# Kill processes using ports
kill -9 $(lsof -t -i:8080)
kill -9 $(lsof -t -i:8081)
```

#### Database Connection Issues
```bash
# Check PostgreSQL status
docker-compose ps postgres

# Restart database
docker-compose restart postgres

# View database logs
docker-compose logs postgres
```

#### Docker Issues
```bash
# Clean up Docker resources
docker-compose down -v
docker system prune -f

# Rebuild from scratch
docker-compose build --no-cache
```

### Logs Location

- **Local Development**: Console output
- **Docker**: `docker-compose logs <service-name>`
- **Container Logs**: `/app/logs/` (mounted to host volumes)

## 🔐 Security Considerations

- Database credentials should be externalized in production
- API endpoints should be secured with authentication/authorization
- Network communication should use HTTPS in production
- Sensitive data should be encrypted at rest and in transit

## 📈 Performance Tuning

### JVM Options (Already configured in Dockerfiles)
```bash
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
```

### Database Optimization
- Connection pooling configured with HikariCP
- Database indexes on frequently queried columns
- Query optimization with JPA/Hibernate

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the [SOLUTION.md](SOLUTION.md) for design decisions and trade-offs