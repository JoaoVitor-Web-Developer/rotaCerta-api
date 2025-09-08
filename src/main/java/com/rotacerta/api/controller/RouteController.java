package com.rotacerta.api.controller;

import com.rotacerta.api.dto.RouteOptimizationRequestDTO;
import com.rotacerta.api.dto.RouteOptimizationResponseDTO;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.service.RouteOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

	private final RouteOptimizationService routeOptimizationService;

	@PostMapping("/optimize")
	public ResponseEntity<RouteOptimizationResponseDTO> optimizeRoute(
			@Valid @RequestBody RouteOptimizationRequestDTO request,
			Authentication authentication
	) {
		User user = (User) authentication.getPrincipal();

		RouteOptimizationResponseDTO response = routeOptimizationService.optimizeRoute(request, user);

		return ResponseEntity.ok(response);
	}
}
