package com.rotacerta.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class QuoteRequestDTO {

	@NotNull(message = "O CEP de origem não pode ser nulo.")
	private String fromPostalCode;

	@NotNull(message = "O CEP de destino não pode ser nulo.")
	private String toPostalCode;

	@JsonProperty("package")
	@JsonAlias("aPackage")
	@NotNull(message = "Os dados do pacote não podem ser nulos.")
	@Valid
	private PackageDTO aPackage;

	@Data
	@NoArgsConstructor
	public static class PackageDTO {
		@NotNull(message = "O peso não pode ser nulo.")
		private Double weight;

		@NotNull(message = "A largura não pode ser nula.")
		private Double width;

		@NotNull(message = "A altura não pode ser nula.")
		private Double height;

		@NotNull(message = "O comprimento não pode ser nulo.")
		private Double length;
	}
}
