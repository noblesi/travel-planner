package com.noblesi.travelplanner.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
		@NotBlank @Size(max = 100) String loginId,
		@NotBlank @Size(max = 200) String password
) {
}
