package andariegos.andariegos_api_gw.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;


@Data
public class GraphQLUsersDetailsResponse {
   private DataContainer data;

    @Data
    public static class DataContainer {
        private List<UserWrapper> findUsersByIds;
    }

    @Data
    public static class UserWrapper {
        private User user;
    }

    @Data
    public static class User {
        @JsonProperty("_id")
        private String id;
        private String name;
        private String username;
        private String state;
        // Eliminamos email ya que no viene en la respuesta
    }

    // Método para obtener la lista plana de usuarios
    public List<User> getUsers() {
        return this.data.getFindUsersByIds().stream()
                .map(UserWrapper::getUser)
                .collect(Collectors.toList());
    }
}
