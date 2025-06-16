package andariegos.andariegos_api_gw.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import andariegos.andariegos_api_gw.dto.GraphQLUserResponse;
import andariegos.andariegos_api_gw.dto.RegistationResponse;
import andariegos.andariegos_api_gw.dto.RegistationSucceedResponse;
import andariegos.andariegos_api_gw.dto.UserDetailsResponse;
import andariegos.andariegos_api_gw.dto.Report;

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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ReportFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(ReportFilter.class);

    private final WebClient eventServiceWebClient;
    private final WebClient reportServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportFilter(
        @Qualifier("eventServiceWebClient") WebClient eventServiceWebClient,
        @Qualifier("reportServiceWebClient") WebClient reportServiceWebClient
    ) {
        this.eventServiceWebClient = eventServiceWebClient;
        this.reportServiceWebClient = reportServiceWebClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!isReportRequest(exchange.getRequest())) {
            return chain.filter(exchange);
        }


        return processReportRequest(exchange);
    }

    
 private Mono<Void> processReportRequest(ServerWebExchange exchange) {
    return retrieveAllReports()
        .doOnNext(reportList -> log.info("Reportes obtenidos: {}", reportList)) // 👈 log de toda la lista
        .flatMapMany(Flux::fromIterable) // Mono<List<Report>> → Flux<Report>
        .doOnNext(report -> log.info("Procesando reporte con ID: {}", report.getIdEvent())) // 👈 log antes de getEventName
        .flatMap(report -> 
            getEventName(report.getIdEvent()) // Mono<String>
                .doOnNext(eventName -> log.info("Nombre del evento para ID {}: {}", report.getIdEvent(), eventName)) 
                .map(eventName -> {
                    report.setEventName(eventName);
                    return report;
                })
        )
        .flatMap(updatedReport -> buildSuccessResponse(exchange, updatedReport)) // Mono<Void>
        .then()
        .onErrorResume(error -> buildErrorResponse(exchange, error));
}


    private boolean isReportRequest(ServerHttpRequest request) {
        return request.getPath().toString().equals("/api/reports") 
            && request.getMethod() == HttpMethod.POST;
    }

    public Mono<List<Report>> retrieveAllReports() {
        log.info("estamos en retiurve reports");
            return reportServiceWebClient.get()
                .uri("/api/reports/")
                .retrieve()
                .bodyToFlux(Report.class)
                .collectList()
                .switchIfEmpty(Mono.error(new RuntimeException("No reports found")));
        }  

    public Mono<String> getEventName(Long eventId) {
        return eventServiceWebClient.get()
            .uri("/api/events/name/{eventId}", eventId)
            .retrieve()
            .bodyToMono(String.class);
    }

   
 

    private Mono<Void> buildSuccessResponse(ServerWebExchange exchange, Report response) {
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