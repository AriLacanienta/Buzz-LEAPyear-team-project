package com.buzzleapyear.trading_api.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

/**
 * Startup configuration to eagerly initialize the DispatcherServlet
 * This prevents lazy initialization on the first request, which can cause
 * issues with the first browser request after application startup
 */
@Configuration
public class StartupConfig {

    @Bean
    public ApplicationRunner warmUpDispatcherServlet(RestTemplate restTemplate) {
        return args -> {
            // Small delay to ensure server is fully ready
            Thread.sleep(1000);
            
            try {
                // Make a dummy OPTIONS request to warm up the DispatcherServlet
                restTemplate.exchange(
                    "http://localhost:6767/api/v1/auth/login",
                    HttpMethod.OPTIONS,
                    null,
                    String.class
                );
            } catch (Exception e) {
                // Silently fail - this is just for warmup
                // The server might not be ready yet, but that's okay
            }
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
