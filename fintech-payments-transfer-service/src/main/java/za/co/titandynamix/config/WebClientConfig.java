package za.co.titandynamix.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Creates a WebClient bean configured for communication with the ledger service.
     *
     * @param builder the WebClient.Builder instance used to build the WebClient
     * @param baseUrl the base URL for the ledger service, defaulting to "http://localhost:8081"
     * @param connectTimeoutMs the connection timeout in milliseconds, defaulting to 5000 ms
     * @param responseTimeoutMs the response timeout in milliseconds, defaulting to 10000 ms
     * @return a configured WebClient instance for the ledger service
     */
    @Bean
    public WebClient ledgerWebClient(
            WebClient.Builder builder,
            @Value("${ledger.base-url:http://localhost:8081}") String baseUrl,
            @Value("${ledger.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${ledger.response-timeout-ms:10000}") long responseTimeoutMs
    ) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler((int) (responseTimeoutMs / 1000)))
                        .addHandlerLast(new WriteTimeoutHandler((int) (responseTimeoutMs / 1000)))
                );

        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}