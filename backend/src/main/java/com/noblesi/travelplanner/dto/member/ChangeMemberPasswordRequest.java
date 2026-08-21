package com.noblesi.travelplanner.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeMemberPasswordRequest(
		@NotBlank(message = "현재 비밀번호를 입력해 주세요.")
		@Size(max = 72, message = "현재 비밀번호를 확인해 주세요.")
		String currentPassword,

		@NotBlank(message = "새 비밀번호를 입력해 주세요.")
		@Size(min = 10, max = 72, message = "새 비밀번호는 10자 이상 72자 이하로 입력해 주세요.")
		String newPassword
) {
}
