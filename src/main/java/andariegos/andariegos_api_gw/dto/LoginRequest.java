package andariegos.andariegos_api_gw.dto;

import lombok.Data;

@Data
public class LoginRequest {
        private String email;
        private String password;
}
