package andariegos.andariegos_api_gw.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String access_token;
    private String userId;
}