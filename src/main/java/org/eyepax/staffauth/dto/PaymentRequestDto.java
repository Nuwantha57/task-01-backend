package org.eyepax.staffauth.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentRequestDto {

    @JsonProperty("amount")
    private AmountDto amount;

    @JsonProperty("reference")
    private String reference;  // Your order/transaction reference

    @JsonProperty("returnUrl")
    private String returnUrl;  // For 3DS redirects

    @JsonProperty("paymentMethod")
    private Map<String, Object> paymentMethod;  // Encrypted card data from Flutter

    @JsonProperty("shopperReference")
    private String shopperReference;  // User ID from Cognito

    @JsonProperty("shopperEmail")
    private String shopperEmail;  // Optional

    // Getters and Setters
    public AmountDto getAmount() {
        return amount;
    }

    public void setAmount(AmountDto amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public Map<String, Object> getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Map<String, Object> paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getShopperReference() {
        return shopperReference;
    }

    public void setShopperReference(String shopperReference) {
        this.shopperReference = shopperReference;
    }

    public String getShopperEmail() {
        return shopperEmail;
    }

    public void setShopperEmail(String shopperEmail) {
        this.shopperEmail = shopperEmail;
    }

    // Nested Amount class
    public static class AmountDto {
        @JsonProperty("currency")
        private String currency;  // e.g., "USD", "EUR"

        @JsonProperty("value")
        private Long value;  // Amount in minor units (e.g., 1000 = $10.00)

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Long getValue() {
            return value;
        }

        public void setValue(Long value) {
            this.value = value;
        }
    }
}
