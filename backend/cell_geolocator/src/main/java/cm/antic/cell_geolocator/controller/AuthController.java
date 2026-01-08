package cm.antic.cell_geolocator.controller;

import cm.antic.cell_geolocator.model.AuthResponse;
import cm.antic.cell_geolocator.model.CompanyVerificationRequest;
import cm.antic.cell_geolocator.model.LoginRequest;
import cm.antic.cell_geolocator.model.SignupRequest;
import cm.antic.cell_geolocator.service.AuthService;
import cm.antic.cell_geolocator.service.CompanyService;
import cm.antic.cell_geolocator.model.RefreshRequest;
import cm.antic.cell_geolocator.entity.User;
import cm.antic.cell_geolocator.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "User login", description = "Authenticate user and return access + refresh JWT tokens")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody @Parameter(description = "Login credentials") LoginRequest request) {

        AuthResponse response = authService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "User registration", description = "Create a new user account")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request or username/email already exists"),
        @ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(
            @Valid @RequestBody @Parameter(description = "User registration details") SignupRequest request) {

        authService.signup(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Refresh access token", description = "Generate new access token using valid refresh token")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "New access token issued",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify company information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verification successful"),
        @ApiResponse(responseCode = "400", description = "Invalid company information"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/verify-company")
    public ResponseEntity<String> verifyCompany(
            @Valid @RequestBody CompanyVerificationRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isVerified()) {
            return ResponseEntity.ok("Already verified");
        }

        if (companyService.verify(request.getCompanyName(), request.getCompanyDepartment())) {
            user.setVerified(true);
            userRepository.save(user);
            return ResponseEntity.ok("Verification successful");
        } else {
            return ResponseEntity.badRequest().body("Invalid company information");
        }
    }
}