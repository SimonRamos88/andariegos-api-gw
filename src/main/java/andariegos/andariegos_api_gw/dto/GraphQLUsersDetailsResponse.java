package andariegos.andariegos_api_gw.dto;

import java.util.List;


@Data
public class GraphQLUsersDetailsResponse {
    
    private Data data;
    
    @lombok.Data
    public static class Data {
        private FindUsersById findUsersByIds;
    }
    
    @lombok.Data
    public static class FindUsersById{
        List<User> user;

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
