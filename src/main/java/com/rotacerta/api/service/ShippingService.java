package com.rotacerta.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.*;
import com.rotacerta.api.config.MelhorEnvioConfig;
import com.rotacerta.api.dto.QuoteRequestDTO;
import com.rotacerta.api.dto.QuoteResponseDTO;
import com.rotacerta.api.dto.ShippingOptionDTO;
import com.rotacerta.api.exception.MelhorEnvioApiException;
import com.rotacerta.api.model.entities.*;
import com.rotacerta.api.repository.QuoteRepository;
import com.rotacerta.api.repository.SubscriptionRepository;
import com.rotacerta.api.repository.TrustedDriverRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShippingService {

	private final WebClient.Builder webClientBuilder;
	private final MelhorEnvioConfig melhorEnvioConfig;
	private final ObjectMapper objectMapper;
	private final SubscriptionRepository subscriptionRepository;
	private final QuoteRepository quoteRepository;
	private final TrustedDriverRepository trustedDriverRepository;
	private final GeoApiContext geoApiContext;
	private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

	// --- A MELHORIA ESTÁ AQUI: DEFINIMOS UM LIMITE ---
	private static final double MAX_LOCAL_DELIVERY_DISTANCE_KM = 100.0; // Limite de 100km para entregas locais

	public Mono<QuoteResponseDTO> calculateShipping(QuoteRequestDTO request, User user) {
		log.info("Iniciando cálculo de frete HÍBRIDO para o utilizador: {}", user.getEmail());

		Mono<List<ShippingOptionDTO>> melhorEnvioOptionsMono = getMelhorEnvioOptions(request);
		List<ShippingOptionDTO> localDriverOptions = getLocalDriverOptions(request, user);

		return melhorEnvioOptionsMono.map(melhorEnvioOptions -> {
			                             List<ShippingOptionDTO> allOptions = new ArrayList<>(melhorEnvioOptions);
			                             allOptions.addAll(localDriverOptions);
			                             return new QuoteResponseDTO(allOptions);
		                             })
		                             .doOnSuccess(responseDto -> {
			                             saveQuoteHistory(request, responseDto);
			                             incrementQuoteCountForCurrentUser();
		                             });
	}

	private Mono<List<ShippingOptionDTO>> getMelhorEnvioOptions(QuoteRequestDTO request) {
		WebClient webClient = webClientBuilder.baseUrl(melhorEnvioConfig.getApiUrl()).build();
		Map<String, Object> requestBody = Map.of(
				"from", Map.of("postal_code", request.getFromPostalCode()),
				"to", Map.of("postal_code", request.getToPostalCode()),
				"package", Map.of(
						"weight", request.getAPackage().getWeight(),
						"width", request.getAPackage().getWidth(),
						"height", request.getAPackage().getHeight(),
						"length", request.getAPackage().getLength()
				                 )
		                                        );

		return webClient.post()
		                .uri("/api/v2/me/shipment/calculate")
		                .header("Authorization", "Bearer " + melhorEnvioConfig.getApiToken())
		                .header("Accept", "application/json")
		                .header("Content-Type", "application/json")
		                .header("User-Agent", melhorEnvioConfig.getUserAgent())
		                .bodyValue(requestBody)
		                .retrieve()
		                .onStatus(HttpStatusCode::isError, clientResponse ->
				                clientResponse.bodyToMono(String.class)
				                              .defaultIfEmpty("[CORPO DA RESPOSTA VAZIO]")
				                              .flatMap(errorBody -> Mono.error(new MelhorEnvioApiException("Erro Melhor Envio: " + clientResponse.statusCode()))))
		                .bodyToMono(JsonNode.class)
		                .map(this::mapMelhorEnvioResponseToDTO);
	}

	private List<ShippingOptionDTO> getLocalDriverOptions(QuoteRequestDTO request, User user) {
		try {
			DistanceMatrix matrix = DistanceMatrixApi.newRequest(geoApiContext)
			                                         .origins(request.getFromPostalCode())
			                                         .destinations(request.getToPostalCode())
			                                         .mode(TravelMode.DRIVING)
			                                         .units(Unit.METRIC)
			                                         .await();

			if (matrix.rows.length == 0 || matrix.rows[0].elements.length == 0 || matrix.rows[0].elements[0].status != DistanceMatrixElementStatus.OK || matrix.rows[0].elements[0].distance == null) {
				log.warn("A Google não conseguiu calcular uma distância válida entre {} e {}.", request.getFromPostalCode(), request.getToPostalCode());
				return List.of();
			}

			long distanceMeters = matrix.rows[0].elements[0].distance.inMeters;
			BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters / 1000.0);

			// --- A MELHORIA ESTÁ AQUI: VERIFICAÇÃO DO LIMITE DE DISTÂNCIA ---
			if (distanceKm.doubleValue() > MAX_LOCAL_DELIVERY_DISTANCE_KM) {
				log.info("Distância de {} km excede o limite para entregas locais. A ignorar cotação com entregadores.", distanceKm);
				return List.of();
			}

			log.info("Distância calculada para entrega local: {} km", distanceKm);
			List<TrustedDriver> drivers = trustedDriverRepository.findByUser(user);

			return drivers.stream().map(driver ->
					                            driver.getPricingRules().stream()
					                                  .filter(rule -> distanceKm.compareTo(rule.getMinDistanceKm()) >= 0 && distanceKm.compareTo(rule.getMaxDistanceKm()) <= 0)
					                                  .findFirst()
					                                  .map(rule -> ShippingOptionDTO.builder()
					                                                                .carrier(driver.getName())
					                                                                .service("Entrega Local")
					                                                                .price(rule.getPrice().doubleValue())
					                                                                .deliveryTime(0)
					                                                                .build()
					                                      )
			                           ).filter(java.util.Optional::isPresent)
			              .map(java.util.Optional::get)
			              .collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Erro ao calcular opções de entregadores locais para o utilizador {}: {}", user.getEmail(), e.getMessage());
			return List.of();
		}
	}

	private List<ShippingOptionDTO> mapMelhorEnvioResponseToDTO(JsonNode response) {
		List<ShippingOptionDTO> options = new ArrayList<>();
		if(response != null && response.isArray()) {
			for(JsonNode node: response) {
				if(node.has("id") && !node.has("error")) {
					options.add(ShippingOptionDTO.builder()
					                             .carrier(node.path("company").path("name").asText("N/A"))
					                             .service(node.path("name").asText("N/A"))
					                             .price(node.path("price").asDouble(0.0))
					                             .deliveryTime(node.path("delivery_time").asInt(0))
					                             .build());
				}
			}
		}
		return options;
	}

	@Transactional
	public void incrementQuoteCountForCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated()) {
			User user = (User) authentication.getPrincipal();
			subscriptionRepository.findByUser(user).ifPresent(subscription -> {
				subscription.setQuoteCount(subscription.getQuoteCount() + 1);
				subscriptionRepository.save(subscription);
			});
		}
	}

	@Transactional
	public void saveQuoteHistory(QuoteRequestDTO request, QuoteResponseDTO response) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) return;

		User user = (User) authentication.getPrincipal();
		try {
			Quote quote = new Quote();
			quote.setUser(user);
			quote.setOriginZip(request.getFromPostalCode());
			quote.setDestZip(request.getToPostalCode());
			quote.setPayload(objectMapper.writeValueAsString(request.getAPackage()));
			quote.setResult(objectMapper.writeValueAsString(response.getOptions()));
			quoteRepository.save(quote);
		} catch (JsonProcessingException e) {
			log.error("Falha ao serializar dados para o histórico de cotação do utilizador {}", user.getEmail(), e);
		}
	}
}