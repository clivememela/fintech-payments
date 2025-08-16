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
 * OpenAPI configuration for Ledger Service
 * Only enabled in development and test profiles for security
 */
@Configuration
@Profile({"!production", "!aws"}) // Exclude from production and AWS profiles
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI ledgerServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fintech Payments - Ledger Service API")
                        .description("""
                                # Ledger Service API
                                
                                The Ledger Service provides atomic double-entry bookkeeping with comprehensive 
                                concurrency control, ensuring ACID compliance for all financial transactions.
                                
                                ## Key Features
                                - **Atomic Transactions**: All operations are ACID-compliant
                                - **Double-Entry Bookkeeping**: Every transfer creates both debit and credit entries
                                - **Pessimistic Locking**: SELECT FOR UPDATE prevents race conditions
                                - **Optimistic Locking**: Version-based conflict resolution with retry logic
                                - **Idempotency**: Database-level constraints prevent duplicate entries
                                
                                ## Database Design
                                - **Accounts**: Store account balances with version control
                                - **Ledger Entries**: Immutable transaction records
                                - **Unique Constraints**: Prevent duplicate transactions per transfer
                                
                                ## Concurrency Control
                                - **Serializable Isolation**: Maximum consistency for critical operations
                                - **Deterministic Lock Ordering**: Prevents deadlocks
                                - **Retry Logic**: Automatic retry on optimistic lock failures
                                
                                ## Performance
                                - **Connection Pooling**: HikariCP for optimal database connections
                                - **Indexed Queries**: Optimized database access patterns
                                - **Batch Operations**: Efficient bulk processing capabilities
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
                                .url("http://localhost:8081")
                                .description("Docker Compose Environment"),
                        new Server()
                                .url("https://ledger.payments.titandynamix.co.za")
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