# Swagger/OpenAPI Documentation Guide

## Overview

The Fintech Payments system includes comprehensive Swagger/OpenAPI 3.0 documentation for both services. The documentation is automatically generated and provides interactive API exploration capabilities.

## 🚀 Quick Access

### Service URLs

| Service | Local Development | Docker Compose | Description |
|---------|------------------|----------------|-------------|
| **Ledger Service** | http://localhost:8080/swagger-ui.html | http://localhost:8080/swagger-ui.html | Account management & atomic transactions |
| **Transfer Service** | http://localhost:8081/swagger-ui.html | http://localhost:8081/swagger-ui.html | Payment transfers & batch operations |

### API Documentation URLs

| Service | OpenAPI JSON | Description |
|---------|-------------|-------------|
| **Ledger Service** | http://localhost:8080/v3/api-docs | Raw OpenAPI 3.0 specification |
| **Transfer Service** | http://localhost:8081/v3/api-docs | Raw OpenAPI 3.0 specification |

## 📋 API Overview

### Ledger Service API (Port 8080)

**Core Features:**
- ✅ Atomic double-entry bookkeeping
- ✅ ACID-compliant transactions
- ✅ Pessimistic & optimistic locking
- ✅ Idempotency support
- ✅ Comprehensive concurrency control

**Key Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/accounts` | Create account with initial balance |
| `GET` | `/api/accounts` | List all accounts |
| `GET` | `/api/accounts/{id}` | Get account details |
| `GET` | `/api/accounts/{id}/balance` | Get account balance |
| `POST` | `/api/ledger/transfer` | Apply atomic transfer |
| `POST` | `/api/ledger/transfers` | Create and process transfer |
| `GET` | `/api/ledger/transfers/{id}` | Get transfer status |

### Transfer Service API (Port 8081)

**Core Features:**
- ✅ Idempotent transfer operations
- ✅ Batch processing with parallel execution
- ✅ Circuit breaker patterns
- ✅ Comprehensive monitoring
- ✅ High-performance concurrency

**Key Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health` | Service health check |
| `POST` | `/api/transfers` | Create single transfer |
| `GET` | `/api/transfers/{id}` | Get transfer status |
| `POST` | `/api/transfers/batch` | Create batch transfers (max 100) |

## 🔧 Configuration

### Environment-Based Documentation

The Swagger UI is configured to be **environment-aware**:

```java
@Profile({"!production", "!aws"}) // Exclude from production and AWS profiles
```

**Available Profiles:**
- ✅ **Development**: Full Swagger UI enabled
- ✅ **Test**: Full Swagger UI enabled  
- ❌ **Production**: Swagger UI disabled for security
- ❌ **AWS**: Swagger UI disabled for security

### Server Configuration

Each service automatically detects the correct server URLs:

```yaml
# Ledger Service (application.yml)
server:
  port: 8080

# Transfer Service (application.yml)  
server:
  port: 8081
```

## 🔐 Security & Authentication

### Idempotency Keys

Both services use **Idempotency-Key** headers for request deduplication:

```http
POST /api/transfers
Content-Type: application/json
Idempotency-Key: transfer-2024-001-abc123

{
  "fromAccountId": 123,
  "toAccountId": 456,
  "amount": 100.50
}
```

**Key Features:**
- ✅ Prevents duplicate processing
- ✅ 24-hour TTL for idempotency records
- ✅ Returns cached response for duplicates
- ✅ Thread-safe implementation

### Security Schemes

The OpenAPI specification includes security scheme definitions:

```yaml
components:
  securitySchemes:
    IdempotencyKey:
      type: apiKey
      in: header
      name: Idempotency-Key
      description: Unique key to ensure idempotent operations
```

## 📊 API Examples

### 1. Create Account

```bash
curl -X POST "http://localhost:8080/api/accounts" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: account-create-001" \
  -d '{
    "accountNumber": "ACC-001",
    "balance": 1000.00
  }'
```

### 2. Create Transfer

```bash
curl -X POST "http://localhost:8081/api/transfers" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: transfer-001" \
  -d '{
    "fromAccountId": 123,
    "toAccountId": 456,
    "amount": 100.50
  }'
```

### 3. Batch Transfers

```bash
curl -X POST "http://localhost:8081/api/transfers/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "fromAccountId": 123,
      "toAccountId": 456,
      "amount": 50.00
    },
    {
      "fromAccountId": 123,
      "toAccountId": 789,
      "amount": 25.00
    }
  ]'
```

## 🎯 Interactive Testing

### Using Swagger UI

1. **Navigate to Swagger UI**: http://localhost:8080/swagger-ui.html
2. **Select Server**: Choose appropriate server from dropdown
3. **Authorize**: Add Idempotency-Key in the security section
4. **Try It Out**: Click "Try it out" on any endpoint
5. **Execute**: Fill parameters and click "Execute"

### Sample Test Flow

1. **Create Accounts**:
   ```
   POST /api/accounts (Ledger Service)
   - Create source account with balance 1000.00
   - Create destination account with balance 0.00
   ```

2. **Execute Transfer**:
   ```
   POST /api/transfers (Transfer Service)
   - Transfer 100.00 from source to destination
   - Use unique Idempotency-Key
   ```

3. **Check Status**:
   ```
   GET /api/transfers/{id} (Transfer Service)
   - Verify transfer status is "SUCCEEDED"
   ```

4. **Verify Balances**:
   ```
   GET /api/accounts/{id}/balance (Ledger Service)
   - Check both account balances updated correctly
   ```

## 🔍 Advanced Features

### Schema Validation

All DTOs include comprehensive validation annotations:

```java
@Schema(description = "Transfer amount (must be positive)",
        example = "100.50",
        required = true,
        minimum = "0.01")
@NotNull @Positive 
BigDecimal amount
```

### Response Examples

Each endpoint includes realistic response examples:

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "status": "SUCCEEDED",
  "failureReason": null
}
```

### Error Documentation

Comprehensive error response documentation:

| Status Code | Description | Example |
|-------------|-------------|---------|
| `200` | Success | Operation completed |
| `201` | Created | Resource created |
| `400` | Bad Request | Invalid parameters |
| `404` | Not Found | Resource not found |
| `409` | Conflict | Duplicate request |
| `422` | Unprocessable Entity | Business logic error |
| `503` | Service Unavailable | Circuit breaker open |

## 🚀 Getting Started

### 1. Start Services

```bash
# Using Docker Compose
docker-compose up -d

# Or start individually
./gradlew :fintech-payments-ledger-service:bootRun
./gradlew :fintech-payments-transfer-service:bootRun
```

### 2. Access Documentation

- **Ledger Service**: http://localhost:8080/swagger-ui.html
- **Transfer Service**: http://localhost:8081/swagger-ui.html

### 3. Test APIs

Use the interactive Swagger UI to:
- ✅ Explore all available endpoints
- ✅ View request/response schemas
- ✅ Execute test requests
- ✅ Download OpenAPI specifications

## 📝 Customization

### Adding New Endpoints

1. **Add Controller Method**:
   ```java
   @Operation(summary = "Your Operation", description = "Detailed description")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "Success")
   })
   @GetMapping("/your-endpoint")
   public ResponseEntity<YourResponse> yourMethod() {
       // Implementation
   }
   ```

2. **Add DTO Annotations**:
   ```java
   @Schema(description = "Your DTO description")
   public record YourDto(
       @Schema(description = "Field description", example = "example")
       String field
   ) {}
   ```

### Customizing OpenAPI Configuration

Edit the `OpenApiConfig.java` files to:
- ✅ Update service information
- ✅ Add custom security schemes
- ✅ Configure additional servers
- ✅ Add global parameters

## 🔧 Troubleshooting

### Common Issues

1. **Swagger UI Not Loading**:
   - Check service is running on correct port
   - Verify profile is not production/aws
   - Check browser console for errors

2. **Authentication Issues**:
   - Ensure Idempotency-Key header is set
   - Use unique keys for each request
   - Check key format and length

3. **CORS Issues**:
   - Swagger UI should work from same origin
   - For cross-origin, configure CORS in Spring Boot

### Debug Information

Enable debug logging:

```yaml
logging:
  level:
    org.springdoc: DEBUG
    io.swagger: DEBUG
```

## 📚 Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)

---

## 🎉 Ready to Explore!

Your Swagger documentation is fully configured and ready to use. Navigate to the URLs above to start exploring the interactive API documentation!