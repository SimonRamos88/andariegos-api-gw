package andariegos.andariegos_api_gw.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class RegistationSucceedResponse {
    private String id;
    private Event event;
    private Long eventId;
    private String userId;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Event{
         private String idEvent;
        private String name;
        private String description;
        
        // @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        // private LocalDateTime date;
        
        private String city;
        private String address;
        private Integer availableSpots;
        private BigDecimal price;
        private String image1;
        private String image2;
        private String image3;

    }
}
