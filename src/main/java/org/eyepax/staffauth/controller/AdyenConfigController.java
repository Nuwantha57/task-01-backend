package org.eyepax.staffauth.controller;

import org.eyepax.staffauth.config.ApplicationConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/adyen")
public class AdyenConfigController {

    private final ApplicationConfiguration config;

    public AdyenConfigController(ApplicationConfiguration config) {
        this.config = config;
    }

    @GetMapping("/config")
    public AdyenFrontendConfigDto getConfig() {
        return new AdyenFrontendConfigDto(
                config.getAdyenClientKey(),
                config.getAdyenEnvironment(),
                config.getAdyenMerchantAccount()
        );
    }

    // DTO class
    public static class AdyenFrontendConfigDto {
        public String clientKey;
        public String environment;
        public String merchantAccount;

        public AdyenFrontendConfigDto(String clientKey, String environment, String merchantAccount) {
            this.clientKey = clientKey;
            this.environment = environment;
            this.merchantAccount = merchantAccount;
        }
    }
}
