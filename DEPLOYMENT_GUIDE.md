# Fintech Payments System - Deployment Guide

## 🚀 Quick Start Options

### Option 1: Local Development with Docker Compose
```bash
# Start all services locally
./docker-scripts/start.sh

# Test the setup
./test-docker-setup.sh

# Stop services
./docker-scripts/stop.sh
```

### Option 2: AWS Production Deployment
```bash
# Deploy to AWS (requires AWS CLI configured)
./aws/deploy.sh production us-east-1

# Or deploy to staging
./aws/deploy.sh staging us-west-2
```

## 📋 Prerequisites

### Local Development
- Docker Desktop
- Docker Compose
- Git

### AWS Deployment
- AWS CLI v2
- Docker
- Valid AWS credentials
- SSL Certificate in AWS Certificate Manager

## 🏗️ Architecture Overview

### Local (Docker Compose)
```
┌─────────────────┐    ┌─────────────────┐
│  Transfer       │    │  Ledger         │
│  Service        │◄──►│  Service        │
│  (Port 8080)    │    │  (Port 8081)    │
└─────────────────┘    └─────────────────┘
         │                       │
         └───────────┬───────────┘
                     ▼
         ┌─────────────────┐    ┌─────────────────┐
         │  PostgreSQL     │    │  Redis          │
         │  (Port 5433)    │    │  (Port 6379)    │
         └─────────────────┘    └─────────────────┘
```

### AWS Production
```
Internet Gateway
    ↓
Application Load Balancer (HTTPS)
    ↓
ECS Fargate Cluster
├── Transfer Service Tasks (Auto Scaling 2-10)
└── Ledger Service Tasks (Auto Scaling 2-10)
    ↓
┌─────────────────┐    ┌─────────────────┐
│  RDS PostgreSQL │    │  ElastiCache    │
│  (Multi-AZ)     │    │  Redis Cluster  │
└─────────────────┘    └─────────────────┘
```

## 🐳 Docker Configuration

### Multi-Stage Builds
Both services use optimized multi-stage Docker builds:
- **Builder Stage**: Gradle with full JDK for compilation
- **Runtime Stage**: JRE slim for minimal production footprint
- **Security**: Non-root user execution
- **Health Checks**: Built-in container health monitoring

### Key Features
- **Health Checks**: All containers have health monitoring
- **Dependency Management**: Services start in correct order
- **Volume Persistence**: Database data persists across restarts
- **Network Isolation**: Services communicate via custom bridge network

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow
The pipeline includes 4 main stages:

1. **Test Stage**
   - Multi-JDK setup (JDK 21 for Ledger, JDK 17 for Transfer)
   - PostgreSQL and Redis test services
   - Gradle test execution with caching
   - Test result reporting

2. **Security Scan**
   - Trivy vulnerability scanning
   - SARIF report upload to GitHub Security tab
   - Dependency and filesystem scanning

3. **Build & Push**
   - Docker Buildx for multi-platform builds
   - GitHub Container Registry (GHCR) integration
   - Automated tagging (branch, SHA, latest)

4. **Integration Tests**
   - Full stack deployment with Docker Compose
   - API endpoint testing
   - Health check validation

### Pipeline Triggers
- **Push to main**: Full pipeline with deployment
- **Pull Requests**: Tests and security scans only
- **Manual**: On-demand execution

## ☁️ AWS Deployment Architecture

### Infrastructure Components

#### 1. Networking
- **VPC**: Custom VPC with public/private subnets
- **NAT Gateway**: Outbound internet access for private subnets
- **Security Groups**: Layered security with least privilege

#### 2. Compute (ECS Fargate)
- **Serverless**: No EC2 instances to manage
- **Auto Scaling**: 2-10 tasks based on CPU utilization
- **Blue-Green Deployment**: Zero-downtime deployments
- **Health Checks**: Application-level health monitoring

#### 3. Database (RDS PostgreSQL)
- **Multi-AZ**: High availability with automatic failover
- **Encrypted**: Data encryption at rest and in transit
- **Automated Backups**: 7-day retention with point-in-time recovery
- **Performance Insights**: Query performance monitoring

#### 4. Caching (ElastiCache Redis)
- **Cluster Mode**: High availability Redis cluster
- **Encryption**: At-rest and in-transit encryption
- **Automatic Failover**: Multi-AZ deployment

#### 5. Load Balancing (ALB)
- **HTTPS Termination**: SSL/TLS certificate management
- **Health Checks**: Application health monitoring
- **Path-Based Routing**: Route to appropriate services

### Security Features
- **IAM Roles**: Least privilege access
- **Secrets Manager**: Secure credential storage
- **VPC Security Groups**: Network-level security
- **Encryption**: Data encrypted at rest and in transit

## 📊 Monitoring & Observability

### Health Checks
```bash
# Local Development
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# AWS Production
curl https://your-alb-dns/actuator/health
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Circuit breaker status
curl http://localhost:8080/actuator/circuitbreakers

# Application metrics
curl http://localhost:8080/actuator/metrics
```

### Logs
```bash
# Docker Compose
docker-compose logs -f transfer-service
docker-compose logs -f ledger-service

# AWS CloudWatch
aws logs tail /ecs/production-transfer-service --follow
```

## 🧪 Testing

### Local Testing
```bash
# Unit tests
./gradlew test

# Integration tests with Docker
docker-compose up -d
./test-docker-setup.sh

# Load testing
ab -n 1000 -c 10 -H "Content-Type: application/json" \
   -H "Idempotency-Key: load-test-$(date +%s)" \
   -p test-payload.json \
   http://localhost:8080/api/transfers
```

### API Testing
```bash
# Create transfer
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-123" \
  -d '{
    "fromAccountId": "acc-123",
    "toAccountId": "acc-456",
    "amount": 100.00
  }'

# Batch transfers
curl -X POST http://localhost:8080/api/transfers/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"fromAccountId": "acc-123", "toAccountId": "acc-456", "amount": 100.00},
    {"fromAccountId": "acc-789", "toAccountId": "acc-012", "amount": 250.00}
  ]'

# Get transfer status
curl http://localhost:8080/api/transfers/{transfer-id}
```

## 🔧 Configuration

### Environment Variables

#### Transfer Service
```bash
SPRING_PROFILES_ACTIVE=docker|aws
LEDGER_SERVICE_URL=http://ledger-service:8081
SERVER_PORT=8080
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
```

#### Ledger Service
```bash
SPRING_PROFILES_ACTIVE=docker|aws
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/fintechpayments
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
SERVER_PORT=8081
```

### Application Profiles
- **default**: Local development with H2 database
- **docker**: Docker Compose environment
- **aws**: AWS ECS deployment with RDS and ElastiCache
- **test**: Testing environment with embedded databases

## 🚨 Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check if ports are in use
lsof -i :8080
lsof -i :8081
lsof -i :5433

# Kill processes using ports
sudo kill -9 $(lsof -t -i:8080)
```

#### Docker Issues
```bash
# Clean up Docker resources
docker system prune -a
docker volume prune

# Restart Docker Desktop
# macOS: Restart Docker Desktop application
# Linux: sudo systemctl restart docker
```

#### Database Connection Issues
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Connect to database directly
docker-compose exec postgres psql -U postgres -d fintechpayments
```

#### Service Health Issues
```bash
# Check service logs
docker-compose logs transfer-service
docker-compose logs ledger-service

# Check health endpoints
curl -v http://localhost:8080/actuator/health
curl -v http://localhost:8081/actuator/health
```

### AWS Troubleshooting

#### ECS Service Issues
```bash
# Check service status
aws ecs describe-services --cluster production-fintech-cluster --services production-transfer-service

# Check task logs
aws logs tail /ecs/production-transfer-service --follow

# Check task health
aws ecs describe-tasks --cluster production-fintech-cluster --tasks <task-arn>
```

#### Database Connection Issues
```bash
# Check RDS status
aws rds describe-db-instances --db-instance-identifier production-fintech-postgres

# Check security groups
aws ec2 describe-security-groups --group-ids <security-group-id>
```

## 📈 Performance Optimization

### JVM Tuning
```bash
# Container-optimized JVM settings
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Database Optimization
```sql
-- Create indexes for better performance
CREATE INDEX CONCURRENTLY idx_ledger_entry_transfer_id ON ledger_entry(transfer_id);
CREATE INDEX CONCURRENTLY idx_ledger_entry_account_id ON ledger_entry(account_id);
CREATE INDEX CONCURRENTLY idx_account_balance ON account(balance) WHERE balance > 0;
```

### Connection Pool Tuning
```properties
# HikariCP settings
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

## 🔒 Security Best Practices

### Container Security
- Non-root user execution
- Minimal base images (JRE slim)
- Regular security scanning with Trivy
- No secrets in Docker images

### Application Security
- Input validation with Bean Validation
- SQL injection prevention with JPA
- Rate limiting via Nginx/ALB
- Circuit breaker protection

### AWS Security
- IAM roles with least privilege
- Secrets Manager for credentials
- VPC security groups
- Encryption at rest and in transit

## 📞 Support

### Getting Help
1. Check this deployment guide
2. Review the [README.md](README.md) for API documentation
3. Check the [SOLUTION.md](SOLUTION.md) for implementation details
4. Create an issue in the repository
5. Review CI/CD pipeline logs for deployment issues

### Useful Commands
```bash
# View all running containers
docker ps

# Check Docker Compose status
docker-compose ps

# View resource usage
docker stats

# Clean up everything
./docker-scripts/stop.sh --all
```

---

Built with ❤️ for enterprise-grade fintech applications