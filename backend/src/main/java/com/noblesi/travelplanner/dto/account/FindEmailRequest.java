package com.noblesi.travelplanner.dto.account;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FindEmailRequest(
		@NotBlank(message = "이름을 입력해 주세요.")
		@Size(max = 10, message = "이름은 10자 이하로 입력해 주세요.")
		String memberName,

		@NotNull(message = "생년월일을 입력해 주세요.")
		@Past(message = "생년월일은 오늘보다 이전이어야 합니다.")
		LocalDate birthDate,

		@NotBlank(message = "전화번호를 입력해 주세요.")
		@Size(max = 20, message = "전화번호는 20자 이하로 입력해 주세요.")
		@Pattern(regexp = "\\d{2,3}-?\\d{3,4}-?\\d{4}", message = "전화번호 형식이 올바르지 않습니다.")
		String phoneNumber
) {
	public FindEmailRequest {
		if (memberName != null) {
			memberName = memberName.strip();
		}
		if (phoneNumber != null) {
			phoneNumber = phoneNumber.strip();
		}
	}
}
