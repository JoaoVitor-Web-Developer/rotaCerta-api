package com.rotacerta.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.shipping.melhor-envio")
public class MelhorEnvioConfig {
	private String apiUrl;
	private String apiToken;
	private String userAgent;
}
