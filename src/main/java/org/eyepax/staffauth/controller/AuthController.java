package org.eyepax.staffauth.controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.eyepax.staffauth.dto.AuthResponse;
import org.eyepax.staffauth.dto.LoginRequest;
import org.eyepax.staffauth.dto.ResendCodeRequest;
import org.eyepax.staffauth.dto.SignUpRequest;
import org.eyepax.staffauth.dto.VerifyEmailRequest;
import org.eyepax.staffauth.service.CognitoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private CognitoService cognitoService;

    @Value("${aws.cognito.clientId}")
    private String clientId;
    
    @Value("${aws.cognito.clientSecret:}")  // Optional - empty default
    private String clientSecret;
    
    @Value("${aws.cognito.redirectUri:}")
    private String redirectUri;
    
    @Value("${aws.cognito.tokenEndpoint:}")
    private String tokenEndpoint;

    @PostMapping("/token")
    public ResponseEntity<Map<String,Object>> exchangeCode(@RequestParam String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);

        // Request body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // Exchange code for tokens
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> rawResponse = restTemplate.postForEntity(tokenEndpoint, request, Map.class);
        @SuppressWarnings("unchecked")
        Map<String,Object> bodyMap = (Map<String,Object>) rawResponse.getBody();
        return ResponseEntity.ok(bodyMap);
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@RequestBody SignUpRequest request) {
        try {
            System.out.println("========================================");
            System.out.println("Sign Up API Request Received");
            System.out.println("Username: " + request.getUsername());
            System.out.println("Email: " + request.getEmail());
            System.out.println("Name: " + request.getName());
            System.out.println("Phone: " + request.getPhoneNumber());
            System.out.println("========================================");
            
            // Validate required fields
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                throw new IllegalArgumentException("Username is required");
            }
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required");
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new IllegalArgumentException("Email is required");
            }
            
            String userId = cognitoService.signUp(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getName(),
                request.getPhoneNumber()
            );
            
            System.out.println("Sign up successful! User ID: " + userId);
            
            AuthResponse response = new AuthResponse(
                    true,
                    "User registered successfully. Please check your email to verify your account.",
                    null, null, null, null
            );
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
            AuthResponse response = new AuthResponse(
                    false,
                    e.getMessage(),
                    null, null, null, null
            );
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            System.err.println("Sign up failed: " + e.getMessage());
            e.printStackTrace();
            
            AuthResponse response = new AuthResponse(
                    false,
                    e.getMessage(),
                    null, null, null, null
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            System.out.println("========================================");
            System.out.println("Login API Request Received");
            System.out.println("Username: " + request.getUsername());
            System.out.println("========================================");
            
            AuthenticationResultType authResult = cognitoService.signIn(
                    request.getUsername(),
                    request.getPassword()
            );

            AuthResponse response = new AuthResponse(
                true,
                "Login successful",
                authResult.accessToken(),
                authResult.refreshToken(),
                authResult.idToken(),
                authResult.expiresIn()
            );
            
            System.out.println("Login successful!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            
            AuthResponse response = new AuthResponse(
                    false,
                    e.getMessage(),
                    null, null, null, null
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        try {
            System.out.println("========================================");
            System.out.println("Verify Email Request");
            System.out.println("Username: " + request.getUsername());
            System.out.println("========================================");
            
            cognitoService.confirmSignUp(request.getUsername(), request.getCode());
            
            AuthResponse response = new AuthResponse(
                    true,
                    "Email verified successfully! You can now sign in.",
                    null, null, null, null
            );
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Verification failed: " + e.getMessage());
            AuthResponse response = new AuthResponse(
                    false,
                    e.getMessage(),
                    null, null, null, null
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/resend-code")
    public ResponseEntity<AuthResponse> resendCode(@RequestBody ResendCodeRequest request) {
        try {
            cognitoService.resendConfirmationCode(request.getUsername());
            
            AuthResponse response = new AuthResponse(
                    true,
                    "Verification code resent successfully",
                    null, null, null, null
            );
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AuthResponse response = new AuthResponse(
                    false,
                    e.getMessage(),
                    null, null, null, null
            );
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String token) {
        try {
            System.out.println("========================================");
            System.out.println("Logout API Request Received");
            System.out.println("========================================");
            
            String accessToken = token.replace("Bearer ", "");
            cognitoService.signOut(accessToken);
            
            AuthResponse response = new AuthResponse(
                    true,
                    "Logged out successfully",
                    null, null, null, null
            );
            
            System.out.println("Logout successful!");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Logout failed: " + e.getMessage());
            
            AuthResponse response = new AuthResponse(
                    false,
                    e.getMessage(),
                    null, null, null, null
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
}
