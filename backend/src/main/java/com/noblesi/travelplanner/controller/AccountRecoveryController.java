package com.noblesi.travelplanner.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.account.FindEmailRequest;
import com.noblesi.travelplanner.dto.account.ResetRecoveredPasswordRequest;
import com.noblesi.travelplanner.dto.account.VerifyPasswordRecoveryRequest;
import com.noblesi.travelplanner.service.AccountRecoveryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account-recovery")
public class AccountRecoveryController {

	private final AccountRecoveryService accountRecoveryService;

	public AccountRecoveryController(AccountRecoveryService accountRecoveryService) {
		this.accountRecoveryService = accountRecoveryService;
	}

	@PostMapping("/email")
	public ApiResponse<String> findEmail(@Valid @RequestBody FindEmailRequest request) {
		return ApiResponse.success(accountRecoveryService.findMaskedEmail(request));
	}

	@PostMapping("/password/verify")
	public ApiResponse<Void> verifyPasswordRecovery(
			@Valid @RequestBody VerifyPasswordRecoveryRequest request,
			HttpServletRequest httpRequest
	) {
		HttpSession session = httpRequest.getSession(true);
		accountRecoveryService.verifyPasswordRecovery(request, session);
		return ApiResponse.successWithoutData();
	}

	@PatchMapping("/password")
	public ApiResponse<Void> resetPassword(
			@Valid @RequestBody ResetRecoveredPasswordRequest request,
			HttpSession session
	) {
		accountRecoveryService.resetPassword(request, session);
		return ApiResponse.successWithoutData();
	}
}
