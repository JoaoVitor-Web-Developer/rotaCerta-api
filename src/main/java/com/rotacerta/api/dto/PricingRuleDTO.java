package com.rotacerta.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PricingRuleDTO {

	@NotNull(message = "A distância mínima é obrigatória.")
	private BigDecimal minDistanceKm;

	@NotNull(message = "A distância máxima é obrigatória.")
	private BigDecimal maxDistanceKm;

	@NotNull(message = "O preço é obrigatório.")
	private BigDecimal price;
}
