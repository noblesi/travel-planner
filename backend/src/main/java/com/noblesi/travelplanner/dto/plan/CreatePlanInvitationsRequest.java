package com.noblesi.travelplanner.dto.plan;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreatePlanInvitationsRequest(
		@NotEmpty(message = "초대할 이메일을 한 개 이상 입력해 주세요.")
		@Size(max = 20, message = "초대는 한 번에 최대 20명까지 생성할 수 있습니다.")
		@Valid
		List<
				@NotBlank(message = "초대 이메일은 필수 값입니다.")
				@Email(message = "초대 이메일 형식이 올바르지 않습니다.")
				@Size(max = 254, message = "초대 이메일은 254자 이하여야 합니다.")
				String
		> inviteeEmails
) {

	public CreatePlanInvitationsRequest {
		inviteeEmails = inviteeEmails == null
				? null
				: inviteeEmails.stream()
						.map(email -> email == null ? null : email.strip())
						.toList();
	}
}
