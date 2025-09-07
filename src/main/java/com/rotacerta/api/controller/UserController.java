package com.rotacerta.api.controller;

import com.rotacerta.api.dto.ChangePasswordDTO;
import com.rotacerta.api.dto.UserProfileDTO;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.SubscriptionRepository;
import com.rotacerta.api.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserRepository userRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final PasswordEncoder passwordEncoder;

	@GetMapping("/me")
	public ResponseEntity<UserProfileDTO> getCurrentUserProfile(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		var subscription = subscriptionRepository.findByUser(user).orElse(null);
		return ResponseEntity.ok(UserProfileDTO.from(user, subscription));
	}

	@PutMapping("/me/password")
	public ResponseEntity<?> changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordDTO passwordDTO) {
		User user = (User) authentication.getPrincipal();

		if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
			return ResponseEntity.badRequest().body(Map.of("error", "Senha antiga incorreta."));
		}

		user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
		userRepository.save(user);

		return ResponseEntity.noContent().build();
	}

}
