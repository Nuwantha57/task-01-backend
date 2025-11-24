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
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

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

    // Calculate Secret Hash - FIXED
    private String calculateSecretHash(String username) {
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

    // Sign Up - UPDATED to include all required attributes
    public String signUp(String username, String password, String email, String name, String phoneNumber) {
        try {
            System.out.println("=== Sign Up Request ===");
            System.out.println("Username: " + username);
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);
            System.out.println("Phone: " + phoneNumber);

            // Build user attributes list
            List<AttributeType> attributes = new ArrayList<>();
            
            // Required: email
            attributes.add(AttributeType.builder()
                .name("email")
                .value(email)
                .build());

            // Required: preferred_username (use username as default)
            attributes.add(AttributeType.builder()
                .name("preferred_username")
                .value(username)
                .build());

            // Required: name (simple name attribute)
            if (name != null && !name.isBlank()) {
                attributes.add(AttributeType.builder()
                    .name("name")
                    .value(name)
                    .build());
            }

            // Required: name.formatted (full formatted name)
            if (name != null && !name.isBlank()) {
                attributes.add(AttributeType.builder()
                    .name("name.formatted")
                    .value(name)
                    .build());
            }

            // Required: phoneNumbers (note: plural)
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                attributes.add(AttributeType.builder()
                    .name("phoneNumbers")
                    .value(phoneNumber)
                    .build());
            } else {
                // Provide default if not given
                attributes.add(AttributeType.builder()
                    .name("phoneNumbers")
                    .value("+10000000000")  // Default placeholder
                    .build());
            }

            // Build and execute sign up request
            SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .username(username)
                .password(password)
                .secretHash(calculateSecretHash(username))
                .userAttributes(attributes)
                .build();

            SignUpResponse result = cognitoClient.signUp(signUpRequest);
            
            System.out.println("Sign up successful! User Sub: " + result.userSub());
            System.out.println("======================");
            
            return result.userSub();
            
        } catch (UsernameExistsException e) {
            System.err.println("Username already exists: " + username);
            throw new RuntimeException("Username already exists");
        } catch (InvalidPasswordException e) {
            System.err.println("Invalid password: " + e.getMessage());
            throw new RuntimeException("Password does not meet requirements");
        } catch (InvalidParameterException e) {
            System.err.println("Invalid parameter: " + e.getMessage());
            throw new RuntimeException("Invalid input: " + e.getMessage());
        } catch (CognitoIdentityProviderException e) {
            System.err.println("Cognito error: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Sign up failed: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Sign up failed: " + e.getMessage());
        }
    }


    // Sign In - CORRECTED
    public AuthenticationResultType signIn(String username, String password) {
        try {
            System.out.println("=== Sign In Request ===");
            System.out.println("Username: " + username);

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
            
            System.out.println("Sign in successful!");
            System.out.println("======================");
            
            return result.authenticationResult();
            
        } catch (NotAuthorizedException e) {
            System.err.println("Not authorized: " + e.getMessage());
            throw new RuntimeException("Invalid username or password");
        } catch (UserNotFoundException e) {
            System.err.println("User not found: " + username);
            throw new RuntimeException("User not found");
        } catch (UserNotConfirmedException e) {
            System.err.println("User not confirmed: " + username);
            throw new RuntimeException("Please verify your email before signing in");
        } catch (CognitoIdentityProviderException e) {
            System.err.println("Cognito error: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Sign in failed: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during sign in: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Sign in failed: " + e.getMessage());
        }
    }

    // Sign Out - CORRECTED
    public void signOut(String accessToken) {
        try {
            System.out.println("=== Sign Out Request ===");
            
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                .accessToken(accessToken)
                .build();
                
            cognitoClient.globalSignOut(signOutRequest);
            
            System.out.println("Sign out successful!");
            System.out.println("======================");
            
        } catch (CognitoIdentityProviderException e) {
            System.err.println("Cognito error during sign out: " + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Sign out failed: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error during sign out: " + e.getMessage());
            throw new RuntimeException("Sign out failed: " + e.getMessage());
        }
    }
}
