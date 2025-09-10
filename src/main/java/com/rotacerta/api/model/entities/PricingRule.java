package com.rotacerta.api.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules")
@Data
@NoArgsConstructor
public class PricingRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", nullable = false)
	private TrustedDriver driver;

	@Column(nullable = false)
	private BigDecimal minDistanceKm;

	@Column(nullable = false)
	private BigDecimal maxDistanceKm;

	@Column(nullable = false)
	private BigDecimal price;
}
