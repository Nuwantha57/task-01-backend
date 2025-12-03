package org.eyepax.staffauth.service;

import java.io.IOException;
import java.util.Map;

import org.eyepax.staffauth.config.ApplicationConfiguration;
import org.eyepax.staffauth.dto.PaymentRequestDto;
import org.eyepax.staffauth.dto.PaymentResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.adyen.model.checkout.Amount;
import com.adyen.model.checkout.CheckoutPaymentMethod;
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
}
