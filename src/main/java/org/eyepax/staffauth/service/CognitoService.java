package org.eyepax.staffauth.service;


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;

@Service
public class CognitoService {

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Value("${aws.cognito.clientSecret}")
    private String clientSecret;

    @Value("${aws.cognito.region}")
    private String region;

    private CognitoIdentityProviderClient cognitoClient;

    @PostConstruct
    public void init() {
        this.cognitoClient = CognitoIdentityProviderClient.builder()
            .region(Region.of(region))
            .build();
    }

    // Calculate Secret Hash
    private String calculateSecretHash(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(clientSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal((username + clientId).getBytes());
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }

    // Sign Up
    public String signUp(String username, String password, String email) {
        try {
                AttributeType emailAttr = AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build();

                SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(username)
                    .password(password)
                    .secretHash(calculateSecretHash(username))
                    .userAttributes(emailAttr)
                    .build();

                SignUpResponse result = cognitoClient.signUp(signUpRequest);
                return result.userSub();
        } catch (Exception e) {
            throw new RuntimeException("Sign up failed: " + e.getMessage());
        }
    }

    // Sign In
    public AuthenticationResultType signIn(String username, String password) {
        try {
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", password);
            authParams.put("SECRET_HASH", calculateSecretHash(username));

                InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(authParams)
                    .build();

                InitiateAuthResponse result = cognitoClient.initiateAuth(authRequest);
                return result.authenticationResult();
        } catch (Exception e) {
            throw new RuntimeException("Sign in failed: " + e.getMessage());
        }
    }

    // Sign Out
    public void signOut(String accessToken) {
        try {
                GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(accessToken)
                    .build();
                cognitoClient.globalSignOut(signOutRequest);
        } catch (Exception e) {
            throw new RuntimeException("Sign out failed: " + e.getMessage());
        }
    }
}
