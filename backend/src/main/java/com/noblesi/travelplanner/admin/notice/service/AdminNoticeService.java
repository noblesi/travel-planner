package com.noblesi.travelplanner.admin.notice.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.notice.mapper.AdminNoticeMapper;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeDetailDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeFormDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeListDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeSearchDTO;
import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.common.api.Pagination;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminNoticeService {
	private static final Set<String> CATEGORY_CODES = Set.of("GUIDE", "MAINTENANCE");

	@Autowired
	private AdminNoticeMapper adminNoticeMapper;

	/** 검색 조건에 맞는 전체 개수와 현재 페이지의 공지사항 목록을 조회합니다. */
	@Transactional(readOnly = true)
	public PageResponse<AdminNoticeListDTO> getNoticeList(AdminNoticeSearchDTO search) {
		long totalCount = adminNoticeMapper.countNoticeList(search);
		Pagination pagination = Pagination.of(search.page(), search.size(), totalCount);
		if (totalCount == 0) {
			return PageResponse.empty(pagination);
		}
		List<AdminNoticeListDTO> content = adminNoticeMapper.selectNoticeList(search);
		return PageResponse.of(content, pagination);
	}

	/** 공지사항 상세 정보를 조회하고 존재하지 않으면 404 업무 예외를 발생시킵니다. */
	@Transactional(readOnly = true)
	public AdminNoticeDetailDTO getNoticeDetail(Long noticeId) {
		AdminNoticeDetailDTO notice = adminNoticeMapper.selectNoticeDetail(noticeId);
		if (notice == null) {
			throw noticeNotFound();
		}
		return notice;
	}

	/** 상세 조회 결과를 등록·수정 화면에서 사용하는 Form DTO로 변환합니다. */
	@Transactional(readOnly = true)
	public AdminNoticeFormDTO getNoticeForm(Long noticeId) {
		AdminNoticeDetailDTO detail = getNoticeDetail(noticeId);
		AdminNoticeFormDTO form = new AdminNoticeFormDTO();
		form.setTitle(detail.getTitle());
		form.setContent(detail.getContent());
		form.setCategoryCode(detail.getCategoryCode());
		return form;
	}

	/** 입력값을 정리한 뒤 로그인 관리자 번호와 함께 공지사항을 등록합니다. */
	@Transactional
	public void createNotice(AdminNoticeFormDTO form, Long adminId) {
		normalizeForm(form);
		if (adminNoticeMapper.insertNotice(adminId, form) != 1) {
			throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
					"NOTICE_CREATE_FAILED", "공지사항 등록에 실패했습니다.");
		}
	}

	/** 입력값을 정리한 뒤 공지사항을 수정하고 수정 대상의 존재 여부를 확인합니다. */
	@Transactional
	public void updateNotice(Long noticeId, AdminNoticeFormDTO form) {
		normalizeForm(form);
		if (adminNoticeMapper.updateNotice(noticeId, form) != 1) {
			throw noticeNotFound();
		}
	}

	/** 공지사항을 실제 삭제하고 삭제된 행이 없으면 존재하지 않는 공지로 처리합니다. */
	@Transactional
	public void deleteNotice(Long noticeId) {
		if (adminNoticeMapper.deleteNotice(noticeId) != 1) {
			throw noticeNotFound();
		}
	}

	/** 제목·내용의 양쪽 공백을 제거하고 분류 코드를 DB 저장 형식으로 통일합니다. */
	private void normalizeForm(AdminNoticeFormDTO form) {
		form.setTitle(form.getTitle().strip());
		form.setContent(form.getContent().strip());
		form.setCategoryCode(normalizeCategoryCode(form.getCategoryCode()));
	}

	/** 화면에서 받은 분류 코드를 대문자로 변환하고 허용된 분류인지 검사합니다. */
	private String normalizeCategoryCode(String categoryCode) {
		if (categoryCode == null) {
			throw invalidCategoryCode();
		}
		String code = categoryCode.strip().toUpperCase();
		if (!CATEGORY_CODES.contains(code)) {
			throw invalidCategoryCode();
		}
		return code;
	}

	private BusinessException noticeNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다.");
	}

	private BusinessException invalidCategoryCode() {
		return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_NOTICE_CATEGORY",
				"올바르지 않은 공지사항 분류입니다.");
	}
}
