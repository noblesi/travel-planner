package com.noblesi.travelplanner.plansearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportRequestDTO {

	// 신고 사유는 운영 화면이 처리할 수 있는 코드로 제한해 임의 문자열이 DB에 누적되는 것을 막는다.
	@NotBlank(message = "신고 사유는 필수 값입니다.")
	@Pattern(
			regexp = "INAPPROPRIATE|FALSE_INFO|SPAM|OTHER",
			message = "지원하지 않는 신고 사유입니다."
	)
	private String reason;

	// 운영 DDL의 REASON_DETAIL VARCHAR2(1000)와 동일한 길이로 API 입력을 제한한다.
	@Size(max = 1000, message = "신고 상세 내용은 1000자 이하여야 합니다.")
	private String detail;
}
