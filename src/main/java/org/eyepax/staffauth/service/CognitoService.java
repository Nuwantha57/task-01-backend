package org.eyepax.staffauth.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

@Service
public class CognitoService {

    @Value("${aws.cognito.userPoolId}")
    private String userPoolId;

    @Value("${aws.cognito.clientId}")
    private String clientId;

    @Value("${aws.cognito.clientSecret:}")
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

    private String calculateSecretHash(String username) {
        if (clientSecret == null || clientSecret.isBlank()) {
            return null;
        }
        
        try {
            String message = username + clientId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }

    public String signUp(String username, String password, String email, String name, String phoneNumber) {
        try {
            System.out.println("=== Sign Up Request ===");
            System.out.println("Username: " + username);
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);
            System.out.println("Phone: " + phoneNumber);

            List<AttributeType> attributes = new ArrayList<>();
            
            attributes.add(AttributeType.builder()
                .name("email")
                .value(email)
                .build());

            if (name != null && !name.isBlank()) {
                attributes.add(AttributeType.builder()
                    .name("name")
                    .value(name)
                    .build());
            }

            if (phoneNumber != null && !phoneNumber.isBlank()) {
                attributes.add(AttributeType.builder()
                    .name("phone_number")
                    .value(phoneNumber)
                    .build());
            }

            System.out.println("Sending " + attributes.size() + " attributes");

            SignUpRequest.Builder builder = SignUpRequest.builder()
                .clientId(clientId)
                .username(username)
                .password(password)
                .userAttributes(attributes);

            String secretHash = calculateSecretHash(username);
            if (secretHash != null) {
                builder.secretHash(secretHash);
                System.out.println("Using secret hash");
            } else {
                System.out.println("No secret hash (public client)");
            }

            SignUpResponse result = cognitoClient.signUp(builder.build());
            
            System.out.println("✓ Sign up successful! User Sub: " + result.userSub());
            System.out.println("======================");
            
            return result.userSub();
            
        } catch (Exception e) {
            System.err.println("✗ Sign up failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void confirmSignUp(String username, String code) {
        try {
            ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(username)
                .confirmationCode(code)
                .build();

            cognitoClient.confirmSignUp(confirmRequest);
            System.out.println("✓ Email verified successfully");
            
        } catch (CodeMismatchException e) {
            throw new RuntimeException("Invalid verification code");
        } catch (ExpiredCodeException e) {
            throw new RuntimeException("Verification code has expired");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void resendConfirmationCode(String username) {
        try {
            ResendConfirmationCodeRequest request = ResendConfirmationCodeRequest.builder()
                .clientId(clientId)
                .username(username)
                .build();

            cognitoClient.resendConfirmationCode(request);
            System.out.println("✓ Confirmation code resent");
            
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public AuthenticationResultType signIn(String username, String password) {
        try {
            System.out.println("=== Sign In Request ===");
            System.out.println("Username: " + username);

            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", password);
            
            String secretHash = calculateSecretHash(username);
            if (secretHash != null) {
                authParams.put("SECRET_HASH", secretHash);
            }

            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .clientId(clientId)
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParams)
                .build();

            InitiateAuthResponse result = cognitoClient.initiateAuth(authRequest);
            
            System.out.println("✓ Sign in successful!");
            System.out.println("======================");
            
            return result.authenticationResult();
            
        } catch (NotAuthorizedException e) {
            throw new RuntimeException("Invalid username or password");
        } catch (UserNotFoundException e) {
            throw new RuntimeException("User not found");
        } catch (UserNotConfirmedException e) {
            throw new RuntimeException("Please verify your email before signing in");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void signOut(String accessToken) {
        try {
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                .accessToken(accessToken)
                .build();
            cognitoClient.globalSignOut(signOutRequest);
            System.out.println("✓ Sign out successful!");
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
