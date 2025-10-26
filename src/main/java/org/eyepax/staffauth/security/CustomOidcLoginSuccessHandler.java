package org.eyepax.staffauth.security;


import org.eyepax.staffauth.entity.AppUser;
import org.eyepax.staffauth.entity.LoginAudit;
import org.eyepax.staffauth.repository.AppUserRepository;
import org.eyepax.staffauth.repository.LoginAuditRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CustomOidcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserRepository appUserRepository;
    private final LoginAuditRepository loginAuditRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        // Extract details from Cognito token
        String cognitoId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String displayName = oidcUser.getFullName();
        String locale = (String) oidcUser.getClaims().getOrDefault("locale", "en");

        // Save or update user
        AppUser user = appUserRepository.findByCognitoId(cognitoId)
                .orElseGet(() -> AppUser.builder()
                        .cognitoId(cognitoId)
                        .displayName(displayName)
                        .email(email)
                        .locale(locale)
                        .build());

        user.setDisplayName(displayName);
        user.setEmail(email);
        appUserRepository.save(user);

        // Save login audit
        String ipAddress = request.getRemoteAddr();
        LoginAudit audit = LoginAudit.builder()
                .eventType("LOGIN")
                .ipAddress(ipAddress)
                .loginTime(LocalDateTime.now())
                .user(user)
                .build();

        loginAuditRepository.save(audit);

        // Redirect after login
        response.sendRedirect("/dashboard");
    }
}

