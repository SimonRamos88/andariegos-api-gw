package andariegos.andariegos_api_gw.filters;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import andariegos.andariegos_api_gw.dto.AttendanceResponse;
import andariegos.andariegos_api_gw.dto.GraphQLUsersDetailsResponse;
import andariegos.andariegos_api_gw.dto.UserDetailsResponse;
import andariegos.andariegos_api_gw.dto.UsersDetailsResponse;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AttendanceAggregationFilter implements GlobalFilter {

    private final WebClient eventServiceWebClient;
    private final WebClient userServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private static final Logger log = LoggerFactory.getLogger(EventRegistrationFilter.class);


    public AttendanceAggregationFilter(
        @Qualifier("eventServiceWebClient") WebClient eventServiceWebClient,
        @Qualifier("userServiceWebClient") WebClient userServiceWebClient
    ) {
        this.eventServiceWebClient = eventServiceWebClient;
        this.userServiceWebClient = userServiceWebClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isValidPath(exchange.getRequest().getPath().toString())) {
            return chain.filter(exchange);
        }

        String eventId = extractEventId(exchange.getRequest().getPath().toString());
        
        return fetchAttendanceRecords(eventId)
            .flatMap(this::extractUserIds)
            .doOnNext(response -> log.info("userids 1: {}", response))
            .flatMap(this::fetchUserDetails)
            .doOnNext(response -> log.info("userdet 2: {}", response))
            .flatMap(userDetails -> buildResponse(exchange, userDetails))
            .onErrorResume(e -> handleError(exchange, e));
    }



    // --- Métodos actualizados ---

    private boolean isValidPath(String path) {
        return path.matches("/api/events/registration/attendees/\\d+");
    }

    private String extractEventId(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1]; // Obtiene el último segmento (el ID)
    }

    private Mono<List<AttendanceResponse>> fetchAttendanceRecords(String eventId) {
        return eventServiceWebClient.get()
            .uri("/api/events/registration/attendees/{id}", eventId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<AttendanceResponse>>() {});
    }

    private Mono<List<String>> extractUserIds(List<AttendanceResponse> records) {
        List<String> userIds = records.stream()
            .map(AttendanceResponse::getUserId)
            .collect(Collectors.toList());
        return Mono.just(userIds);
    }

    private Mono<GraphQLUsersDetailsResponse> fetchUserDetails(List<String> userIds) {

    log.info(userIds.getClass().getName());

    String userIdsString = userIds.stream()
    .map(id -> "\"" + id + "\"")  // poner comillas dobles alrededor de cada ID
    .collect(Collectors.joining(", ", "[ ", " ]"));

    log.info(userIdsString);

    String graphqlQuery = """
     query{
            findUsersByIds(userIds: %s){
                user{
                    name
                    email
                    name
                    username
                    email
                    password
                    roles
                    state
                }
                
            }

        }


    """.formatted(userIdsString, null);

    log.info(graphqlQuery);

    // return userServiceWebClient.post()
    // .header("x-apollo-operation-name", "GetUser")
    // .bodyValue(Map.of(
    //     "query", graphqlQuery
    // ))
    // .retrieve()
    // .bodyToMono(GraphQLUsersDetailsResponse.class)
    // .doOnNext(response -> log.info("userdet1: {}", response.toString()))
    // .onErrorResume(e -> Mono.error(new RuntimeException("Error al validar usuario: " + e.getMessage())));


                
    return userServiceWebClient.post()
        .header("x-apollo-operation-name", "GetUser")
        .bodyValue(Map.of(
            "query", graphqlQuery
        ))
        .retrieve()
        .bodyToMono(String.class) // primero como JSON crudo
        .doOnNext(json -> log.info("JSON recibido: {}", json)) // log del JSON crudo
        .map(json -> {
            try {
                // convertir tú mismo con Jackson u otro
                ObjectMapper mapper = new ObjectMapper();
                log.info("antes de mapper");
                return mapper.readValue(json, GraphQLUsersDetailsResponse.class);
            } catch (Exception e) {
                 log.error("Error al convertir JSON a objeto: {}", e.getMessage(), e);
                throw new RuntimeException("Error al convertir JSON a objeto", e);
            }
        }).onErrorResume(e -> Mono.error(new RuntimeException("Error al validar usuario: " + e.getMessage())));
    }


    private Mono<Void> buildResponse(ServerWebExchange exchange, GraphQLUsersDetailsResponse userDetails) {
    try {
        log.info("llego a build");
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = objectMapper.writeValueAsBytes(userDetails);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
        return Mono.error(e);
    }
}

    private Mono<Void> handleError(ServerWebExchange exchange, Throwable error) {
        exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return exchange.getResponse().setComplete();
    }




}


// @Component
// public class AttendanceAggregationFilter implements GlobalFilter {

//     // ... (inyección de dependencias y configuraciones previas se mantienen igual)

//     @Override
//     public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//         if (!isValidPath(exchange.getRequest().getPath().toString())) {
//             return chain.filter(exchange);
//         }

//         String eventId = extractEventId(exchange.getRequest().getPath().toString());
        
//         return fetchAttendanceRecords(eventId)
//             .flatMap(records -> {
//                 if (records.isEmpty()) {
//                     return Mono.error(new RuntimeException("No attendees found"));
//                 }
                
//                 // Tomamos el primer ID para debug (puedes cambiarlo por cualquier lógica)
//                 Long firstUserId = records.get(0).getUserId();
//                 return fetchSingleUserDetails(firstUserId);
//             })
//             .flatMap(userDetails -> buildResponse(exchange, userDetails))
//             .onErrorResume(e -> handleError(exchange, e));
//     }

//     // Método para obtener detalles de un solo usuario
//     private Mono<UserDetails> fetchSingleUserDetails(Long userId) {
//         return userServiceWebClient.get()
//             .uri("/api/users/{id}", userId)
//             .retrieve()
//             .bodyToMono(UserDetails.class); // Asume que UserDetails es tu DTO para un usuario individual
//     }

//     // ... (otros métodos auxiliares se mantienen igual)

//     // Nuevo DTO para la respuesta de usuario individual
//     @Data
//     public static class UserDetails {
//         private Long id;
//         private String name;
//         private String email;
//         // otros campos que devuelva tu API
//     }
// }