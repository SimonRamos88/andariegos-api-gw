package andariegos.andariegos_api_gw.dto;

import java.util.List;

import lombok.Data;

@Data
public class User {
    private String id;
    private String name;
    private String email;
    private String username;
    private String password;
    private String state;
    private List<String> roles;
}