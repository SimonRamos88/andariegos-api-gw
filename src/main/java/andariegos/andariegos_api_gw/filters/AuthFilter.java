package andariegos.andariegos_api_gw.filters;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import andariegos.andariegos_api_gw.dto.AttendanceResponse;
import andariegos.andariegos_api_gw.dto.AuthResponse;
import andariegos.andariegos_api_gw.dto.GraphQLUsersDetailsResponse;
import andariegos.andariegos_api_gw.dto.LoginRequest;
import andariegos.andariegos_api_gw.dto.RegistationResponse;
import andariegos.andariegos_api_gw.dto.RegisterRequest;
import andariegos.andariegos_api_gw.dto.UserDetailsResponse;
import andariegos.andariegos_api_gw.dto.UsersDetailsResponse;
import ch.qos.logback.core.subst.Token;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuthFilter implements GlobalFilter {

    private final WebClient userServiceWebClient;
    private final WebClient authServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private static final Logger log = LoggerFactory.getLogger(EventRegistrationFilter.class);


    public AuthFilter(
        @Qualifier("userServiceWebClient") WebClient userServiceWebClient,
        @Qualifier("authServiceWebClient") WebClient authServiceWebClient
    ) {
        this.userServiceWebClient = userServiceWebClient;
        this.authServiceWebClient = authServiceWebClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();
        System.out.println("AuthFilter: Entrando al filtro de autenticación 1" + path);
        if (path.matches("/api/auth/login")) {
            return processLoginRequest(exchange);
        } else if (path.matches("/api/auth/register")) {
            return processRegisterRequest(exchange);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> processLoginRequest(ServerWebExchange exchange) {
        return parseRequestBody(exchange)
            .flatMap(this::login)
            .flatMap(authResponse -> fetchUserProfile(authResponse.getAccess_token(), authResponse.getUserId())
                .map(userProfile -> Map.of(
                    "access_token", authResponse.getAccess_token(),
                    "user", userProfile
                ))
            )
            .flatMap(responseBody -> buildSuccessResponse(exchange, responseBody))
            .onErrorResume(error -> buildErrorResponse(exchange, error));
    }

    private Mono<Void> processRegisterRequest(ServerWebExchange exchange) {
        return parseRequestRegisterBody(exchange)
                .flatMap(registerRequest -> 
                    register(registerRequest)
                        .flatMap(authResponse -> 
                            // Pasamos registerRequest como segundo argumento
                            createUserProfile(authResponse.getAccess_token(), registerRequest)
                                .map(userProfile -> Map.of(
                                    "access_token", authResponse.getAccess_token(),
                                    "user", userProfile
                                ))
                        )
                        // Pasamos registerRequest al resto del flujo si es necesario
                ).flatMap(responseBody -> buildSuccessResponse(exchange, responseBody))
                .onErrorResume(error -> buildErrorResponse(exchange, error));
    }
    
    private Mono<RegisterRequest> parseRequestRegisterBody(ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
            .next()
            .flatMap(buffer -> {
                try {
                    String bodyStr = buffer.toString(StandardCharsets.UTF_8);
                    // System.out.println("exito parse");
                    return Mono.just(objectMapper.readValue(bodyStr, RegisterRequest.class));
                } catch (Exception e) {
                    // System.out.println("fracaso parse");       
                    return Mono.error(new RuntimeException("Formato de solicitud inválido"));
                }
            });
    }


    private Mono<AuthResponse> register(RegisterRequest request) {
        System.out.println("AuthFilter: Procesando solicitud de inicio de sesión "+ request.getEmail());
        System.out.println("AuthFilter: Procesando solicitud de inicio de sesión "+ request.getPassword());

        String graphqlMutation = """
            mutation registerUser($createUserInput: CreateUserInput!) {
                registerUser(createUserInput: $createUserInput) {
                    access_token
                }
            }
        """;

        Map<String, Object> createUserInput = Map.of(
            "email", request.getEmail(),
            "username", request.getUsername(),
            "password", request.getPassword(),
            "roles", request.getRoles() != null ? request.getRoles() : List.of("USER") // Default role if none provided
        );

        Map<String, Object> requestBody = Map.of(
            "query", graphqlMutation,
            "variables", Map.of("createUserInput", createUserInput)
        );

        return authServiceWebClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {

                if (response.has("errors")) {
                    String errorMessage = response.get("errors").get(0).get("message").asText();
                    throw new RuntimeException("GraphQL error: " + errorMessage);
                }

                JsonNode registerNode = response.path("data").path("registerUser");
                AuthResponse auth = new AuthResponse();
                auth.setAccess_token(registerNode.path("access_token").asText());
                return auth;
            });
    }

    private Mono<JsonNode> createUserProfile(String token, RegisterRequest request) {

        String graphqlMutation = """
            mutation registerUser($createProfileInput: CreateProfileInput!) {
                registerUser(createProfileInput: $createProfileInput) {
                    userId
                    name
                    nationality
                }
            }
        """;

        Map<String, Object> createProfileInput = Map.of(
            "accessToken", token,
            "name", request.getName(),
            "nationality", request.getNationality(),
            "state", "active" // Default role if none provided
        );

        Map<String, Object> requestBody = Map.of(
            "query", graphqlMutation,
            "variables", Map.of("createProfileInput", createProfileInput)
        );

        return userServiceWebClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody) // deja que WebClient lo serialice correctamente
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                if (response.has("errors")) {
                    String errorMessage = response.get("errors").get(0).get("message").asText();
                    throw new RuntimeException("GraphQL error: " + errorMessage);
                }
                return response.path("data").path("registerUser");
            });
    }

    private Mono<LoginRequest> parseRequestBody(ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
            .next()
            .flatMap(buffer -> {
                try {
                    String bodyStr = buffer.toString(StandardCharsets.UTF_8);
                    // System.out.println("exito parse");
                    return Mono.just(objectMapper.readValue(bodyStr, LoginRequest.class));
                } catch (Exception e) {
                    // System.out.println("fracaso parse");       
                    return Mono.error(new RuntimeException("Formato de solicitud inválido"));
                }
            });
    }

   private Mono<AuthResponse> login(LoginRequest request) {
        System.out.println("AuthFilter: Procesando solicitud de inicio de sesión "+ request.getIdentifier());
        System.out.println("AuthFilter: Procesando solicitud de inicio de sesión "+ request.getPassword());

        String query = String.format("""
            {
                "query": "mutation { login(identifier: \\"%s\\", password: \\"%s\\") { access_token userId } }"
            }
            """, request.getIdentifier(), request.getPassword());

        return authServiceWebClient.post()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(query)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {

                if (response.has("errors")) {
                    String errorMessage = response.get("errors").get(0).get("message").asText();
                    throw new RuntimeException("GraphQL error: " + errorMessage);
                }

                JsonNode loginNode = response.path("data").path("login");
                AuthResponse auth = new AuthResponse();
                auth.setAccess_token(loginNode.path("access_token").asText());
                auth.setUserId(loginNode.path("userId").asText());
                return auth;
            }).doOnError(error -> {
                System.out.println("🔥 Error capturado: " + error.getMessage());
            });
    }

    private Mono<JsonNode> fetchUserProfile(String token, String userId) {
        System.out.println("AuthFilter: Fetching user profile for userId: " + userId);
        System.out.println("AuthFilter: Fetching user profile for token: " + token);
        String graphqlQuery = """
            query User($id: String!) {
                user(id: $id) {
                    userId
                    name
                }
            }
        """;
        // _id
        //             name
        //             username
        //             email
        //             registrationDate
        //             state

        // Crear estructura JSON como Map
        Map<String, Object> body = new HashMap<>();
        body.put("query", graphqlQuery);

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", userId);

        body.put("variables", variables);

        return userServiceWebClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body) // deja que WebClient lo serialice correctamente
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(response -> {
                if (response.has("errors")) {
                    String errorMessage = response.get("errors").get(0).get("message").asText();
                    throw new RuntimeException("GraphQL error: " + errorMessage);
                }
                return response.path("data").path("user");
            });
    }

    private Mono<Void> buildSuccessResponse(ServerWebExchange exchange, Map<String, Object> data) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(data);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(json)));
        } catch (Exception e) {
            return buildErrorResponse(exchange, e);
        }
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, Throwable error) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = ("{\"error\": \"" + error.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }




}
