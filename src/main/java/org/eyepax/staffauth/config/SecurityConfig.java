package org.eyepax.staffauth.config;

import org.eyepax.staffauth.security.CustomOidcLoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOidcLoginSuccessHandler customOidcLoginSuccessHandler;

    // Cognito User Pool ID
    private static final String USER_POOL_ID = "eu-north-1_ExI0Aq7Ov";
    private static final String JWKS_URI =
            "https://cognito-idp.eu-north-1.amazonaws.com/" + USER_POOL_ID + "/.well-known/jwks.json";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // Authorization
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/healthz").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api/v1/token-debug").permitAll() // For debugging
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated()
                )
                // OIDC login for browser sessions
                .oauth2Login(oauth -> oauth
                        .successHandler(customOidcLoginSuccessHandler)
                )
                // JWT resource server for SPA/mobile token auth
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                // Default logout
                .logout(Customizer.withDefaults());

        return http.build();
    }

    // JWT decoder to validate Cognito tokens
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
    }

    // Convert Cognito groups to Spring Security roles
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            System.out.println("=== JWT Authentication Converter ===");
            System.out.println("JWT Claims: " + jwt.getClaims());
            
            var groups = jwt.getClaimAsStringList("cognito:groups");
            System.out.println("Cognito groups: " + groups);
            
            if (groups == null) {
                System.out.println("No groups found, returning empty authorities");
                return List.<GrantedAuthority>of();
            }
            
            List<GrantedAuthority> authorities = groups.stream()
                    .map(g -> {
                        String role = "ROLE_" + g.toUpperCase();
                        System.out.println("Mapping group '" + g + "' to role '" + role + "'");
                        return new SimpleGrantedAuthority(role);
                    })
                    .collect(Collectors.toList());
            
            System.out.println("Final authorities: " + authorities);
            System.out.println("=== JWT Authentication Converter Complete ===");
            
            return authorities;
        });
        return converter;
    }

    // CORS configuration for frontend and mobile
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow both web and mobile origins
        config.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",  // React web app
            "http://10.0.2.2:3000",   // Android emulator accessing host
            "http://localhost:*",      // Flutter mobile
            "*"                        // Allow all for development (REMOVE IN PRODUCTION!)
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "OPTIONS", "PUT"));
        config.setAllowedHeaders(Arrays.asList("*")); // Allow all headers
        config.setAllowCredentials(false); // Set to false when using "*" origin
        config.setExposedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}