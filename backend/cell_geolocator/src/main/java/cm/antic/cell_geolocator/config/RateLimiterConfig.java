package cm.antic.cell_geolocator.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public Bucket rateLimiterBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)                                 
                .refillGreedy(10, Duration.ofMinutes(1))      
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

}
