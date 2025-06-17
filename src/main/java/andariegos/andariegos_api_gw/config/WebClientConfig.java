package andariegos.andariegos_api_gw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
;


@Configuration
public class WebClientConfig {


    @Value("${AUTH_SERVICE}")
    private String authServiceUrl;

    @Value("${EVENT_SERVICE}")
    private String eventServiceUrl;

    @Value("${PROFILE_SERVICE}")
    private String userServiceUrl;

    @Value("${CLIENT_SERVICE}")
    private String clientServiceUrl;

    @Value("${REPORTS_SERVICE}")
    private String reportsServiceUrl;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin(clientServiceUrl);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addExposedHeader("Authorization");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
    
    @Bean
    @Primary
    public WebClient eventServiceWebClient() {
        return WebClient.builder()
            .baseUrl(eventServiceUrl) // MS Eventos (Spring Boot)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

     @Bean
    public WebClient authServiceWebClient() {
        return WebClient.builder()
            .baseUrl(authServiceUrl+"/graphql") // MS Usuarios (NestJS)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .defaultHeader("x-apollo-operation-name", "GraphQLRequest") // Header anti-CSRF
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


    @Bean
    public WebClient reportServiceWebClient() {
        return WebClient.builder()
            .baseUrl(reportsServiceUrl) // MS Eventos (Spring Boot)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}