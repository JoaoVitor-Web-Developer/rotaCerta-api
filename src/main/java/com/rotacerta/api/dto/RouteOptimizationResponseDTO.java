package com.rotacerta.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RouteOptimizationResponseDTO {

	private UUID routeId;
	private String originAddress;
	private List<String> optimizedStopOrder;
	private String totalDistance;
	private String totalDuration;
	private String googleMapsUrl;
	private BigDecimal totalCost;
}
