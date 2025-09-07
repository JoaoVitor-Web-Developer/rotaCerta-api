package com.rotacerta.api.service;

import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.UserRepository;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import com.stripe.exception.StripeException;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final UserRepository userRepository;

	public String createCheckoutSession(String userEmail, String priceId) throws StripeException {
		User user = userRepository.findByEmail(userEmail)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
	//TODO - Urls de pag. do front
		String successUrl = "http://localhost:3000/dashboard?payment=sucess";
		String cancelUrl = "http://localhost:3000/dashboard?payment=cancel";

		SessionCreateParams params = SessionCreateParams.builder()
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setSuccessUrl(successUrl)
				.setCancelUrl(cancelUrl)
				.setClientReferenceId(user.getId().toString())
				.addLineItem(
						SessionCreateParams.LineItem.builder()
								.setQuantity(1L)
								.setPrice(priceId)
								.build())
				.build();

		Session session = Session.create(params);

		return session.getUrl();
	}
}
