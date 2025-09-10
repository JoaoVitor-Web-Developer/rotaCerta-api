package com.rotacerta.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rotacerta.api.dto.QuoteHistoryDTO;
import com.rotacerta.api.dto.QuoteRequestDTO;
import com.rotacerta.api.dto.QuoteResponseDTO;
import com.rotacerta.api.model.entities.Quote;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.QuoteRepository;
import com.rotacerta.api.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quotes")
@RequiredArgsConstructor
public class QuoteController {

	private final ShippingService shippingService;
	private final QuoteRepository quoteRepository;

	private final ObjectMapper objectMapper;

	@PostMapping("/calculate")
	public ResponseEntity<QuoteResponseDTO> calculate(@Valid @RequestBody QuoteRequestDTO request, Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		Mono<QuoteResponseDTO> quoteResponseMono = shippingService.calculateShipping(request, user);
		QuoteResponseDTO response = quoteResponseMono.block(Duration.ofSeconds(10));
		return ResponseEntity.ok(response);
	}

	@GetMapping("/history")
	public ResponseEntity<List<QuoteHistoryDTO>> getQuoteHistory(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		List<Quote> historyEntities = quoteRepository.findByUserOrderByCreatedAtDesc(user);

		List<QuoteHistoryDTO> historyDTOs = historyEntities.stream()
		                                                   .map(quote -> QuoteHistoryDTO.fromEntity(quote, objectMapper))
		                                                   .collect(Collectors.toList());

		return ResponseEntity.ok(historyDTOs);
	}
}

