package andariegos.andariegos_api_gw.dto;

import java.util.List;

import lombok.Data;

@Data
public class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private List<String> roles;

        private String nationality;
        private String name;
}
