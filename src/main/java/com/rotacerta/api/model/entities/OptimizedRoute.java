package com.rotacerta.api.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "optimized_route")
@Data
@NoArgsConstructor
public class OptimizedRoute {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RouteStop> stops = new ArrayList<>();

	private Integer totalDistanceMeters;
	private Integer totalDurationSeconds;
	private BigDecimal totalCost;

	@Enumerated(EnumType.STRING)
	private RouteStatus status = RouteStatus.PENDING;

	@CreationTimestamp
	private OffsetDateTime createdAt;

	public enum RouteStatus {
		PENDING,
		COMPLETED,
		CANCELED
	}
}
