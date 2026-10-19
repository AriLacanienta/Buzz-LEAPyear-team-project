package com.buzzleapyear.trading_api.config;

<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
import java.util.Arrays;
import org.springframework.http.HttpStatus;
=======
>>>>>>> e36882a (clean up imports)
import org.springframework.beans.factory.annotation.Autowired;
=======
>>>>>>> a812211 (stubbed /user endpoint, minimal admin page)
=======
import org.springframework.http.HttpStatus;
>>>>>>> e7922e0 (making role specific log ins work)
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
<<<<<<< HEAD
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/validate").authenticated()

                .requestMatchers("/error").permitAll()

                // AUTHENTICATED ENDPOINTS
                .requestMatchers("/api/v1/accounts/**").authenticated()
                .requestMatchers("/api/v1/holdings/**").authenticated()
                .requestMatchers("/api/v1/instruments/**", "/api/v1/quote/**").authenticated()
                

                // RESTRICTED (RBAC)
<<<<<<< HEAD
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/test").hasRole("ADMIN")
                // The analyst dashboard is currently available to authenticated users
                // until the application has a role-management flow for assigning analysts.
                .requestMatchers("/api/v1/analyst/**").authenticated()
                .requestMatchers("/api/v1/compliance/**").hasAnyRole("ADMIN", "COMPLIANCE")
                .requestMatchers("/api/v1/client/**").hasAnyRole("ADMIN","CLIENT","ANALYST")
=======
                .requestMatchers("/api/v1/admin/**").hasRole("MANAGER")
                .requestMatchers("/api/v1/test").hasRole("MANAGER")
                .requestMatchers("/api/v1/analyst/**").hasAnyRole("MANAGER", "ANALYST")
                .requestMatchers("/api/v1/compliance/**").hasRole("MANAGER")
                .requestMatchers("/api/v1/instruments/**", "/api/v1/quote/**").authenticated()
                .requestMatchers("/api/v1/client/**").hasAnyRole("MANAGER", "CLIENT", "ANALYST")
>>>>>>> 15b55d4 (update security configuration)
=======
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/validate").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()

                // RESTRICTED (RBAC)
                .requestMatchers("/api/v1/accounts/**").authenticated()
                .requestMatchers("/api/v1/holdings/**").authenticated()
                .requestMatchers("/api/v1/instruments/**").authenticated()
                .requestMatchers("/api/v1/quote/**").authenticated()

                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/analyst/**").hasAnyRole(ROLE_MANAGER, ROLE_ANALYST)
                .requestMatchers("/api/v1/client/**").hasAnyRole(ROLE_MANAGER, ROLE_CLIENT, ROLE_ANALYST)
                .requestMatchers("/api/v1/compliance/**").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/instruments/**", "/api/v1/quote/**").authenticated()
                .requestMatchers("/api/v1/test").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/user/allUsers").hasRole(ROLE_MANAGER)
                .requestMatchers("/api/v1/user/**").authenticated()
>>>>>>> a812211 (stubbed /user endpoint, minimal admin page)

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
