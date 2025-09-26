package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for user login")
public class LoginRequest {

    @Schema(description = "Username of the user", example = "johndoe")
    private String username;

    @Schema(description = "Password of the user", example = "securepassword123")
    private String password;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
