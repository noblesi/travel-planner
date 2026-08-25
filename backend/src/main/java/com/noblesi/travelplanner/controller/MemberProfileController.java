package com.noblesi.travelplanner.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.member.ChangeMemberPasswordRequest;
import com.noblesi.travelplanner.dto.member.MemberProfileResponse;
import com.noblesi.travelplanner.dto.member.UpdateMemberProfileRequest;
import com.noblesi.travelplanner.dto.member.WithdrawMemberRequest;
import com.noblesi.travelplanner.service.MemberProfileService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/members/me")
public class MemberProfileController {

	private final MemberProfileService memberProfileService;

	public MemberProfileController(MemberProfileService memberProfileService) {
		this.memberProfileService = memberProfileService;
	}

	@GetMapping
	public ApiResponse<MemberProfileResponse> getMyProfile() {
		return ApiResponse.success(memberProfileService.getMyProfile());
	}

	@PatchMapping
	public ApiResponse<MemberProfileResponse> updateMyProfile(
			@Valid @RequestBody UpdateMemberProfileRequest request
	) {
		return ApiResponse.success(memberProfileService.updateMyProfile(request));
	}

	@DeleteMapping
	public ApiResponse<Void> withdrawMyAccount(
			@Valid @RequestBody WithdrawMemberRequest request,
			Authentication authentication,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse
	) {
		memberProfileService.withdrawMyAccount(request);
		new SecurityContextLogoutHandler().logout(servletRequest, servletResponse, authentication);
		return ApiResponse.successWithoutData();
	}

	@PatchMapping("/password")
	public ApiResponse<Void> changeMyPassword(
			@Valid @RequestBody ChangeMemberPasswordRequest request
	) {
		memberProfileService.changeMyPassword(request);
		return ApiResponse.successWithoutData();
	}
}
