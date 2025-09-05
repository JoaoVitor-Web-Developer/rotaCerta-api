package com.rotacerta.api.model.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
	@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "plan_id", referencedColumnName = "id", nullable = false)
	private Plan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionStatus status;

	private OffsetDateTime currentPeriodEnd;

	private OffsetDateTime renewsAt;

	@Column(unique = true)
	private String stripeCustomerId;

	@Column(unique = true)
	private String stripeSubscriptionId;

	@CreationTimestamp
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	private OffsetDateTime updatedAt;

	public enum SubscriptionStatus {
		ACTIVE,
		CANCELED,
		PAST_DUE,
		INCOMPLETE
	}
}