package com.rotacerta.api.service;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;
import com.rotacerta.api.dto.RouteOptimizationRequestDTO;
import com.rotacerta.api.dto.RouteOptimizationResponseDTO;
import com.rotacerta.api.model.entities.*;
import com.rotacerta.api.repository.OptimizedRouteRepository;
import com.rotacerta.api.repository.RouteStopRepository;
import com.rotacerta.api.repository.TrustedDriverRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

	private final GeoApiContext geoApiContext;
	private final OptimizedRouteRepository optimizedRouteRepository;
	private final RouteStopRepository routeStopRepository;
	private final TrustedDriverRepository trustedDriverRepository;
	private static final Logger log = LoggerFactory.getLogger(RouteOptimizationService.class);

	@Transactional
	public RouteOptimizationResponseDTO optimizeRoute(RouteOptimizationRequestDTO request, User user) {
		try {
			log.info("Iniciando otimização de rota para o utilizador: {}", user.getEmail());

			DirectionsResult result = DirectionsApi.newRequest(geoApiContext)
			                                       .origin(request.getOrigin())
			                                       .destination(request.getOrigin())
			                                       .mode(TravelMode.DRIVING)
			                                       .waypoints(request.getWaypoints().toArray(new String[0]))
			                                       .optimizeWaypoints(true)
			                                       .await();

			if (result.routes == null || result.routes.length == 0) {
				throw new IllegalStateException("Nenhuma rota encontrada pela API do Google Maps.");
			}

			DirectionsRoute googleRoute = result.routes[0];

			BigDecimal totalCost = null;
			if (request.getDriverId() != null) {
				TrustedDriver driver = trustedDriverRepository.findById(request.getDriverId())
				                                              .orElseThrow(() -> new IllegalArgumentException("Entregador com o ID fornecido não foi encontrado."));

				if (!driver.getUser().getId().equals(user.getId())) {
					throw new SecurityException("Acesso negado. O entregador selecionado não pertence a este utilizador.");
				}

				totalCost = calculateCostForRoute(googleRoute, driver);
			}

			OptimizedRoute savedRoute = saveOptimizedRoute(googleRoute, user, totalCost);
			return buildResponseDTO(savedRoute, googleRoute);

		} catch (Exception e) {
			log.error("Falha crítica ao otimizar rota para o utilizador {}: {}", user.getEmail(), e.getMessage());
			throw new RuntimeException("Não foi possível otimizar a rota. Verifique os endereços e tente novamente.");
		}
	}

	private BigDecimal calculateCostForRoute(DirectionsRoute googleRoute, TrustedDriver driver) {
		long totalDistanceMeters = 0;
		for (DirectionsLeg leg : googleRoute.legs) {
			totalDistanceMeters += leg.distance.inMeters;
		}
		BigDecimal totalDistanceKm = BigDecimal.valueOf(totalDistanceMeters / 1000.0);

		log.info("Distância total da rota: {} km. A procurar regra de preço para o entregador '{}'.", totalDistanceKm, driver.getName());

		return driver.getPricingRules().stream()
		             .filter(rule -> totalDistanceKm.compareTo(rule.getMinDistanceKm()) >= 0 && totalDistanceKm.compareTo(rule.getMaxDistanceKm()) <= 0)
		             .findFirst()
		             .map(PricingRule::getPrice)
		             .orElse(null);
	}

	private OptimizedRoute saveOptimizedRoute(DirectionsRoute googleRoute, User user, BigDecimal totalCost) {
		OptimizedRoute route = new OptimizedRoute();
		route.setUser(user);

		long totalDistance = 0;
		long totalDuration = 0;
		for (DirectionsLeg leg : googleRoute.legs) {
			totalDistance += leg.distance.inMeters;
			totalDuration += leg.duration.inSeconds;
		}
		route.setTotalDistanceMeters((int) totalDistance);
		route.setTotalDurationSeconds((int) totalDuration);
		route.setTotalCost(totalCost);

		OptimizedRoute managedRoute = optimizedRouteRepository.save(route);

		for (int i = 0; i < googleRoute.waypointOrder.length; i++) {
			RouteStop stop = new RouteStop();
			stop.setRoute(managedRoute);
			stop.setAddressText(googleRoute.legs[i].endAddress);
			stop.setStopOrder(i + 1);
			managedRoute.getStops().add(routeStopRepository.save(stop));
		}
		return managedRoute;
	}

	private RouteOptimizationResponseDTO buildResponseDTO(OptimizedRoute savedRoute, DirectionsRoute googleRoute) {
		List<String> optimizedOrder = savedRoute.getStops().stream()
		                                        .sorted((s1, s2) -> Integer.compare(s1.getStopOrder(), s2.getStopOrder()))
		                                        .map(RouteStop::getAddressText)
		                                        .collect(Collectors.toList());

		String googleMapsUrl = buildGoogleMapsUrl(googleRoute.legs[0].startAddress, optimizedOrder);

		return RouteOptimizationResponseDTO.builder()
		                                   .routeId(savedRoute.getId())
		                                   .originAddress(googleRoute.legs[0].startAddress)
		                                   .optimizedStopOrder(optimizedOrder)
		                                   .totalDistance(formatDistance(savedRoute.getTotalDistanceMeters()))
		                                   .totalDuration(formatDuration(savedRoute.getTotalDurationSeconds()))
		                                   .googleMapsUrl(googleMapsUrl)
		                                   .totalCost(savedRoute.getTotalCost())
		                                   .build();
	}

	private String buildGoogleMapsUrl(String origin, List<String> waypoints) {
		String encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8);
		String encodedWaypoints = waypoints.stream()
		                                   .map(wp -> URLEncoder.encode(wp, StandardCharsets.UTF_8))
		                                   .collect(Collectors.joining("|"));

		return "https://www.google.com/maps/dir/?api=1&origin=" + encodedOrigin +
				"&destination=" + encodedOrigin +
				"&waypoints=" + encodedWaypoints;
	}

	private String formatDistance(int meters) {
		if (meters < 1000) {
			return meters + " m";
		}
		return String.format("%.1f km", meters / 1000.0);
	}

	private String formatDuration(int seconds) {
		long hours = TimeUnit.SECONDS.toHours(seconds);
		long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;

		if (hours > 0) {
			return String.format("%d h %d min", hours, minutes);
		}
		return String.format("%d min", minutes);
	}
}