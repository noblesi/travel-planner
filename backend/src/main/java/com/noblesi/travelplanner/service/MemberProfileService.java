package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.member.MemberProfile;
import com.noblesi.travelplanner.dto.member.ChangeMemberPasswordRequest;
import com.noblesi.travelplanner.dto.member.MemberProfileResponse;
import com.noblesi.travelplanner.dto.member.UpdateMemberProfileRequest;
import com.noblesi.travelplanner.dto.member.WithdrawMemberRequest;
import com.noblesi.travelplanner.mapper.MemberProfileMapper;
import com.noblesi.travelplanner.security.CurrentMemberProvider;

@Service
public class MemberProfileService {

	private final MemberProfileMapper memberProfileMapper;
	private final CurrentMemberProvider currentMemberProvider;
	private final PasswordEncoder passwordEncoder;
	private final ProfileImageStorage profileImageStorage;

	public MemberProfileService(
			MemberProfileMapper memberProfileMapper,
			CurrentMemberProvider currentMemberProvider,
			PasswordEncoder passwordEncoder,
			ProfileImageStorage profileImageStorage
	) {
		this.memberProfileMapper = memberProfileMapper;
		this.currentMemberProvider = currentMemberProvider;
		this.passwordEncoder = passwordEncoder;
		this.profileImageStorage = profileImageStorage;
	}

	@Transactional(readOnly = true)
	public MemberProfileResponse getMyProfile() {
		long memberId = currentMemberProvider.getCurrentMemberId();
		return findActiveProfile(memberId);
	}

	@Transactional
	public MemberProfileResponse updateMyProfile(UpdateMemberProfileRequest request) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		if (memberProfileMapper.updateActiveProfile(memberId, request) != 1) {
			throw profileNotFound();
		}
		return findActiveProfile(memberId);
	}

	@Transactional
	public void withdrawMyAccount(WithdrawMemberRequest request) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		String passwordHash = memberProfileMapper.findActivePasswordHashByMemberId(memberId)
				.orElseThrow(this::profileNotFound);
		if (!passwordEncoder.matches(request.currentPassword(), passwordHash)) {
			throw invalidCurrentPassword();
		}
		if (memberProfileMapper.withdrawActiveMember(memberId) != 1) {
			throw profileNotFound();
		}
	}

	@Transactional
	public void changeMyPassword(ChangeMemberPasswordRequest request) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		String passwordHash = memberProfileMapper.findActivePasswordHashByMemberId(memberId)
				.orElseThrow(this::profileNotFound);
		if (!passwordEncoder.matches(request.currentPassword(), passwordHash)) {
			throw invalidCurrentPassword();
		}
		if (passwordEncoder.matches(request.newPassword(), passwordHash)) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"PASSWORD_UNCHANGED",
					"새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요."
			);
		}
		if (memberProfileMapper.updateActivePassword(
				memberId,
				passwordEncoder.encode(request.newPassword())
		) != 1) {
			throw profileNotFound();
		}
	}

	@Transactional
	public MemberProfileResponse updateProfileImage(MultipartFile file) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		MemberProfile currentProfile = memberProfileMapper.findActiveProfileByMemberId(memberId)
				.orElseThrow(this::profileNotFound);
		String newImageUrl = profileImageStorage.store(file);
		try {
			if (memberProfileMapper.updateActiveProfileImage(memberId, newImageUrl) != 1) {
				throw profileNotFound();
			}
		} catch (RuntimeException exception) {
			profileImageStorage.delete(newImageUrl);
			throw exception;
		}
		profileImageStorage.delete(currentProfile.profileImageUrl());
		return findActiveProfile(memberId);
	}

	private MemberProfileResponse findActiveProfile(long memberId) {
		MemberProfile profile = memberProfileMapper.findActiveProfileByMemberId(memberId)
				.orElseThrow(this::profileNotFound);
		return MemberProfileResponse.from(profile);
	}

	private BusinessException profileNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND,
				"MEMBER_PROFILE_NOT_FOUND",
				"회원 정보를 찾을 수 없습니다."
		);
	}

	private BusinessException invalidCurrentPassword() {
		return new BusinessException(
				HttpStatus.UNAUTHORIZED,
				"INVALID_CURRENT_PASSWORD",
				"현재 비밀번호가 올바르지 않습니다."
		);
	}
}
