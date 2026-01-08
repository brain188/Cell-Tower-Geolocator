package cm.antic.cell_geolocator.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "company_info")
@Data
public class CompanyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

}
