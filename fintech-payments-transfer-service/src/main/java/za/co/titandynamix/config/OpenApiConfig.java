package za.co.titandynamix.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * OpenAPI configuration for Transfer Service
 * Only enabled in development and test profiles for security
 */
@Configuration
// @Profile({"!production", "!aws"}) // Temporarily disabled for debugging
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI transferServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fintech Payments - Transfer Service API")
                        .description("""
                                # Transfer Service API
                                
                                The Transfer Service orchestrates payment transfers with comprehensive concurrency control,
                                idempotency guarantees, and circuit breaker patterns for high-availability operations.
                                
                                ## Key Features
                                - **Idempotency**: All operations support idempotency keys to prevent duplicate processing
                                - **Concurrency**: Batch operations with CompletableFuture for parallel processing
                                - **Circuit Breaker**: Resilience4j integration for fault tolerance
                                - **Monitoring**: Comprehensive health checks and metrics
                                
                                ## Authentication
                                This API uses idempotency keys for request deduplication. Include the `Idempotency-Key` 
                                header with a unique value for each request.
                                
                                ## Rate Limiting
                                - Single transfers: 100 requests per minute per client
                                - Batch transfers: 10 requests per minute per client
                                - Maximum batch size: 100 transfers
                                
                                ## Error Handling
                                The API returns standard HTTP status codes:
                                - `200 OK`: Successful operation
                                - `201 Created`: Resource created successfully
                                - `400 Bad Request`: Invalid request parameters
                                - `404 Not Found`: Resource not found
                                - `409 Conflict`: Duplicate request (idempotency violation)
                                - `422 Unprocessable Entity`: Business logic error
                                - `500 Internal Server Error`: System error
                                - `503 Service Unavailable`: Circuit breaker open
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Fintech Payments Team")
                                .email("payments@titandynamix.co.za")
                                .url("https://titandynamix.co.za"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Docker Compose Environment"),
                        new Server()
                                .url("https://api.payments.titandynamix.co.za")
                                .description("Production Server (Documentation Only)")
                ))
                .components(new Components()
                        .addSecuritySchemes("IdempotencyKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Idempotency-Key")
                                .description("Unique key to ensure idempotent operations")))
                .addSecurityItem(new SecurityRequirement().addList("IdempotencyKey"));
    }
}