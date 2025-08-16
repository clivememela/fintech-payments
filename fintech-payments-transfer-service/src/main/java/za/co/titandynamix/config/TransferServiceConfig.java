package za.co.titandynamix.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Transfer Service beans and settings.
 */
@Configuration
public class TransferServiceConfig {
    
    /**
     * ObjectMapper bean for JSON serialization/deserialization.
     * Configured with JavaTimeModule for proper LocalDateTime handling.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}