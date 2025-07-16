package andariegos.andariegos_api_gw.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerMonitor {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerMonitor.class);
    private final CircuitBreakerRegistry registry;
    private CircuitBreaker cb;

    public CircuitBreakerMonitor(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        cb = registry.circuitBreaker("EventsBreaker");
    }

    @Scheduled(fixedRate = 5000)
    public void logStatus() {
        if (cb != null) {
            log.info("Estado del Circuit Breaker '{}': {}", cb.getName(), cb.getState());
            log.info("Stats -> fallos: {}, exitosas: {}, total llamadas: {}",
                    cb.getMetrics().getNumberOfFailedCalls(),
                    cb.getMetrics().getNumberOfSuccessfulCalls(),
                    cb.getMetrics().getNumberOfBufferedCalls());
        }
    }
}

