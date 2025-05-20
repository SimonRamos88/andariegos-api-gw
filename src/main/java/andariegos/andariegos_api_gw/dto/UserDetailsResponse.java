package andariegos.andariegos_api_gw.dto;

import lombok.Data;

@Data
public class UserDetailsResponse {
    private Data data;

    @lombok.Data
    public static class Data {
        private User user;
    }

}
