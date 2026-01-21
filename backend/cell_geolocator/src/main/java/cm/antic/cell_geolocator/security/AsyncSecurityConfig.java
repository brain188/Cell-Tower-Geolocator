package cm.antic.cell_geolocator.security;

import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncSecurityConfig {

    @Bean
    public Executor taskExecutor() {
        Executor delegate = Executors.newFixedThreadPool(20);
        return new DelegatingSecurityContextExecutor(delegate);
    }

}
