package com.noblesi.travelplanner.admin.notice.service;

import java.util.List;
import java.util.Set;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminNoticeService {
	private static final Set<String> CATEGORY_CODES = Set.of("GUIDE", "MAINTENANCE");
	private final AdminNoticeMapper adminNoticeMapper;

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

	@Transactional(readOnly = true)
	public AdminNoticeDetailDTO getNoticeDetail(Long noticeId) {
		AdminNoticeDetailDTO notice = adminNoticeMapper.selectNoticeDetail(noticeId);
		if (notice == null) {
			throw noticeNotFound();
		}
		return notice;
	}

	@Transactional(readOnly = true)
	public AdminNoticeFormDTO getNoticeForm(Long noticeId) {
		AdminNoticeDetailDTO detail = getNoticeDetail(noticeId);
		AdminNoticeFormDTO form = new AdminNoticeFormDTO();
		form.setTitle(detail.getTitle());
		form.setContent(detail.getContent());
		form.setCategoryCode(detail.getCategoryCode());
		return form;
	}

	@Transactional
	public void createNotice(AdminNoticeFormDTO form, Long adminId) {
		normalizeForm(form);
		if (adminNoticeMapper.insertNotice(adminId, form) != 1) {
			throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
					"NOTICE_CREATE_FAILED", "공지사항 등록에 실패했습니다.");
		}
	}

	@Transactional
	public void updateNotice(Long noticeId, AdminNoticeFormDTO form) {
		normalizeForm(form);
		if (adminNoticeMapper.updateNotice(noticeId, form) != 1) {
			throw noticeNotFound();
		}
	}

	@Transactional
	public void deleteNotice(Long noticeId) {
		if (adminNoticeMapper.deleteNotice(noticeId) != 1) {
			throw noticeNotFound();
		}
	}

	private void normalizeForm(AdminNoticeFormDTO form) {
		form.setTitle(form.getTitle().strip());
		form.setContent(form.getContent().strip());
		form.setCategoryCode(normalizeCategoryCode(form.getCategoryCode()));
	}

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
