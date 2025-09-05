package com.rotacerta.api.dto;

import lombok.Data;

@Data
public class QuoteRequestDTO {
	private String fromPostalCode;
	private String toPostalCode;
	private PackageDTO aPackage;

	@Data
	public static class PackageDTO {
		private double weight;
		private double width;
		private double height;
		private double length;
	}
}
