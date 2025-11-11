package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for user login")
public class LoginRequest {

    @Schema(description = "Username of the user", example = "johndoe")
    private String username;

    @Schema(description = "Password of the user", example = "securepassword123")
    private String password;

}
