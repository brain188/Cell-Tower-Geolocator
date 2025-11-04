package cm.antic.cell_geolocator.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.retry.RetryConfig;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {

        // Reactor Netty Client settings
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5)) // Max wait time for response
                .compress(true)
                .followRedirect(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "cell-geolocator/1.0")
                .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                    System.out.println("Request: " + clientRequest.method() + " " + clientRequest.url());
                    return Mono.just(clientRequest);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                    System.out.println("Response status: " + clientResponse.statusCode());
                    return Mono.just(clientResponse);
                }))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(4 * 1024 * 1024)) // 4MB buffer
                        .build())
                .build();
    }

    // Resilience4j Circuit Breaker
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
    }

    // Time limiter (Prevents async futures hanging)
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();
    }

    // Retry Strategy for provider failures
    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(300))
                .retryExceptions(Exception.class)
                .build();
    }
}
