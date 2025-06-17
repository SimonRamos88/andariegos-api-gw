package andariegos.andariegos_api_gw.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RegistationResponse {
        private int eventId;
        private String userId;
        private String booking_time;
        private String booking_date;
}
