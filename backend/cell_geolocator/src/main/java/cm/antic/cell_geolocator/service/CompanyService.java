package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.entity.CompanyInfo;
import cm.antic.cell_geolocator.repository.CompanyInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CompanyService {

    @Autowired
    private CompanyInfoRepository companyInfoRepository;

    public boolean verify(String name, String department) {
        // Assume single row with ID 1 - adjust if multiple
        CompanyInfo expected = companyInfoRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("No company info configured"));

        return name.equals(expected.getName()) && department.equals(expected.getDepartment());
    }
}
