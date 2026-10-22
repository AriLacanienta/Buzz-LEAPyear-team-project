package com.buzzleapyear.trading_api.config;

import org.springframework.http.HttpStatus;
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

import com.buzzleapyear.trading_api.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROLE_ANALYST = "ANALYST";
    private static final String ROLE_CLIENT = "CLIENT";
    private static final String ROLE_MANAGER = "MANAGER";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        http.cors(cors -> cors.disable())
            // Using stateless JWTs
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                (request, response, authException) -> response.sendError(HttpStatus.UNAUTHORIZED.value())
            ))
            .authorizeHttpRequests(authorize -> authorize
                // PUBLIC
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/validate").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                // RESTRICTED (RBAC)
                .requestMatchers("/api/v1/accounts/**").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/analyst/**").hasAnyRole(ROLE_MANAGER, ROLE_ANALYST)
                .requestMatchers("/api/v1/client/**").hasAnyRole(ROLE_MANAGER, ROLE_CLIENT, ROLE_ANALYST)
                .requestMatchers("/api/v1/compliance/**").hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.GET, "/api/v1/dispute/search").hasAnyRole(ROLE_MANAGER, ROLE_CLIENT)
                .requestMatchers(HttpMethod.GET, "/api/v1/dispute/summary").hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/dispute/*/resolve").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/dispute/**").authenticated()
                .requestMatchers("/api/v1/holdings/**").authenticated()
                .requestMatchers("/api/v1/instruments/**").authenticated()
                .requestMatchers("/api/v1/manager/**").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/quote/**").authenticated()
                .requestMatchers("/api/v1/test").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/tradeorders/**").hasAnyRole(ROLE_MANAGER, ROLE_CLIENT, ROLE_ANALYST)
                .requestMatchers("/api/v1/user/allUsers").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/user/**").authenticated()

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
