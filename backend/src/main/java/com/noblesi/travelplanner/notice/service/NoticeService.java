package com.noblesi.travelplanner.notice.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.common.api.Pagination;
import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.notice.dao.NoticeDAO;
import com.noblesi.travelplanner.notice.dto.NoticeDetailResponseDTO;
import com.noblesi.travelplanner.notice.dto.NoticeListResponseDTO;
import com.noblesi.travelplanner.notice.dto.NoticeSearchRequestDTO;

@Service
public class NoticeService {

	private final NoticeDAO nd;

	public NoticeService(NoticeDAO nd) {
		this.nd = nd;
	}

	// 공지사항 목록 조회 (전체 개수로 페이징 정보 계산 후, 있으면 목록까지 조회)
	@Transactional(readOnly = true)
	public PageResponse<NoticeListResponseDTO> getNoticeList(NoticeSearchRequestDTO request) {
		long totalCount = nd.countNoticeList(request.getCategory());
		Pagination pagination = Pagination.of(request.getPage(), request.getSize(), totalCount);
		if (totalCount == 0) {
			return PageResponse.empty(pagination);
		}
		List<NoticeListResponseDTO> content = nd.selectNoticeList(request);
		return PageResponse.of(content, pagination);
	}

	// 공지사항 상세 조회 (조회수 먼저 증가시키고, 증가된 수치로 상세 반환)
	@Transactional
	public NoticeDetailResponseDTO getNoticeDetail(Long noticeId) {
		increaseNoticeViewCount(noticeId);
		NoticeDetailResponseDTO notice = nd.selectNoticeById(noticeId);
		if (notice == null) {
			throw noticeNotFound();
		}
		return notice;
	}

	// 공지사항 조회수 증가 (대상이 없으면 404)
	private void increaseNoticeViewCount(Long noticeId) {
		if (nd.updateNoticeViewCount(noticeId) != 1) {
			throw noticeNotFound();
		}
	}

	private BusinessException noticeNotFound() {
		return new BusinessException(HttpStatus.NOT_FOUND, "NOTICE_NOT_FOUND", "공지사항을 찾을 수 없습니다.");
	}
}
