package andariegos.andariegos_api_gw.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import andariegos.andariegos_api_gw.dto.Report;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
        String path = exchange.getRequest().getPath().toString();
        HttpMethod method = exchange.getRequest().getMethod();

        if ("/api/report".equals(path) && method == HttpMethod.GET) {
            log.info("Obteniendo todos los reportes");
            return handleAllReports(exchange);
        }

        if (path.matches("/api/report/[a-fA-F0-9]+") && method == HttpMethod.GET) {
            String id = path.split("/")[3];
            log.info("Obteniendo reporte con ID {}", id);
            return handleReportById(exchange, id);
        }

        if (path.matches("/api/report/state/(accepted|denied|pending)") && method == HttpMethod.GET) {
            String state = path.split("/")[4];
            log.info("Obteniendo reportes con estado {}", state);
            return handleReportsByState(exchange, state);
        }


        return chain.filter(exchange);
    }

    private Mono<Void> handleAllReports(ServerWebExchange exchange) {
        return retrieveAllReports()
            .flatMapMany(Flux::fromIterable)
            .flatMap(this::setEventName)
            .collectList()
            .flatMap(reportList -> buildSuccessResponse(exchange, reportList))
            .onErrorResume(error -> buildErrorResponse(exchange, error));
    }

    private Mono<Void> handleReportById(ServerWebExchange exchange, String id) {
        return reportServiceWebClient.get()
            .uri("/api/reports/{id}", id)
            .retrieve()
            .bodyToMono(Report.class)
            .flatMap(this::setEventName)
            .flatMap(report -> buildSuccessResponse(exchange, report))
            .onErrorResume(error -> buildErrorResponse(exchange, error));
    }

    private Mono<Void> handleReportsByState(ServerWebExchange exchange, String state) {
        return reportServiceWebClient.get()
            .uri("api/reports/state/{state}", state)
            .retrieve()
            .bodyToFlux(Report.class)
            .flatMap(this::setEventName)
            .collectList()
            .flatMap(reportList -> buildSuccessResponse(exchange, reportList))
            .onErrorResume(error -> buildErrorResponse(exchange, error));
    }

    private Mono<Report> setEventName(Report report) {
        return getEventName(report.getIdEvent())
            .doOnNext(name -> log.info("Nombre del evento para ID {}: {}", report.getIdEvent(), name))
            .map(eventName -> {
                report.setEventName(eventName);
                return report;
            });
    }

    public Mono<List<Report>> retrieveAllReports() {
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

    // Para una sola respuesta (por ID)
    private Mono<Void> buildSuccessResponse(ServerWebExchange exchange, Report response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return buildErrorResponse(exchange, e);
        }
    }

    // Para lista de respuestas (todos o por estado)
    private Mono<Void> buildSuccessResponse(ServerWebExchange exchange, List<Report> responseList) {
        try {
            String json = objectMapper.writeValueAsString(responseList);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
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
        byte[] bytes = error.getMessage().getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
