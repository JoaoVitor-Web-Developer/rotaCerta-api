package com.rotacerta.api.dto;

import com.rotacerta.api.model.entities.TrustedDriver;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class DriverResponseDTO {

	private UUID id;
	private String name;
	private String phone;
	private String vehicleType;
	private List<PricingRuleDTO> pricingRules;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

	public static DriverResponseDTO fromEntity(TrustedDriver driver) {
		List<PricingRuleDTO> rules = driver.getPricingRules().stream()
		                                   .map(rule -> {
			                                   PricingRuleDTO dto = new PricingRuleDTO();
			                                   dto.setMinDistanceKm(rule.getMinDistanceKm());
			                                   dto.setMaxDistanceKm(rule.getMaxDistanceKm());
			                                   dto.setPrice(rule.getPrice());
			                                   return dto;
		                                   })
		                                   .collect(Collectors.toList());

		return DriverResponseDTO.builder()
		                        .id(driver.getId())
		                        .name(driver.getName())
		                        .phone(driver.getPhone())
		                        .vehicleType(driver.getVehicleType())
		                        .pricingRules(rules)
		                        .createdAt(driver.getCreatedAt())
		                        .updatedAt(driver.getUpdatedAt())
		                        .build();
	}
}
