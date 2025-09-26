package cm.antic.cell_geolocator.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cellGeolocationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cell Tower Geolocation API")
                        .description("API for resolving cell tower location using OpenCellID, UnwiredLabs, and Combain providers with priority aggregation.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Tendong Brain")
                                .email("tendongbrain@gmail.com")
                                .url("https://github.com/brain188/Cell-Tower-Geolocator"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/brain188/Cell-Tower-Geolocator"));
    }

}




// Swagger UI: http://localhost:8081/swagger-ui/index.html

// OpenAPI JSON: http://localhost:8081/v3/api-docs