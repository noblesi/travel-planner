package com.noblesi.travelplanner.notice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.notice.dto.NoticeDetailResponseDTO;
import com.noblesi.travelplanner.notice.dto.NoticeListResponseDTO;
import com.noblesi.travelplanner.notice.dto.NoticeSearchRequestDTO;
import com.noblesi.travelplanner.notice.service.NoticeService;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

	private final NoticeService noticeService;

	public NoticeController(NoticeService noticeService) {
		this.noticeService = noticeService;
	}

	// 공지사항 목록 조회 (카테고리/페이지/사이즈를 쿼리 파라미터로 받음)
	@GetMapping
	public ApiResponse<PageResponse<NoticeListResponseDTO>> getNoticeList(
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		NoticeSearchRequestDTO request = new NoticeSearchRequestDTO(category, page, size);
		return ApiResponse.success(noticeService.getNoticeList(request));
	}

	// 공지사항 상세 조회
	@GetMapping("/{noticeId}")
	public ApiResponse<NoticeDetailResponseDTO> getNoticeDetail(@PathVariable Long noticeId) {
		return ApiResponse.success(noticeService.getNoticeDetail(noticeId));
	}
}
