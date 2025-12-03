package org.eyepax.staffauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationConfiguration {

    @Value("${adyen.api-key:}")
    private String adyenApiKey;

    @Value("${adyen.client-key:}")
    private String adyenClientKey;

    @Value("${adyen.merchant-account:}")
    private String adyenMerchantAccount;

    @Value("${adyen.environment:test}")
    private String adyenEnvironment;

    public String getAdyenApiKey() {
        return adyenApiKey;
    }

    public String getAdyenClientKey() {
        return adyenClientKey;
    }

    public String getAdyenMerchantAccount() {
        return adyenMerchantAccount;
    }

    public String getAdyenEnvironment() {
        return adyenEnvironment;
    }
}
