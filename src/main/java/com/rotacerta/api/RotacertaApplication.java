package com.rotacerta.api;

import com.rotacerta.api.config.MelhorEnvioConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableConfigurationProperties(MelhorEnvioConfig.class)
@ComponentScan(basePackages = "com.rotacerta.api")
public class RotacertaApplication {

	public static void main(String[] args) {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		SpringApplication.run(RotacertaApplication.class, args);
	}

}
