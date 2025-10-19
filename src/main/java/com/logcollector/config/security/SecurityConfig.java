package com.logcollector.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server configuration with Keycloak.
 *
 * Configures JWT-based authentication and RBAC authorization.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Admin endpoints
                        .requestMatchers(HttpMethod.POST, "/v1/servers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/servers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/servers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/alerts/rules").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/alerts/rules/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/alerts/rules/**").hasRole("ADMIN")
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                        // Operator endpoints
                        .requestMatchers(HttpMethod.POST, "/v1/alerts/*/acknowledge").hasAnyRole("ADMIN", "OPERATOR")

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}
