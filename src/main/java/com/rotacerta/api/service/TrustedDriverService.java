package com.rotacerta.api.service;

import com.rotacerta.api.dto.CreateDriverDTO;
import com.rotacerta.api.dto.DriverResponseDTO;
import com.rotacerta.api.model.entities.PricingRule;
import com.rotacerta.api.model.entities.TrustedDriver;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.TrustedDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrustedDriverService {

	private final TrustedDriverRepository trustedDriverRepository;

	@Transactional(readOnly = true)
	public List<DriverResponseDTO> getAllDriversForUser(User user) {
		return trustedDriverRepository.findByUser(user).stream()
		                              .map(DriverResponseDTO::fromEntity)
		                              .collect(Collectors.toList());
	}

	@Transactional
	public DriverResponseDTO createDriver(CreateDriverDTO dto, User user) {
		TrustedDriver driver = new TrustedDriver();
		driver.setUser(user);
		driver.setName(dto.getName());
		driver.setPhone(dto.getPhone());
		driver.setVehicleType(dto.getVehicleType());

		List<PricingRule> rules = dto.getPricingRules().stream().map(ruleDto -> {
			PricingRule rule = new PricingRule();
			rule.setDriver(driver);
			rule.setMinDistanceKm(ruleDto.getMinDistanceKm());
			rule.setMaxDistanceKm(ruleDto.getMaxDistanceKm());
			rule.setPrice(ruleDto.getPrice());
			return rule;
		}).collect(Collectors.toList());

		driver.setPricingRules(rules);

		TrustedDriver savedDriver = trustedDriverRepository.save(driver);
		return DriverResponseDTO.fromEntity(savedDriver);
	}

	@Transactional
	public DriverResponseDTO updateDriver(UUID driverId, CreateDriverDTO dto, User user) {
		TrustedDriver driver = trustedDriverRepository.findById(driverId)
		                                              .orElseThrow(() -> new RuntimeException("Entregador não encontrado."));

		if (!driver.getUser().getId().equals(user.getId())) {
			throw new SecurityException("Acesso negado.");
		}

		driver.setName(dto.getName());
		driver.setPhone(dto.getPhone());
		driver.setVehicleType(dto.getVehicleType());

		driver.getPricingRules().clear();
		dto.getPricingRules().forEach(ruleDto -> {
			PricingRule rule = new PricingRule();
			rule.setDriver(driver);
			rule.setMinDistanceKm(ruleDto.getMinDistanceKm());
			rule.setMaxDistanceKm(ruleDto.getMaxDistanceKm());
			rule.setPrice(ruleDto.getPrice());
			driver.getPricingRules().add(rule);
		});

		TrustedDriver updatedDriver = trustedDriverRepository.save(driver);
		return DriverResponseDTO.fromEntity(updatedDriver);
	}

	@Transactional
	public void deleteDriver(UUID driverId, User user) {
		TrustedDriver driver = trustedDriverRepository.findById(driverId)
		                                              .orElseThrow(() -> new RuntimeException("Entregador não encontrado."));

		if (!driver.getUser().getId().equals(user.getId())) {
			throw new SecurityException("Acesso negado.");
		}

		trustedDriverRepository.delete(driver);
	}
}
