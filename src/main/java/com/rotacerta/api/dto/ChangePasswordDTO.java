package com.rotacerta.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ChangePasswordDTO {
	@NotEmpty
	private String oldPassword;
	@NotEmpty
	private String newPassword;
}