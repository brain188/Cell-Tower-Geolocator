package cm.antic.cell_geolocator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CellGeolocatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CellGeolocatorApplication.class, args);
	}

}

