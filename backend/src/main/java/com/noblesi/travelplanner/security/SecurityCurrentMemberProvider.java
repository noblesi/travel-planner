package com.noblesi.travelplanner.security;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.noblesi.travelplanner.common.exception.BusinessException;

@Component
@Profile("!local")
public class SecurityCurrentMemberProvider implements CurrentMemberProvider {

	private final SecurityMemberResolver securityMemberResolver;

	public SecurityCurrentMemberProvider(SecurityMemberResolver securityMemberResolver) {
		this.securityMemberResolver = securityMemberResolver;
	}

	@Override
	public long getCurrentMemberId() {
		return securityMemberResolver.getAuthenticatedMemberId()
				.orElseThrow(() -> new BusinessException(
						HttpStatus.UNAUTHORIZED,
						"CURRENT_MEMBER_NOT_AVAILABLE",
						"현재 로그인한 회원을 확인할 수 없습니다."
				));
	}
}
