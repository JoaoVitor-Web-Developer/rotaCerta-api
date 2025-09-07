package com.rotacerta.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rotacerta.api.model.entities.Subscription;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.SubscriptionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscriptionFilter extends OncePerRequestFilter {
	private final SubscriptionRepository subscriptionRepository;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {

		if (!request.getRequestURI().startsWith("/api/quotes")) {
			filterChain.doFilter(request, response);
			return;
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated()) {
			User user = (User) authentication.getPrincipal();
			Subscription subscription = subscriptionRepository.findByUser(user).orElse(null);

			if (subscription != null &&
					subscription.getStatus() == Subscription.SubscriptionStatus.ACTIVE &&
					subscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now())) {

					Integer quoteLimit = subscription.getPlan().getQuoteLimit();

					if (quoteLimit != null && subscription.getQuoteCount() >= quoteLimit) {
						sendErrorResponse(response, "Acesso negado. Você atingiu o limite de cotações para o seu plano.");
						return;
					}

				filterChain.doFilter(request, response);
			} else {
				sendErrorResponse(response, "Acesso negado. É necessária uma assinatura ativa para realizar cotações.");
			}
		} else {
			sendErrorResponse(response, "Acesso negado. Usuário não autenticado.");
		}
	}

	private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType("application/json");
		response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", message)));
	}
}
