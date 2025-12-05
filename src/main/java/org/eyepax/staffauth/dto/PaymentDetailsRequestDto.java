package org.eyepax.staffauth.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentDetailsRequestDto {

    @JsonProperty("details")
    private Map<String, String> details;  // Details from 3DS redirect/response

    @JsonProperty("paymentData")
    private String paymentData;  // Encrypted payload from Adyen

    // Getters and Setters
    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public String getPaymentData() {
        return paymentData;
    }

    public void setPaymentData(String paymentData) {
        this.paymentData = paymentData;
    }
}
