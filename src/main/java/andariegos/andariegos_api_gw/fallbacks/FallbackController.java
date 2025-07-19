package andariegos.andariegos_api_gw.fallbacks;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

   @GetMapping("/events")
    public ResponseEntity<List<?>> fallbackGetAllEvents() {
        // Crear disponibilidad horaria (availabilityPattern)
        Map<String, Object> availabilityPattern = new HashMap<>();
        availabilityPattern.put("dayOfWeek", 3); // Miércoles
        availabilityPattern.put("startTime", "10:00:00");
        availabilityPattern.put("endTime", "12:00:00");

        // Crear eventTime con availabilityPattern
        Map<String, Object> eventTime = new HashMap<>();
        eventTime.put("id", 999L);
        eventTime.put("availabilityPattern", availabilityPattern);

        // Crear evento simulado
        Map<String, Object> event = new HashMap<>();
        event.put("id", 999L);
        event.put("name", "Evento mock desde fallback");
        event.put("description", "Este evento fue devuelto porque el servicio de eventos no está disponible.");
        event.put("city", "Bogotá");
        event.put("address", "Fallback Street 123");
        event.put("price", 0);
        event.put("image1", "https://www.museoindependencia.gov.co/quienes-somos/PublishingImages/Casa%20restaurada%201960%20(2).jpg");
        event.put("image2", null);
        event.put("image3", null);
        event.put("eventTimes", List.of(eventTime));

        // Envolver en formato esperado: { success: true, data: [...] }
       List<Map<String, Object>> fallbackList = List.of(event);

        return ResponseEntity.ok().body(fallbackList);
    }

    // // Puedes agregar otros fallbacks si tienes más circuit breakers
    // @GetMapping("/anotherService")
    // public ResponseEntity<String> fallbackAnotherService() {
    //     return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
    //             .body("⚠️ Otro servicio no disponible.");
    // }
}
