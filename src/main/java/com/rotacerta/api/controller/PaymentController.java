package com.rotacerta.api.controller;

import com.rotacerta.api.config.StripeConfig;
import com.rotacerta.api.model.entities.Plan;
import com.rotacerta.api.model.entities.Subscription;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.PlanRepository;
import com.rotacerta.api.repository.SubscriptionRepository;
import com.rotacerta.api.repository.UserRepository;
import com.rotacerta.api.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.LineItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;
	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PlanRepository planRepository;
	private final StripeConfig stripeConfig;
	private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

	@PostMapping("/create-checkout-session")
	public ResponseEntity<Map<String, String>> createCheckoutSession(Authentication authentication, @RequestBody Map<String, String> payload) {
		try {
			String userEmail = authentication.getName();
			String priceId = payload.get("priceId");
			String checkoutUrl = paymentService.createCheckoutSession(userEmail, priceId);
			return ResponseEntity.ok(Map.of("url", checkoutUrl));
		} catch (Exception e) {
			log.error("Erro ao criar sessão de checkout: {}", e.getMessage());
			return ResponseEntity.status(500).body(Map.of("error", "Não foi possível iniciar o pagamento."));
		}
	}

	@PostMapping("/webhook")
	public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
		Event event;
		try {
			event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
		} catch (SignatureVerificationException e) {
			log.warn("Falha na verificação da assinatura do Webhook: {}", e.getMessage());
			return ResponseEntity.status(400).body("Assinatura inválida.");
		}

		switch (event.getType()) {
			case "checkout.session.completed":
				Session session = (Session) event.getData().getObject();
				handleCheckoutSessionCompleted(session);
				break;
			default:
				log.info("Evento não tratado recebido: {}", event.getType());
		}

		return ResponseEntity.ok("Recebido.");
	}

	@Transactional
	protected void handleCheckoutSessionCompleted(Session session) {
		try {
			String userIdStr = session.getClientReferenceId();
			String subscriptionId = session.getSubscription();

			String priceId = session.listLineItems().getData().stream()
			                        .findFirst()
			                        .map(LineItem::getPrice)
			                        .map(com.stripe.model.Price::getId)
			                        .orElse(null);

			if (userIdStr == null || subscriptionId == null || priceId == null) {
				log.error("Webhook recebido com dados incompletos. Session ID: {}", session.getId());
				return;
			}

			User user = userRepository.findById(UUID.fromString(userIdStr)).orElse(null);

			Plan plan = planRepository.findByStripePriceId(priceId).orElse(null);

			if (user == null || plan == null) {
				log.error("FATAL: Usuário ou Plano não encontrado no banco de dados. " +
						          "Verifique se o stripe_price_id '{}' está cadastrado na tabela 'plans'. User ID: {}", priceId, userIdStr);
				return;
			}

			com.stripe.model.Subscription retrievedSubscription = com.stripe.model.Subscription.retrieve(subscriptionId);

			Subscription subscription = subscriptionRepository.findByUser(user)
			                                                  .orElse(new Subscription());

			subscription.setUser(user);
			subscription.setPlan(plan);
			subscription.setStripeSubscriptionId(retrievedSubscription.getId());
			subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);

			long periodEndTimestamp = retrievedSubscription.getCurrentPeriodEnd();
			Instant periodEndInstant = Instant.ofEpochSecond(periodEndTimestamp);
			subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(periodEndInstant, ZoneOffset.UTC));

			subscriptionRepository.save(subscription);
			log.info("Assinatura do plano '{}' atualizada/criada com sucesso para o usuário {}", plan.getName(), user.getEmail());

		} catch (StripeException e) {
			log.error("Erro ao processar webhook de checkout.session.completed. Session ID: {}. Erro: {}", session.getId(), e.getMessage());
		}
	}
}