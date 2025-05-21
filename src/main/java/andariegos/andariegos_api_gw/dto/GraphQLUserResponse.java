package andariegos.andariegos_api_gw.dto;

import java.util.List;
import lombok.Data;

@Data
public class GraphQLUserResponse {
    private Data data;
    
    @lombok.Data
    public static class Data {
        private User user;
    }
    
    @lombok.Data
    public static class User {
        private String id;
        private String name;
        private String username;
        private String email;
        private List<String> roles;
        private String state;
    }
}