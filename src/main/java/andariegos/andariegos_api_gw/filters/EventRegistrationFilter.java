package andariegos.andariegos_api_gw.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import andariegos.andariegos_api_gw.dto.GraphQLUserResponse;
import andariegos.andariegos_api_gw.dto.RegistationResponse;
import andariegos.andariegos_api_gw.dto.RegistationSucceedResponse;
import andariegos.andariegos_api_gw.dto.UserDetailsResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EventRegistrationFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(EventRegistrationFilter.class);

    private final WebClient eventServiceWebClient;
    private final WebClient userServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventRegistrationFilter(
        @Qualifier("eventServiceWebClient") WebClient eventServiceWebClient,
        @Qualifier("userServiceWebClient") WebClient userServiceWebClient
    ) {
        this.eventServiceWebClient = eventServiceWebClient;
        this.userServiceWebClient = userServiceWebClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isRegistrationRequest(exchange.getRequest())) {
            // System.out.println("no es reg request");
            return chain.filter(exchange);
        }


        return processRegistrationRequest(exchange);
    }

    private boolean isRegistrationRequest(ServerHttpRequest request) {
        return request.getPath().toString().equals("/api/events/registration") 
            && request.getMethod() == HttpMethod.POST;
    }

    private Mono<Void> processRegistrationRequest(ServerWebExchange exchange) {
        Mono<RegistationResponse> attendanceRequest = parseRequestBody(exchange).cache();

        return attendanceRequest
            .flatMap(request -> {

                log.info("antes de validate user");
                return  validateUserExists(request)
                    .then(attendanceRequest);
                
            }               
            )
            .flatMap(this::registerAttendance)
            .flatMap(response -> 
            {   log.info("estamos en response");
                log.info(response.toString());
                return buildSuccessResponse(exchange, response);})
            .onErrorResume(error -> buildErrorResponse(exchange, error));
    }

    private Mono<RegistationResponse> parseRequestBody(ServerWebExchange exchange) {
        return exchange.getRequest().getBody()
            .next()
            .flatMap(buffer -> {
                try {
                    String bodyStr = buffer.toString(StandardCharsets.UTF_8);
                    // System.out.println("exito parse");
                    return Mono.just(objectMapper.readValue(bodyStr, RegistationResponse.class));
                } catch (Exception e) {
                    // System.out.println("fracaso parse");       
                    return Mono.error(new RuntimeException("Formato de solicitud inválido"));
                }
            });
    }

    private Mono<GraphQLUserResponse> validateUserExists(RegistationResponse request) {
        
        log.info("Iniciando validación de usuario para ID: {}", request.getUserId()); // Nuevo log
    
        String userId = request.getUserId();


        String graphqlQuery = """
            query User($id: String!) {
                user(id: $id) {
                    _id
                    name
                    username
                    email
                    registrationDate
                    state
                }
            }
        """;

        // Crear estructura JSON como Map
        Map<String, Object> body = new HashMap<>();
        body.put("query", graphqlQuery);

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", userId);

        body.put("variables", variables);

        return userServiceWebClient.post()
            .header("x-apollo-operation-name", "GetUser")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(GraphQLUserResponse.class)
            .doOnSubscribe(subscribe -> log.info("Enviando petición GraphQL..."))
            .doOnNext(response -> log.info("Respuesta recibida: {}", response))
            .doOnError(e -> log.error("Error en la petición GraphQL: {}", e.getMessage()))
            .onErrorResume(e -> Mono.error(new RuntimeException("Error al validar usuario: " + e.getMessage())));

        }

   
    private Mono<RegistationSucceedResponse> registerAttendance(RegistationResponse request) {


        return eventServiceWebClient.post()
            .uri("/api/events/registration")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class) // primero como JSON crudo
            .doOnNext(json -> log.info("JSON recibido: {}", json)) // log del JSON crudo
            .map(json -> {
                try {
                    // convertir tú mismo con Jackson u otro
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(json, RegistationSucceedResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Error al convertir JSON a objeto", e);
                }
            });
        }

    private Mono<Void> buildSuccessResponse(ServerWebExchange exchange, RegistationSucceedResponse response) {
        try {

            log.info(response.toString());
            String json = objectMapper.writeValueAsString(response);
            log.info("JSON de respuesta: {}", response);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return buildErrorResponse(exchange, e);
        }
    }

    private Mono<Void> buildErrorResponse(ServerWebExchange exchange, Throwable error) {
        exchange.getResponse().setStatusCode(
            error.getMessage().contains("no encontrado") 
                ? HttpStatus.NOT_FOUND 
                : HttpStatus.BAD_REQUEST
        );
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(error.getMessage().getBytes()))
        );
    }
}