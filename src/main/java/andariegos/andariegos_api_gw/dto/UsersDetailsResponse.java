package andariegos.andariegos_api_gw.dto;

import java.util.List;

import lombok.Data;

@Data
public class UsersDetailsResponse {
    private List<User> users;
}