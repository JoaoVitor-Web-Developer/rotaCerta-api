package com.rotacerta.api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.api.stripe")
public class StripeConfig {
	private String secretKey;
	private String webhookSecret;

	@PostConstruct
	public void init() {
		Stripe.apiKey = secretKey;
	}
}
