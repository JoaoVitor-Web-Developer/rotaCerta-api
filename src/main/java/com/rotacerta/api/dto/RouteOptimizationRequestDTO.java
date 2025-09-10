package com.rotacerta.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RouteOptimizationRequestDTO {

	@NotEmpty(message = "O endereço de origem é obrigatório.")
	private String origin;

	@NotEmpty(message = "É necessária pelo menos uma parada.")
	@Size(min = 1, message = "É necessária pelo menos uma parada.")
	private List<String> waypoints;

	private UUID driverId;
}
