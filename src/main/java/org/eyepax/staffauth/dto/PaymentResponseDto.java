package org.eyepax.staffauth.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentResponseDto {

    @JsonProperty("resultCode")
    private String resultCode;  // "Authorised", "Refused", "RedirectShopper", etc.

    @JsonProperty("pspReference")
    private String pspReference;  // Adyen's unique transaction reference

    @JsonProperty("refusalReason")
    private String refusalReason;  // If refused

    @JsonProperty("action")
    private Map<String, Object> action;  // For 3DS or other actions

    @JsonProperty("merchantReference")
    private String merchantReference;  // Your reference

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getPspReference() {
        return pspReference;
    }

    public void setPspReference(String pspReference) {
        this.pspReference = pspReference;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public Map<String, Object> getAction() {
        return action;
    }

    public void setAction(Map<String, Object> action) {
        this.action = action;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public void setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
    }
}
