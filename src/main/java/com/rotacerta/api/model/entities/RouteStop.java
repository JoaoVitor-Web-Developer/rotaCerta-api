package com.rotacerta.api.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "route_stops")
@Data
@NoArgsConstructor
public class RouteStop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id", nullable = false)
	private OptimizedRoute route;

	@Column(nullable = false)
	private String addressText;

	private Double latitude;
	private Double longitude;

	@Column(nullable = false)
	private int stopOrder;
}
