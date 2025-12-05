package org.eyepax.staffauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SessionRequestDto {

    @JsonProperty("amount")
    private PaymentRequestDto.AmountDto amount;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("returnUrl")
    private String returnUrl;

    @JsonProperty("shopperReference")
    private String shopperReference;

    @JsonProperty("shopperEmail")
    private String shopperEmail;

    @JsonProperty("countryCode")
    private String countryCode;

    // Getters and Setters
    public PaymentRequestDto.AmountDto getAmount() {
        return amount;
    }

    public void setAmount(PaymentRequestDto.AmountDto amount) {
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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
