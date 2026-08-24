package com.noblesi.travelplanner.admin.member.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.member.mapper.AdminMemberMapper;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberDetailDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberListDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberSearchDTO;
import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.common.api.Pagination;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminMemberService {

	// MEMBER 테이블의 제약조건에서 허용하는 회원 상태입니다.
	private static final Set<String> MEMBER_STATUSES = Set.of("ACTIVE", "WITHDRAWN");

	// Spring이 MyBatis Mapper 구현 객체를 자동으로 주입합니다.
	@Autowired
	private AdminMemberMapper adminMemberMapper;

	/**
	 * 검색어와 회원 상태를 이용해 관리자 회원 목록을 조회합니다.
	 */
	public PageResponse<AdminMemberListDTO> getMemberList(AdminMemberSearchDTO search) {
		long totalCount = adminMemberMapper.countMemberList(search);
		Pagination pagination = Pagination.of(search.page(), search.size(), totalCount);
		if (totalCount == 0) {
			return PageResponse.empty(pagination);
		}
		return PageResponse.of(adminMemberMapper.selectMemberList(search), pagination);
	}

	/**
	 * 회원 번호에 해당하는 회원 상세 정보를 조회합니다.
	 */
	public AdminMemberDetailDTO getMemberDetail(Long memberId) {
		AdminMemberDetailDTO member = adminMemberMapper.selectMemberDetail(memberId);

		// 조회 결과가 없으면 존재하지 않는 회원이므로 404 예외를 발생시킵니다.
		if (member == null) {
			throw memberNotFound();
		}

		return member;
	}

	/**
	 * 회원 상태를 변경한 후 변경된 회원 상세 정보를 다시 조회합니다.
	 */
	@Transactional
	public AdminMemberDetailDTO removeMemberStatus(Long memberId, String memberStatus) {
		// 화면에서 받은 상태 값을 DB에 저장할 수 있는 값인지 검사합니다.
		String normalizedMemberStatus = normalizeMemberStatus(memberStatus);

		int updatedCount = adminMemberMapper.updateStatus(memberId, normalizedMemberStatus);

		// 수정된 행이 0개라면 해당 회원 번호가 존재하지 않는 것입니다.
		if (updatedCount == 0) {
			throw memberNotFound();
		}

		// DB 수정 후 최신 회원 정보를 반환합니다.
		return getMemberDetail(memberId);
	}

	/**
	 * 회원 상태의 공백과 대소문자를 정리하고 허용된 값인지 검사합니다.
	 */
	private String normalizeMemberStatus(String memberStatus) {
		if (memberStatus == null || memberStatus.isBlank()) {
			throw invalidMemberStatus();
		}

		String normalizedMemberStatus = memberStatus.strip().toUpperCase();

		if (!MEMBER_STATUSES.contains(normalizedMemberStatus)) {
			throw invalidMemberStatus();
		}

		return normalizedMemberStatus;
	}

	// 여러 메서드에서 사용하는 회원 미존재 예외를 한곳에서 생성합니다.
	private BusinessException memberNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다.");
	}

	// 올바르지 않은 상태 값이 전달됐을 때 사용할 예외를 생성합니다.
	private BusinessException invalidMemberStatus() {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_MEMBER_STATUS", "올바르지 않은 회원 상태입니다.");
	}
}
