package cm.antic.cell_geolocator.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "RULE_USER";

    private String refreshToken;

    @Column(nullable = false)
    private Boolean verified = false;

    public Boolean isVerified() {
        return verified != null ? verified : false;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

}