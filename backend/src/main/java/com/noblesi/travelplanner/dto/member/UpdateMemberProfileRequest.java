package com.noblesi.travelplanner.dto.member;

import java.time.LocalDate;
import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberProfileRequest(
		@NotBlank(message = "이름을 입력해 주세요.")
		@Size(max = 10, message = "이름은 10자 이하로 입력해 주세요.")
		String name,

		@NotBlank(message = "닉네임을 입력해 주세요.")
		@Size(max = 50, message = "닉네임은 50자 이하로 입력해 주세요.")
		String nickname,

		@NotBlank(message = "성별을 선택해 주세요.")
		@Pattern(regexp = "M|F|N", message = "성별 코드가 올바르지 않습니다.")
		String genderCode,

		@Past(message = "생년월일은 오늘보다 이전이어야 합니다.")
		LocalDate birthDate,

		@Size(max = 20, message = "전화번호는 20자 이하로 입력해 주세요.")
		@Pattern(
				regexp = "\\d{2,3}-?\\d{3,4}-?\\d{4}",
				message = "전화번호 형식이 올바르지 않습니다."
		)
		String phoneNumber
) {

	public UpdateMemberProfileRequest {
		if (name != null) {
			name = name.strip();
		}
		if (nickname != null) {
			nickname = nickname.strip();
		}
		if (genderCode != null) {
			genderCode = genderCode.strip().toUpperCase(Locale.ROOT);
		}
		if (phoneNumber != null) {
			phoneNumber = phoneNumber.strip();
			if (phoneNumber.isBlank()) {
				phoneNumber = null;
			}
		}
	}
}
