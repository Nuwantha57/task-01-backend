package org.eyepax.staffauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.adyen.Client;
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

        Environment environment = "live".equalsIgnoreCase(env)
                ? Environment.LIVE
                : Environment.TEST;

        return new Client(apiKey, environment);
    }

    @Bean
    public PaymentsApi adyenPaymentsApi(Client adyenClient) {
        return new PaymentsApi(adyenClient);
    }
}
