package andariegos.andariegos_api_gw.dto;

import java.util.List;
import lombok.Data;


@Data
public class GraphQLUsersDetailsResponse {
    private DataContainer data;

    @Data
    public static class DataContainer {
        private List<FindUserById> findUsersByIds;
    }

    @Data
    public static class FindUserById {
        private User user;
    }

    @Data
    public static class User {
        private String name;
        private String email;
        private String username;
        private String password;
        private List<String> roles;
        private String state;
    }
}
