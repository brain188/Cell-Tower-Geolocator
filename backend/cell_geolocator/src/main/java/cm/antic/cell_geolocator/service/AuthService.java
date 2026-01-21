package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.AuthResponse;
import cm.antic.cell_geolocator.entity.User;
import cm.antic.cell_geolocator.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access.token.expiry-ms:86400000}") // 1 day default
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh.token.expiry-ms:604800000}") // 7 days default
    private long refreshTokenExpiryMs;

    private SecretKey key;


    @Autowired
    public void initKey() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // LOGIN 
    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String accessToken = generateAccessToken(username);
        String refreshToken = generateRefreshToken(username);

        // Store refresh token in DB
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    // SIGNUP 
    public AuthResponse signup(String username, String email, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setVerified(false);

        userRepository.save(user);

        String accessToken = generateAccessToken(username);
        String refreshToken = generateRefreshToken(username);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken);
    }

    // REFRESH TOKEN 
    public AuthResponse refreshToken(String refreshToken) {
        // Extract username from token
        String username = extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        // Validate stored refresh token (hashed)
        if (!passwordEncoder.matches(refreshToken, user.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Check if token is expired
        if (isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh token expired");
        }

        // Generate new access token
        String newAccessToken = generateAccessToken(username);

        // rotate refresh token 
        String newRefreshToken = generateRefreshToken(username);
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    // TOKEN GENERATION 
    private String generateAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    private String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiryMs))
                .signWith(key)
                .compact();
    }

    // TOKEN VALIDATION HELPERS 
    private String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}