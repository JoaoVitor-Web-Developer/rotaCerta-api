package com.rotacerta.api.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
public class Subscription {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "plan_id", nullable = false)
	private Plan plan;

	@Column(name = "stripe_subscription_id", unique = true)
	private String stripeSubscriptionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionStatus status;

	@Column(name = "current_period_end")
	private LocalDateTime currentPeriodEnd;

	@Column(name = "quote_count", nullable = false)
	private int quoteCount = 0;

	@CreationTimestamp
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	private OffsetDateTime updatedAt;

	public enum SubscriptionStatus {
		ACTIVE,
		CANCELED,
		INACTIVE,
		PAST_DUE
	}
}