package com.noblesi.travelplanner.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetRecoveredPasswordRequest(
		@NotBlank(message = "새 비밀번호를 입력해 주세요.")
		@Size(min = 10, max = 72, message = "새 비밀번호는 10자 이상 72자 이하로 입력해 주세요.")
		String newPassword
) {
}
