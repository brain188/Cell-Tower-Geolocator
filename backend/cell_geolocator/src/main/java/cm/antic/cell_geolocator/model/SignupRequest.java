package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request payload for new user signup")
public class SignupRequest {

    @Schema(description = "Unique username for the user", example = "johndoe")
    private String username;

    @Schema(description = "Email address of the user", example = "johndoe@example.com")
    private String email;

    @Schema(description = "Password for the user account", example = "securepassword123")
    private String password;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

}
