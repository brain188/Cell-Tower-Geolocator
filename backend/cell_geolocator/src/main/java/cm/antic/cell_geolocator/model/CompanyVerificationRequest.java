package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CompanyVerificationRequest {

    @Schema(description = "Company name", example = "My Company")
    private String companyName;

    @Schema(description = "Department name", example = "IT Department")
    private String companyDepartment;

}
