package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for new user signup")
public class SignupRequest {

    @Schema(description = "Unique username for the user", example = "johndoe")
    private String username;

    @Schema(description = "Email address of the user", example = "johndoe@example.com")
    private String email;

    @Schema(description = "Password for the user account", example = "securepassword123")
    private String password;

}
