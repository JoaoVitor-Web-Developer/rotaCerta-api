package com.rotacerta.api.controller;

import com.rotacerta.api.dto.CreateDriverDTO;
import com.rotacerta.api.dto.DriverResponseDTO;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.service.TrustedDriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class TrustedDriverController {
	private final TrustedDriverService trustedDriverService;

	@GetMapping
	public ResponseEntity<List<DriverResponseDTO>> getAllDrivers(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		List<DriverResponseDTO> drivers = trustedDriverService.getAllDriversForUser(user);
		return ResponseEntity.ok(drivers);
	}

	@PostMapping
	public ResponseEntity<DriverResponseDTO> createDriver(
			@Valid @RequestBody CreateDriverDTO request,
			Authentication authentication
	                                                     ) {
		User user = (User) authentication.getPrincipal();
		DriverResponseDTO createdDriver = trustedDriverService.createDriver(request, user);
		return new ResponseEntity<>(createdDriver, HttpStatus.CREATED);
	}

	@PutMapping("/{driverId}")
	public ResponseEntity<DriverResponseDTO> updateDriver(
			@PathVariable UUID driverId,
			@Valid @RequestBody CreateDriverDTO request,
			Authentication authentication
	                                                     ) {
		User user = (User) authentication.getPrincipal();
		DriverResponseDTO updatedDriver = trustedDriverService.updateDriver(driverId, request, user);
		return ResponseEntity.ok(updatedDriver);
	}

	@DeleteMapping("/{driverId}")
	public ResponseEntity<Void> deleteDriver(
			@PathVariable UUID driverId,
			Authentication authentication
	                                        ) {
		User user = (User) authentication.getPrincipal();
		trustedDriverService.deleteDriver(driverId, user);
		return ResponseEntity.noContent().build();
	}
}
