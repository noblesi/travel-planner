package com.noblesi.travelplanner.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.account.PasswordRecoveryGrant;
import com.noblesi.travelplanner.dto.account.FindEmailRequest;
import com.noblesi.travelplanner.dto.account.ResetRecoveredPasswordRequest;
import com.noblesi.travelplanner.dto.account.VerifyPasswordRecoveryRequest;
import com.noblesi.travelplanner.mapper.AccountRecoveryMapper;

import jakarta.servlet.http.HttpSession;

@Service
public class AccountRecoveryService {

	private static final String PASSWORD_RECOVERY_GRANT =
			AccountRecoveryService.class.getName() + ".PASSWORD_RECOVERY_GRANT";
	private static final Duration GRANT_TTL = Duration.ofMinutes(10);

	private final AccountRecoveryMapper accountRecoveryMapper;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public AccountRecoveryService(
			AccountRecoveryMapper accountRecoveryMapper,
			PasswordEncoder passwordEncoder,
			Clock clock
	) {
		this.accountRecoveryMapper = accountRecoveryMapper;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public String findMaskedEmail(FindEmailRequest request) {
		String email = accountRecoveryMapper.findActiveEmail(request)
				.orElseThrow(this::recoveryInformationNotFound);
		return maskEmail(email);
	}

	@Transactional(readOnly = true)
	public void verifyPasswordRecovery(
			VerifyPasswordRecoveryRequest request,
			HttpSession session
	) {
		long memberId = accountRecoveryMapper.findActiveMemberId(request)
				.orElseThrow(this::recoveryInformationNotFound);
		Instant expiresAt = clock.instant().plus(GRANT_TTL);
		session.setAttribute(PASSWORD_RECOVERY_GRANT, new PasswordRecoveryGrant(memberId, expiresAt));
	}

	@Transactional
	public void resetPassword(ResetRecoveredPasswordRequest request, HttpSession session) {
		PasswordRecoveryGrant grant = getValidGrant(session);
		String currentPasswordHash = accountRecoveryMapper.findActivePasswordHash(grant.memberId())
				.orElseThrow(this::recoveryInformationNotFound);
		if (passwordEncoder.matches(request.newPassword(), currentPasswordHash)) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"PASSWORD_UNCHANGED",
					"새 비밀번호는 기존 비밀번호와 다르게 입력해 주세요."
			);
		}
		if (accountRecoveryMapper.updateActivePassword(
				grant.memberId(),
				passwordEncoder.encode(request.newPassword())
		) != 1) {
			session.removeAttribute(PASSWORD_RECOVERY_GRANT);
			throw recoveryInformationNotFound();
		}
		session.removeAttribute(PASSWORD_RECOVERY_GRANT);
	}

	private PasswordRecoveryGrant getValidGrant(HttpSession session) {
		Object attribute = session.getAttribute(PASSWORD_RECOVERY_GRANT);
		if (attribute instanceof PasswordRecoveryGrant grant
				&& grant.expiresAt().isAfter(clock.instant())) {
			return grant;
		}
		session.removeAttribute(PASSWORD_RECOVERY_GRANT);
		throw new BusinessException(
				HttpStatus.FORBIDDEN,
				"PASSWORD_RECOVERY_REQUIRED",
				"회원정보 확인이 만료되었습니다. 다시 확인해 주세요."
		);
	}

	private String maskEmail(String email) {
		int separator = email.indexOf('@');
		if (separator <= 0) {
			return "***";
		}
		String localPart = email.substring(0, separator);
		String visiblePrefix = localPart.substring(0, 1);
		return visiblePrefix + "***" + email.substring(separator);
	}

	private BusinessException recoveryInformationNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND,
				"ACCOUNT_RECOVERY_NOT_FOUND",
				"입력한 정보와 일치하는 활성 회원을 찾을 수 없습니다."
		);
	}
}
