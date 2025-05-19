package andariegos.andariegos_api_gw.filters;

import java.util.List;
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
import andariegos.andariegos_api_gw.dto.UserDetailsResponse;
import andariegos.andariegos_api_gw.dto.UsersDetailsResponse;
import reactor.core.publisher.Mono;

@Component
public class AttendanceAggregationFilter implements GlobalFilter {

    private final WebClient eventServiceWebClient;
    private final WebClient userServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AttendanceAggregationFilter(
        @Qualifier("eventServiceWebClient") WebClient eventServiceWebClient,
        @Qualifier("userServiceWebClient") WebClient userServiceWebClient
    ) {
        this.eventServiceWebClient = eventServiceWebClient;
        this.userServiceWebClient = userServiceWebClient;
    }

    // @Override
    // public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    //     if (!isValidPath(exchange.getRequest().getPath().toString())) {
    //         return chain.filter(exchange);
    //     }

    //     String eventId = extractEventId(exchange.getRequest().getPath().toString());
        
    //     return fetchAttendanceRecords(eventId)
    //         .flatMap(this::extractUserIds) // Extrae solo los IDs de usuario
    //         .flatMap(this::fetchUserDetails)
    //         .flatMap(userDetails -> buildResponse(exchange, userDetails))
    //         .onErrorResume(e -> handleError(exchange, e));
    // }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isValidPath(exchange.getRequest().getPath().toString())) {
            return chain.filter(exchange);
        }

        String eventId = extractEventId(exchange.getRequest().getPath().toString());
        
        return fetchAttendanceRecords(eventId)
            .flatMap(records -> {
                if (records.isEmpty()) {
                    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                    return exchange.getResponse().setComplete();
                }
                
                // System.out.println(records);
                String firstUserId = records.get(0).getUserId(); // Asume que userId es String
                // System.out.println("priemr id" + firstUserId);
                return fetchSingleUserDetails(firstUserId)
                    .flatMap(userDetails -> buildResponse(exchange, userDetails));
            });
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

    private Mono<UsersDetailsResponse> fetchUserDetails(List<String> userIds) {
        return userServiceWebClient.post()
            .uri("/api/users/batch")
            .bodyValue(userIds)
            .retrieve()
            .bodyToMono(UsersDetailsResponse.class);
    }

    // private Mono<Void> buildResponse(ServerWebExchange exchange, UsersDetailsResponse userDetails) {
    //     try {
    //         exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    //         byte[] bytes = objectMapper.writeValueAsBytes(userDetails);
    //         DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
    //         return exchange.getResponse().writeWith(Mono.just(buffer));
    //     } catch (JsonProcessingException e) {
    //         return Mono.error(e);
    //     }
    // }

       private Mono<Void> buildResponse(ServerWebExchange exchange, UserDetailsResponse userDetails) {
        try {
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



         // Método para obtener detalles de un solo usuario
    private Mono<UserDetailsResponse> fetchSingleUserDetails(String userId) {
        return userServiceWebClient.get()
            .uri("/api/users/{id}", userId)
            .retrieve()
            .bodyToMono(UserDetailsResponse.class); // Asume que UserDetails es tu DTO para un usuario individual
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