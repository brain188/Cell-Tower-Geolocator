package cm.antic.cell_geolocator;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableCaching
public class CellGeolocatorApplication {

	public static void main(String[] args) {
		Dotenv.configure().directory("./").load().entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
		SpringApplication.run(CellGeolocatorApplication.class, args);
	}

	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

