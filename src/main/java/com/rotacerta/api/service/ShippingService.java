package com.rotacerta.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rotacerta.api.config.MelhorEnvioConfig;
import com.rotacerta.api.dto.QuoteRequestDTO;
import com.rotacerta.api.dto.QuoteResponseDTO;
import com.rotacerta.api.dto.ShippingOptionDTO;
import com.rotacerta.api.exception.MelhorEnvioApiException;
import com.rotacerta.api.model.entities.Quote;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.QuoteRepository;
import com.rotacerta.api.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingService {

	private final WebClient.Builder webClientBuilder;
	private final MelhorEnvioConfig melhorEnvioConfig;
	private final ObjectMapper objectMapper;
	private final SubscriptionRepository subscriptionRepository;
	private final QuoteRepository quoteRepository;
	private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

	public Mono<QuoteResponseDTO> calculateShipping(QuoteRequestDTO request) {
		log.info("Iniciando cálculo de frete assíncrono para o CEP de destino: {}", request.getToPostalCode());

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

		logRequestDetails(requestBody);

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
		                                        .flatMap(errorBody -> {
			                                        log.error("====================== ERRO NA API MELHOR ENVIO ======================");
			                                        log.error("Status Code: {}", clientResponse.statusCode());
			                                        log.error("Response Headers: {}", clientResponse.headers().asHttpHeaders());
			                                        log.error("Response Body: {}", errorBody);
			                                        log.error("=======================================================================");
			                                        return Mono.error(new MelhorEnvioApiException("Erro na comunicação com Melhor Envio: " + clientResponse.statusCode()));
		                                        }))
		                .bodyToMono(JsonNode.class)
		                .map(this::mapResponseToDTO)
		                .doOnSuccess(responseDto -> {
			                saveQuoteHistory(request, responseDto);
			                incrementQuoteCountForCurrentUser();
		                });
	}

	private QuoteResponseDTO mapResponseToDTO(JsonNode response) {
		List<ShippingOptionDTO> options = new ArrayList<>();
		if(response != null && response.isArray()) {
			for(JsonNode node: response) {
				if(node.has("id") && !node.has("error")) {
					ShippingOptionDTO option = ShippingOptionDTO.builder()
					                                            .carrier(node.path("company").path("name").asText("N/A"))
					                                            .service(node.path("name").asText("N/A"))
					                                            .price(node.path("price").asDouble(0.0))
					                                            .deliveryTime(node.path("delivery_time").asInt(0))
					                                            .build();
					options.add(option);
				} else if(node.has("error")) {
					String errorMessage = node.path("error").asText("Erro desconhecido na opção de frete.");
					log.warn("Opção de frete ignorada devido a erro da API: {}", errorMessage);
				}
			}
		}
		return new QuoteResponseDTO(options);
	}

	private void logRequestDetails(Map<String, Object> requestBody) {
		try {
			log.info("====================== ENVIANDO REQUISIÇÃO PARA MELHOR ENVIO ======================");
			log.info("URL: {}", melhorEnvioConfig.getApiUrl() + "/api/v2/me/shipment/calculate");
			log.info("Token: Bearer {}...{}", melhorEnvioConfig.getApiToken().substring(0, 10), melhorEnvioConfig.getApiToken().substring(melhorEnvioConfig.getApiToken().length() - 10));
			log.info("User-Agent: {}", melhorEnvioConfig.getUserAgent());
			log.info("Request Body: {}", objectMapper.writeValueAsString(requestBody));
			log.info("===================================================================================");
		} catch (JsonProcessingException e) {
			log.error("Erro ao serializar o corpo da requisição para o log", e);
		}
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
		if (authentication == null || !authentication.isAuthenticated()) {
			return;
		}

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
			log.error("Falha ao serializar dados para o histórico de cotação do usuário {}", user.getEmail(), e);
		}
	}
}