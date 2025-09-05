package com.rotacerta.api.service;

import com.rotacerta.api.dto.AuthResponseDTO;
import com.rotacerta.api.dto.LoginRequestDTO;
import com.rotacerta.api.dto.RegisterRequestDTO;
import com.rotacerta.api.model.entities.User;
import com.rotacerta.api.repository.UserRepository;
import com.rotacerta.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthResponseDTO register(RegisterRequestDTO request) {
		var user = new User();
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setCompanyName(request.getCompanyName());
		user.setRole(User.Role.USER);

		userRepository.save(user);

		var jwtToken = jwtService.generateToken(user);
		return AuthResponseDTO.builder()
		                      .token(jwtToken)
		                      .build();
	}

	public AuthResponseDTO login(LoginRequestDTO request) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(
						request.getEmail(),
						request.getPassword()
				)
		                                  );

		var user = userRepository.findByEmail(request.getEmail())
		                         .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

		var jwtToken = jwtService.generateToken(user);
		return AuthResponseDTO.builder()
		                      .token(jwtToken)
		                      .build();
	}
}
