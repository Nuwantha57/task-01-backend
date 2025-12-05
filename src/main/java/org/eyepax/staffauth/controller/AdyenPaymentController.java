package org.eyepax.staffauth.controller;

import java.io.IOException;

import org.eyepax.staffauth.dto.PaymentDetailsRequestDto;
import org.eyepax.staffauth.dto.PaymentRequestDto;
import org.eyepax.staffauth.dto.PaymentResponseDto;
import org.eyepax.staffauth.dto.SessionRequestDto;
import org.eyepax.staffauth.dto.SessionResponseDto;
import org.eyepax.staffauth.service.AdyenPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adyen.service.exception.ApiException;

@RestController
@RequestMapping("/api/payments/adyen")
public class AdyenPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(AdyenPaymentController.class);

    private final AdyenPaymentService adyenPaymentService;

    public AdyenPaymentController(AdyenPaymentService adyenPaymentService) {
        this.adyenPaymentService = adyenPaymentService;
    }

    /**
     * Initiate a payment
     * POST /api/payments/adyen/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentRequestDto request) {
        try {
            logger.info("Received payment initiation request for reference: {}", request.getReference());

            PaymentResponseDto response = adyenPaymentService.initiatePayment(request);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            logger.error("Configuration error: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("Configuration error: " + e.getMessage()));

        } catch (ApiException e) {
            logger.error("Adyen API error: {}", e.getMessage(), e);
            String detailedMessage = "Adyen API error: " + e.getMessage();
            if (e.getError() != null) {
                detailedMessage += " - " + e.getError();
            }
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(detailedMessage));

        } catch (IOException e) {
            logger.error("IO error calling Adyen: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Payment processing error: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    // Simple error response class
    static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

        /**
     * Submit payment details (for 3DS completion)
     * POST /api/payments/adyen/details
     */
    @PostMapping("/details")
    public ResponseEntity<?> submitPaymentDetails(@RequestBody PaymentDetailsRequestDto request) {
        try {
            logger.info("Received payment details submission");

            PaymentResponseDto response = adyenPaymentService.submitPaymentDetails(request);

            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            logger.error("Adyen API error: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Adyen API error: " + e.getMessage()));

        } catch (IOException e) {
            logger.error("IO error calling Adyen: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Payment processing error"));

        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error"));
        }
    }


        /**
     * Create a payment session
     * POST /api/payments/adyen/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody SessionRequestDto request) {
        try {
            logger.info("Received session creation request for reference: {}", request.getReference());

            SessionResponseDto response = adyenPaymentService.createSession(request);

            return ResponseEntity.ok(response);

        } catch (ApiException e) {
            logger.error("Adyen API error: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Adyen API error: " + e.getMessage()));

        } catch (IOException e) {
            logger.error("IO error calling Adyen: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Session creation error"));

        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error"));
        }
    }

}
