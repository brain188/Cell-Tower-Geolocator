package cm.antic.cell_geolocator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class RequestLog {

    @Id
    @GeneratedValue
    private Long id;
    private String mcc;
    private String mnc;
    private String lac;
    private String cellId;
    private Double accuracy;
    private String providerUsed;
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime timestamp;

}
