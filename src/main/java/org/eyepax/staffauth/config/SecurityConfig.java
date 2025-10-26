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
                        .requestMatchers("/auth/**").permitAll()   // optional login endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
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
            var groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups == null) return List.<GrantedAuthority>of();
            return groups.stream()
                    .map(g -> new SimpleGrantedAuthority("ROLE_" + g.toUpperCase()))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    // CORS configuration for frontend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // frontend URL
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
