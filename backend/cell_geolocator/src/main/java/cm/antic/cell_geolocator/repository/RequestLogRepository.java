package cm.antic.cell_geolocator.repository;

import cm.antic.cell_geolocator.entity.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

}
