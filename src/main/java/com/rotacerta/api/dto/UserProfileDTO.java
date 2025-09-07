package com.rotacerta.api.dto;

import com.rotacerta.api.model.entities.Subscription;
import com.rotacerta.api.model.entities.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileDTO {
	private String email;
	private String companyName;
	private SubscriptionDTO subscription;

	public static UserProfileDTO from(User user, Subscription subscription) {
		return UserProfileDTO.builder()
				.email(user.getEmail())
				.companyName(user.getCompanyName())
				.subscription(SubscriptionDTO.from(subscription))
				.build();
	}

	@Data
	@Builder
	private static class SubscriptionDTO {
		private String planName;
		private String status;
		private int quoteCount;
		private Integer quoteLimit;
		private LocalDateTime periodEnd;

		public static SubscriptionDTO from(Subscription sub) {
			if (sub == null) return null;
			return SubscriptionDTO.builder()
					.planName(sub.getPlan().getName())
					.status(sub.getStatus().name())
					.quoteCount(sub.getQuoteCount())
					.quoteLimit(sub.getPlan().getQuoteLimit())
					.periodEnd(sub.getCurrentPeriodEnd())
					.build();
		}
	}
}
