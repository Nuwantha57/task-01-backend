package org.eyepax.staffauth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.adyen.Client;
import com.adyen.Config;
import com.adyen.enums.Environment;
import com.adyen.service.checkout.PaymentsApi;

@Configuration
public class DependencyInjectionConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DependencyInjectionConfiguration.class);

    private final ApplicationConfiguration applicationConfiguration;

    public DependencyInjectionConfiguration(ApplicationConfiguration applicationConfiguration) {
        this.applicationConfiguration = applicationConfiguration;
    }

    @Bean
    public Client adyenClient() {
        String apiKey = applicationConfiguration.getAdyenApiKey();
        String env = applicationConfiguration.getAdyenEnvironment();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "Adyen API Key is not configured. Please set adyen.api-key in application.properties or ADYEN_API_KEY environment variable"
            );
        }

        logger.info("Initializing Adyen Client with environment: {}", env);
        logger.debug("API Key starts with: {}", apiKey.substring(0, Math.min(10, apiKey.length())));
        logger.debug("API Key length: {}", apiKey.length());

        Environment environment = "live".equalsIgnoreCase(env)
                ? Environment.LIVE
                : Environment.TEST;

        // Use Config object for proper initialization (Adyen SDK v21+)
        Config config = new Config();
        config.setApiKey(apiKey);
        config.setEnvironment(environment);
        
        logger.info("Adyen Client initialized successfully with environment: {}", environment);
        return new Client(config);
    }

    @Bean
    public PaymentsApi adyenPaymentsApi(Client adyenClient) {
        try {
            logger.info("Creating PaymentsApi instance...");
            PaymentsApi api = new PaymentsApi(adyenClient);
            logger.info("PaymentsApi created successfully");
            return api;
        } catch (Exception e) {
            logger.error("Failed to create PaymentsApi: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize PaymentsApi", e);
        }
    }
}
