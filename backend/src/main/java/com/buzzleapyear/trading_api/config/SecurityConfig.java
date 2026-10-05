package com.buzzleapyear.trading_api.config;

import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.buzzleapyear.trading_api.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                (request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value())
            ))
            .authorizeHttpRequests(authorize -> authorize
                // PUBLIC 
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/validate").authenticated()

                .requestMatchers("/error").permitAll()

                // AUTHENTICATED ENDPOINTS
                .requestMatchers("/api/v1/accounts/**").authenticated()
                .requestMatchers("/api/v1/holdings/**").authenticated()
                .requestMatchers("/api/v1/instruments/**", "/api/v1/quote/**").authenticated()
                
                // RESTRICTED (RBAC)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/test").hasRole("ADMIN")
                .requestMatchers("/api/v1/analyst/**").hasAnyRole("ADMIN", "ANALYST")
                .requestMatchers("/api/v1/compliance/**").hasAnyRole("ADMIN", "COMPLIANCE")
                .requestMatchers("/api/v1/client/**").hasAnyRole("ADMIN","CLIENT","ANALYST")
                
                // HEALTHCHECK (internal only - docker-compose, kubernetes, etc.)
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                
                // ALL paths
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
    