package com.rotacerta.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShippingOptionDTO {
	private String carrier;
	private String service;
	private Double price;
	private Integer deliveryTime;
}
