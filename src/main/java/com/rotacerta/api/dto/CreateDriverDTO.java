package com.rotacerta.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@NoArgsConstructor
public class CreateDriverDTO {

	@NotBlank(message = "O nome do entregador é obrigatório.")
	private String name;

	@NotNull
	private String phone;

	@NotNull
	private String vehicleType;

	@NotNull
	private String document;

	@Valid
	@NotEmpty(message = "É necessária pelo menos uma regra de preço.")
	private List<PricingRuleDTO> pricingRules;
}
