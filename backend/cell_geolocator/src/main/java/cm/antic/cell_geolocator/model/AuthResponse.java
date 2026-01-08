package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response payload after successful authentication (login or signup)")
public class AuthResponse {

    public AuthResponse(String newAccessToken, String newRefreshToken) {
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
    }

    @Schema(description = "JWT access token (short-lived, used for API calls)")
    private String accessToken;

    @Schema(description = "JWT refresh token (long-lived, used to get new access token)")
    private String refreshToken;

    @Schema(description = "Token type (always Bearer)", example = "Bearer", defaultValue = "Bearer")
    private String tokenType = "Bearer";

}
