package za.co.titandynamix.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fintech Payments - Ledger Service API")
                        .version("1.0.0")
                        .description("The Ledger Service provides core financial ledger functionality with double-entry bookkeeping."));
    }
}