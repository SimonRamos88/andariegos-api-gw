package andariegos.andariegos_api_gw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;


@Configuration
public class WebClientConfig {

    @Value("${EVENT_SERVICE}")
    private String eventServiceUrl;

    @Value("${PROFILE_SERVICE}")
    private String userServiceUrl;
    
    @Bean
    @Primary
    public WebClient eventServiceWebClient() {
        return WebClient.builder()
            .baseUrl(eventServiceUrl) // MS Eventos (Spring Boot)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
            .baseUrl(userServiceUrl+"/graphql") // MS Usuarios (NestJS)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader("x-apollo-operation-name", "GraphQLRequest") // Header anti-CSRF
            .build();

    }
}