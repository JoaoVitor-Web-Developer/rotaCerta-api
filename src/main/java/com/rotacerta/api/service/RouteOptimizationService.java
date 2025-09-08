package com.rotacerta.api.service;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;
import com.rotacerta.api.dto.RouteOptimizationRequestDTO;
import com.rotacerta.api.dto.RouteOptimizationResponseDTO;
import com.rotacerta.api.model.entities.OptimizedRoute;
import com.rotacerta.api.model.entities.RouteStop;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.OptimizedRouteRepository;
import com.rotacerta.api.repository.RouteStopRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private static final Logger log = LoggerFactory.getLogger(RouteOptimizationService.class);

	@Transactional
	public RouteOptimizationResponseDTO optimizeRoute(RouteOptimizationRequestDTO request, User user) {
		try {
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
			OptimizedRoute savedRoute = saveOptimizedRoute(googleRoute, user);
			return buildResponseDTO(savedRoute, googleRoute);

		} catch (Exception e) {
			log.error("Falha crítica ao otimizar rota para o usuário {}: {}", user.getEmail(), e.getMessage());
			throw new RuntimeException("Não foi possível otimizar a rota. Verifique se os endereços são válidos e tente novamente.");
		}
	}

	private OptimizedRoute saveOptimizedRoute(DirectionsRoute googleRoute, User user) {
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