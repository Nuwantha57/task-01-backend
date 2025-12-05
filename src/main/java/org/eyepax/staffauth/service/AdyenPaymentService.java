package org.eyepax.staffauth.service;

import java.io.IOException;
import java.util.Map;

import org.eyepax.staffauth.config.ApplicationConfiguration;
import org.eyepax.staffauth.dto.PaymentDetailsRequestDto;
import org.eyepax.staffauth.dto.PaymentRequestDto;
import org.eyepax.staffauth.dto.PaymentResponseDto;
import org.eyepax.staffauth.dto.SessionRequestDto;
import org.eyepax.staffauth.dto.SessionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.adyen.model.checkout.Amount;
import com.adyen.model.checkout.CheckoutPaymentMethod;
import com.adyen.model.checkout.CreateCheckoutSessionRequest;
import com.adyen.model.checkout.CreateCheckoutSessionResponse;
import com.adyen.model.checkout.PaymentCompletionDetails;
import com.adyen.model.checkout.PaymentDetailsRequest;
import com.adyen.model.checkout.PaymentDetailsResponse;
import com.adyen.model.checkout.PaymentRequest;
import com.adyen.model.checkout.PaymentResponse;
import com.adyen.service.checkout.PaymentsApi;
import com.adyen.service.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class AdyenPaymentService {

    private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentService.class);

    private final PaymentsApi paymentsApi;
    private final ApplicationConfiguration config;
    private final ObjectMapper objectMapper;

    public AdyenPaymentService(PaymentsApi paymentsApi, ApplicationConfiguration config) {
        this.paymentsApi = paymentsApi;
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Initiate a payment with Adyen
     */
    public PaymentResponseDto initiatePayment(PaymentRequestDto request) throws IOException, ApiException {
        logger.info("Initiating payment for reference: {}", request.getReference());

        // Validate configuration
        if (config.getAdyenMerchantAccount() == null || config.getAdyenMerchantAccount().trim().isEmpty()) {
            logger.error("Adyen Merchant Account is not configured");
            throw new IllegalStateException("Adyen Merchant Account is not configured. Please set adyen.merchant-account");
        }

        logger.debug("Adyen Configuration - Environment: {}, Merchant Account: {}", 
                config.getAdyenEnvironment(), 
                config.getAdyenMerchantAccount());

        // Build Adyen PaymentRequest
        PaymentRequest paymentsRequest = new PaymentRequest();

        // Amount
        Amount amount = new Amount()
                .currency(request.getAmount().getCurrency())
                .value(request.getAmount().getValue());
        paymentsRequest.setAmount(amount);

        // Merchant Account
        paymentsRequest.setMerchantAccount(config.getAdyenMerchantAccount());

        // Reference (your transaction ID)
        paymentsRequest.setReference(request.getReference());

        // Return URL (for 3DS redirects)
        paymentsRequest.setReturnUrl(request.getReturnUrl());

        // Payment Method (encrypted card data from Flutter)
        // Convert Map to CheckoutPaymentMethod using ObjectMapper
        CheckoutPaymentMethod paymentMethod = objectMapper.convertValue(
                request.getPaymentMethod(), 
                CheckoutPaymentMethod.class
        );
        paymentsRequest.setPaymentMethod(paymentMethod);

        // Shopper Reference (user ID)
        if (request.getShopperReference() != null) {
            paymentsRequest.setShopperReference(request.getShopperReference());
        }

        // Shopper Email (optional)
        if (request.getShopperEmail() != null) {
            paymentsRequest.setShopperEmail(request.getShopperEmail());
        }

        // Channel (always "Android" or "iOS" for mobile apps)
        paymentsRequest.setChannel(PaymentRequest.ChannelEnum.ANDROID);

        logger.debug("Calling Adyen /payments API...");

        // Call Adyen with enhanced error handling
        try {
            PaymentResponse paymentsResponse = paymentsApi.payments(paymentsRequest);

            logger.info("Adyen response - resultCode: {}, pspReference: {}",
                    paymentsResponse.getResultCode(),
                    paymentsResponse.getPspReference());

            // Map to DTO
            return mapToResponseDto(paymentsResponse, request.getReference());
            
        } catch (ApiException e) {
            logger.error("Adyen API Error Details:");
            logger.error("  Status Code: {}", e.getStatusCode());
            logger.error("  Error Message: {}", e.getMessage());
            logger.error("  Error: {}", e.getError());
            logger.error("  Response Body: {}", e.getResponseBody());
            
            // Re-throw with more context
            throw new ApiException(
                String.format("Adyen API call failed - Status: %d, Message: %s, Details: %s", 
                    e.getStatusCode(), 
                    e.getMessage(),
                    e.getError() != null ? e.getError().toString() : "No error details"),
                e.getStatusCode(),
                e.getResponseHeaders()
            );
        }
    }

    /**
     * Map Adyen PaymentResponse to our DTO
     */
    private PaymentResponseDto mapToResponseDto(PaymentResponse response, String merchantReference) {
        PaymentResponseDto dto = new PaymentResponseDto();

        dto.setResultCode(response.getResultCode().getValue());
        dto.setPspReference(response.getPspReference());
        dto.setMerchantReference(merchantReference);

        if (response.getRefusalReason() != null) {
            dto.setRefusalReason(response.getRefusalReason());
        }

        if (response.getAction() != null) {
            // Convert action to map using ObjectMapper
            @SuppressWarnings("unchecked")
            Map<String, Object> actionMap = objectMapper.convertValue(response.getAction(), Map.class);
            dto.setAction(actionMap);
        }

        return dto;
    }

    /**
     * Submit additional payment details (for 3DS completion)
     */
    public PaymentResponseDto submitPaymentDetails(PaymentDetailsRequestDto request) throws IOException, ApiException {
        logger.info("Submitting payment details...");

        // Build Adyen PaymentDetailsRequest
        PaymentDetailsRequest detailsRequest = new PaymentDetailsRequest();
        
        // Convert Map to PaymentCompletionDetails using ObjectMapper
        PaymentCompletionDetails completionDetails = objectMapper.convertValue(
                request.getDetails(), 
                PaymentCompletionDetails.class
        );
        detailsRequest.setDetails(completionDetails);
        detailsRequest.setPaymentData(request.getPaymentData());

        logger.debug("Calling Adyen /payments/details API...");

        // Call Adyen using paymentsApi
        PaymentDetailsResponse detailsResponse = paymentsApi.paymentsDetails(detailsRequest);

        logger.info("Adyen details response - resultCode: {}, pspReference: {}",
                detailsResponse.getResultCode(),
                detailsResponse.getPspReference());

        // Map to DTO (reuse same mapper since response structure is similar)
        return mapDetailsToResponseDto(detailsResponse);
    }

    /**
     * Map Adyen PaymentDetailsResponse to our DTO
     */
    private PaymentResponseDto mapDetailsToResponseDto(PaymentDetailsResponse response) {
        PaymentResponseDto dto = new PaymentResponseDto();

        dto.setResultCode(response.getResultCode().getValue());
        dto.setPspReference(response.getPspReference());

        if (response.getRefusalReason() != null) {
            dto.setRefusalReason(response.getRefusalReason());
        }

        // PaymentDetailsResponse doesn't have getAction() method
        // Actions are typically only in the initial payment response
        // If there are additional actions needed, they would be handled differently

        return dto;
    }
    

    /**
     * Create a payment session
     */
    public SessionResponseDto createSession(SessionRequestDto request) throws IOException, ApiException {

        try{
        // Build Adyen CreateCheckoutSessionRequest
        CreateCheckoutSessionRequest sessionRequest = new CreateCheckoutSessionRequest();

        // Amount
        Amount amount = new Amount()
                .currency(request.getAmount().getCurrency())
                .value(request.getAmount().getValue());
        sessionRequest.setAmount(amount);

        // Merchant Account
        sessionRequest.setMerchantAccount(config.getAdyenMerchantAccount());

        // Reference
        sessionRequest.setReference(request.getReference());

        // Return URL
        sessionRequest.setReturnUrl(request.getReturnUrl());

        // Country Code
        sessionRequest.setCountryCode(request.getCountryCode() != null ? request.getCountryCode() : "US");

        // Shopper Reference
        if (request.getShopperReference() != null) {
            sessionRequest.setShopperReference(request.getShopperReference());
        }

        // Shopper Email
        if (request.getShopperEmail() != null) {
            sessionRequest.setShopperEmail(request.getShopperEmail());
        }

        // Channel
        sessionRequest.setChannel(CreateCheckoutSessionRequest.ChannelEnum.ANDROID);

        logger.debug("Calling Adyen /sessions API...");
        logger.debug("Session Request Details:");
        logger.debug("  - Merchant Account: {}", sessionRequest.getMerchantAccount());
        logger.debug("  - Reference: {}", sessionRequest.getReference());
        logger.debug("  - Amount: {} {}", sessionRequest.getAmount().getValue(), sessionRequest.getAmount().getCurrency());
        logger.debug("  - Return URL: {}", sessionRequest.getReturnUrl());
        logger.debug("  - Country Code: {}", sessionRequest.getCountryCode());
        logger.debug("  - Channel: {}", sessionRequest.getChannel());

        // Call Adyen using PaymentsApi
        CreateCheckoutSessionResponse sessionResponse = paymentsApi.sessions(sessionRequest);

        logger.info("Adyen session created - sessionId: {}", sessionResponse.getId());

        // Map to DTO
        SessionResponseDto dto = new SessionResponseDto();
        dto.setId(sessionResponse.getId());
        dto.setSessionData(sessionResponse.getSessionData());

        return dto;
        } catch (ApiException e){
            logger.error("Adyen API Exception Details:");
            logger.error("  Status Code: {}", e.getStatusCode());
            logger.error("  Error Message: {}", e.getMessage());
            logger.error("  Error: {}", e.getError());
            logger.error("  Response Body: {}", e.getResponseBody());
            throw e;
        }
        
    }

} 