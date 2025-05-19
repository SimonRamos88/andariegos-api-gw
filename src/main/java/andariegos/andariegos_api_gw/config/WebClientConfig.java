package andariegos.andariegos_api_gw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    
    @Bean
    @Primary
    public WebClient eventServiceWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:9080") // MS Eventos (Spring Boot)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Bean
    public WebClient userServiceWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:4000") // MS Usuarios (NestJS)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}