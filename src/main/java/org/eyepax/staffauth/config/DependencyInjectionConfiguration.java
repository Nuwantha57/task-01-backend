package org.eyepax.staffauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.adyen.Client;
import com.adyen.Config;
import com.adyen.enums.Environment;
import com.adyen.service.checkout.PaymentsApi;

@Configuration
public class DependencyInjectionConfiguration {

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

        Environment environment = "live".equalsIgnoreCase(env)
                ? Environment.LIVE
                : Environment.TEST;

        // Use Config object for proper initialization (Adyen SDK v21+)
        Config config = new Config();
        config.setApiKey(apiKey);
        config.setEnvironment(environment);
        
        return new Client(config);
    }

    @Bean
    public PaymentsApi adyenPaymentsApi(Client adyenClient) {
        return new PaymentsApi(adyenClient);
    }
}
