package com.noblesi.travelplanner.security;

/**
 * 로그인 수단과 무관하게 현재 인증된 서비스 회원 ID를 제공합니다.
 *
 * <p>구현체는 현재 회원을 확인할 수 없을 때
 * {@code CURRENT_MEMBER_NOT_AVAILABLE} BusinessException을 발생시켜야 합니다.</p>
 */
public interface CurrentMemberProvider {

	long getCurrentMemberId();
}
