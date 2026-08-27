package com.noblesi.travelplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
		MemberProfile currentProfile = memberProfileMapper.findActiveProfileByMemberIdForUpdate(memberId)
				.orElseThrow(this::profileNotFound);
		String passwordHash = memberProfileMapper.findActivePasswordHashByMemberId(memberId)
				.orElseThrow(this::profileNotFound);
		if (!passwordEncoder.matches(request.currentPassword(), passwordHash)) {
			throw invalidCurrentPassword();
		}
		if (memberProfileMapper.withdrawActiveMember(memberId) != 1) {
			throw profileNotFound();
		}
		deleteAfterCommit(currentProfile.profileImageUrl());
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
		MemberProfile currentProfile = memberProfileMapper.findActiveProfileByMemberIdForUpdate(memberId)
				.orElseThrow(this::profileNotFound);
		String newImageUrl = profileImageStorage.store(file);
		try {
			registerImageReplacementCleanup(newImageUrl, currentProfile.profileImageUrl());
		} catch (RuntimeException exception) {
			profileImageStorage.delete(newImageUrl);
			throw exception;
		}
		if (memberProfileMapper.updateActiveProfileImage(memberId, newImageUrl) != 1) {
			throw profileNotFound();
		}
		return findActiveProfile(memberId);
	}

	private void registerImageReplacementCleanup(String newImageUrl, String previousImageUrl) {
		requireTransactionSynchronization();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				profileImageStorage.delete(previousImageUrl);
			}

			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					profileImageStorage.delete(newImageUrl);
				}
			}
		});
	}

	private void deleteAfterCommit(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return;
		}
		requireTransactionSynchronization();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				profileImageStorage.delete(imageUrl);
			}
		});
	}

	private void requireTransactionSynchronization() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("Profile image changes require an active transaction");
		}
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
