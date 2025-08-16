# Fintech Payments System - API Documentation

## 🚀 Quick Access

### Swagger UI (Development Only)
- **Transfer Service**: http://localhost:8080/swagger-ui.html
- **Ledger Service**: http://localhost:8081/swagger-ui.html

### OpenAPI Specifications
- **Transfer Service**: http://localhost:8080/v3/api-docs
- **Ledger Service**: http://localhost:8081/v3/api-docs

> **Security Note**: Swagger UI is automatically disabled in `production` and `aws` profiles for security.

## 📋 API Overview

### Transfer Service (Port 8080)
Orchestrates payment transfers with idempotency, concurrency control, and circuit breaker patterns.

### Ledger Service (Port 8081)
Provides atomic double-entry bookkeeping with ACID compliance and pessimistic locking.

## 🔐 Authentication & Security

### Idempotency Keys
All transfer operations require an `Idempotency-Key` header to prevent duplicate processing:

```bash
curl -H "Idempotency-Key: unique-key-123" \
     -H "Content-Type: application/json" \
     -X POST http://localhost:8080/api/transfers \
     -d '{"fromAccountId": 123, "toAccountId": 456, "amount": 100.00}'
```

### Key Requirements
- **Uniqueness**: Each key must be unique across all requests
- **TTL**: Keys expire after 24 hours
- **Format**: Alphanumeric string, 1-255 characters
- **Recommendation**: Use format like `{client-id}-{timestamp}-{random}`

## 📡 Transfer Service API

### Base URL
- **Local**: `http://localhost:8080/api`
- **Docker**: `http://localhost:8080/api`
- **Production**: `https://api.payments.titandynamix.co.za/api`

### Endpoints

#### 1. Create Transfer
```http
POST /transfers
Content-Type: application/json
Idempotency-Key: transfer-2024-001-abc123

{
  "fromAccountId": 123,
  "toAccountId": 456,
  "amount": 100.50
}
```

**Response (201 Created):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "SUCCEEDED",
  "failureReason": null
}
```

**Possible Status Values:**
- `PENDING`: Transfer is being processed
- `SUCCEEDED`: Transfer completed successfully
- `FAILED`: Transfer failed (see failureReason)
- `ERROR`: System error occurred

#### 2. Get Transfer Status
```http
GET /transfers/{id}
```

**Response (200 OK):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "SUCCEEDED",
  "failureReason": null
}
```

#### 3. Batch Transfers
```http
POST /transfers/batch
Content-Type: application/json

[
  {
    "fromAccountId": 123,
    "toAccountId": 456,
    "amount": 100.00
  },
  {
    "fromAccountId": 789,
    "toAccountId": 012,
    "amount": 250.00
  }
]
```

**Features:**
- Maximum 100 transfers per batch
- Parallel processing with CompletableFuture
- Individual failure handling
- 30-second timeout protection

#### 4. Health Check
```http
GET /health
```

**Response (200 OK):**
```
Transfer Service is running
```

## 🏦 Ledger Service API

### Base URL
- **Local**: `http://localhost:8081/api`
- **Docker**: `http://localhost:8081/api`
- **Production**: `https://ledger.payments.titandynamix.co.za/api`

### Endpoints

#### 1. Create Account
```http
POST /accounts
Content-Type: application/json
Idempotency-Key: account-creation-123

{
  "initialBalance": 1000.00,
  "accountType": "CHECKING"
}
```

#### 2. Get Account
```http
GET /accounts/{accountId}
```

**Response (200 OK):**
```json
{
  "id": 123,
  "balance": 1000.00,
  "version": 1,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

#### 3. Get Account Balance
```http
GET /accounts/{accountId}/balance
```

**Response (200 OK):**
```json
1000.00
```

#### 4. Apply Ledger Transfer
```http
POST /ledger/transfer
Content-Type: application/json

{
  "transferId": "123e4567-e89b-12d3-a456-426614174000",
  "fromAccountId": 123,
  "toAccountId": 456,
  "amount": 100.00
}
```

**Response (200 OK):**
```json
{
  "status": "success",
  "message": "Transfer applied successfully"
}
```

#### 5. Create and Process Transfer
```http
POST /ledger/transfers
Content-Type: application/json
Idempotency-Key: ledger-transfer-123

{
  "fromAccountId": 123,
  "toAccountId": 456,
  "amount": 100.00
}
```

#### 6. Get Transfer Status
```http
GET /ledger/transfers/{transferId}
```

## 📊 Monitoring Endpoints

### Health Checks
```bash
# Transfer Service
curl http://localhost:8080/actuator/health

# Ledger Service
curl http://localhost:8081/actuator/health
```

### Circuit Breaker Status
```bash
# Circuit breaker state
curl http://localhost:8080/actuator/circuitbreakers

# Circuit breaker events
curl http://localhost:8080/actuator/circuitbreakerevents
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8081/actuator/prometheus

# Application metrics
curl http://localhost:8080/actuator/metrics
curl http://localhost:8081/actuator/metrics
```

## 🧪 Testing Examples

### Single Transfer Test
```bash
#!/bin/bash
IDEMPOTENCY_KEY="test-$(date +%s)-$(uuidgen)"

curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{
    "fromAccountId": 123,
    "toAccountId": 456,
    "amount": 100.00
  }' | jq '.'
```

### Batch Transfer Test
```bash
#!/bin/bash
curl -X POST http://localhost:8080/api/transfers/batch \
  -H "Content-Type: application/json" \
  -d '[
    {"fromAccountId": 123, "toAccountId": 456, "amount": 100.00},
    {"fromAccountId": 789, "toAccountId": 012, "amount": 250.00},
    {"fromAccountId": 345, "toAccountId": 678, "amount": 75.50}
  ]' | jq '.'
```

### Idempotency Test
```bash
#!/bin/bash
IDEMPOTENCY_KEY="idempotency-test-123"

echo "First request:"
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{"fromAccountId": 123, "toAccountId": 456, "amount": 100.00}' | jq '.'

echo -e "\nSecond request (should return same result):"
curl -X POST http://localhost:8080/api/transfers \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d '{"fromAccountId": 123, "toAccountId": 456, "amount": 100.00}' | jq '.'
```

### Load Testing
```bash
#!/bin/bash
# Install Apache Bench: brew install httpie (macOS) or apt-get install apache2-utils (Ubuntu)

# Create test payload
cat > test-payload.json << EOF
{
  "fromAccountId": 123,
  "toAccountId": 456,
  "amount": 100.00
}
EOF

# Run load test
ab -n 1000 -c 10 \
   -H "Content-Type: application/json" \
   -H "Idempotency-Key: load-test-$(date +%s)" \
   -p test-payload.json \
   http://localhost:8080/api/transfers
```

## 🚨 Error Handling

### HTTP Status Codes
- **200 OK**: Successful operation
- **201 Created**: Resource created successfully
- **400 Bad Request**: Invalid request parameters
- **404 Not Found**: Resource not found
- **409 Conflict**: Duplicate request (idempotency violation)
- **422 Unprocessable Entity**: Business logic error
- **500 Internal Server Error**: System error
- **503 Service Unavailable**: Circuit breaker open

### Error Response Format
```json
{
  "error": "Bad Request",
  "message": "Amount must be positive",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/transfers"
}
```

### Common Error Scenarios

#### Insufficient Funds
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "FAILED",
  "failureReason": "Insufficient funds in source account"
}
```

#### Invalid Account
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "FAILED",
  "failureReason": "Source account not found"
}
```

#### Circuit Breaker Open
```json
{
  "error": "Service Unavailable",
  "message": "Ledger service is temporarily unavailable",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 🔧 Configuration

### Environment Variables
```bash
# Transfer Service
SPRING_PROFILES_ACTIVE=development
LEDGER_SERVICE_URL=http://localhost:8081
SERVER_PORT=8080

# Ledger Service
SPRING_PROFILES_ACTIVE=development
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/fintechpayments
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SERVER_PORT=8081
```

### Profile-Specific Settings

#### Development Profile
- Swagger UI enabled
- Detailed logging
- H2 in-memory database option
- Debug endpoints exposed

#### Docker Profile
- Swagger UI enabled for testing
- PostgreSQL and Redis connections
- Container-optimized settings

#### Production Profile
- Swagger UI disabled
- Minimal logging
- Security hardening
- Performance optimization

#### AWS Profile
- Swagger UI disabled
- CloudWatch logging
- RDS and ElastiCache integration
- ECS-specific health checks

## 📚 Additional Resources

### OpenAPI Specifications
- Download OpenAPI specs in JSON format from `/v3/api-docs`
- Import into Postman, Insomnia, or other API clients
- Generate client SDKs using OpenAPI Generator

### Postman Collection
```bash
# Import OpenAPI spec into Postman
curl http://localhost:8080/v3/api-docs > transfer-service-openapi.json
curl http://localhost:8081/v3/api-docs > ledger-service-openapi.json
```

### Client SDK Generation
```bash
# Generate Java client
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g java \
  -o ./generated-client-java

# Generate Python client
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g python \
  -o ./generated-client-python
```

---

For more detailed implementation information, see [SOLUTION.md](SOLUTION.md) and [README.md](README.md).