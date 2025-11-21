package org.eyepax.staffauth.config;

import java.util.List;
import java.util.stream.Collectors;

import org.eyepax.staffauth.security.CustomOidcLoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOidcLoginSuccessHandler customOidcLoginSuccessHandler;

    // Read from application.properties
    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.region}")
    private String region;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // CORS - must come first; use existing CorsConfigurationSource bean from CorsConfig
            .cors(Customizer.withDefaults())
                
                // CSRF - disable for stateless REST API
                .csrf(csrf -> csrf.disable())
                
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers("/healthz").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()  // Android sign-in/sign-up
                        .requestMatchers("/api/v1/token-debug").permitAll()
                        
                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN")
                        
                        // Protected API endpoints (require authentication)
                        .requestMatchers("/api/v1/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()  // Android user profile
                        .requestMatchers("/api/home/**").authenticated()  // Android home/dashboard
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // OIDC login for web browser sessions (optional for web app)
                .oauth2Login(oauth -> oauth
                        .successHandler(customOidcLoginSuccessHandler)
                )
                
                // JWT resource server for mobile/API token authentication
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                
                // Logout configuration
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                
                // Stateless session for REST API
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * JWT decoder to validate Cognito tokens
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwksUri = String.format(
            "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
            region,
            userPoolId
        );
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }

    /**
     * Convert Cognito groups to Spring Security roles
     * Maps cognito:groups claim to ROLE_* authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            System.out.println("=== JWT Authentication Converter ===");
            System.out.println("JWT Subject (username): " + jwt.getSubject());
            System.out.println("JWT Claims: " + jwt.getClaims());
            
            // Extract cognito:groups claim
            var groups = jwt.getClaimAsStringList("cognito:groups");
            System.out.println("Cognito groups: " + groups);
            
            if (groups == null || groups.isEmpty()) {
                System.out.println("No groups found, returning default USER role");
                // Return default role if no groups assigned
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            // Map groups to Spring Security authorities
            List<GrantedAuthority> authorities = groups.stream()
                    .map(group -> {
                        String role = "ROLE_" + group.toUpperCase();
                        System.out.println("Mapping group '" + group + "' to role '" + role + "'");
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toList());
            
            System.out.println("Final authorities: " + authorities);
            System.out.println("=== JWT Authentication Converter Complete ===");
            
            return authorities;
        });
        
        return converter;
    }

}
