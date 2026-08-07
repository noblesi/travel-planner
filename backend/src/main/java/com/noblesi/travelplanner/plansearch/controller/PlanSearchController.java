package com.noblesi.travelplanner.plansearch.controller;

import java.util.OptionalLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.common.api.PageResponse;
import com.noblesi.travelplanner.plansearch.dto.PlanCopyRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanDetailResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanListResponseDTO;
import com.noblesi.travelplanner.plansearch.dto.PlanSearchRequestDTO;
import com.noblesi.travelplanner.plansearch.dto.ReportRequestDTO;
import com.noblesi.travelplanner.plansearch.service.PlanSearchService;
import com.noblesi.travelplanner.security.CurrentMemberProvider;
import com.noblesi.travelplanner.security.SecurityMemberResolver;

@RestController
@RequestMapping("/api/plan-search")
public class PlanSearchController {

	private final PlanSearchService planSearchService;
	private final CurrentMemberProvider currentMemberProvider;
	private final SecurityMemberResolver securityMemberResolver;

	public PlanSearchController(
			PlanSearchService planSearchService,
			CurrentMemberProvider currentMemberProvider,
			SecurityMemberResolver securityMemberResolver
	) {
		this.planSearchService = planSearchService;
		this.currentMemberProvider = currentMemberProvider;
		this.securityMemberResolver = securityMemberResolver;
	}

	// 공개 플랜 목록 조회
	@GetMapping("/plans")
	public ApiResponse<PageResponse<PlanListResponseDTO>> getPlanList(
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size
	) {
		PlanSearchRequestDTO request = new PlanSearchRequestDTO();
		request.setKeyword(keyword);
		request.setPage(page);
		request.setSize(size);
		return ApiResponse.success(planSearchService.searchPlanList(request));
	}

	// 공개 플랜 상세 조회 (조회수 증가 포함)
	@GetMapping("/plans/{planId}")
	public ApiResponse<PlanDetailResponseDTO> getPlanDetail(@PathVariable Long planId) {
		planSearchService.increasePlanViewCount(planId);
		OptionalLong memberId = securityMemberResolver.getAuthenticatedMemberId();
		Long memberIdOrNull = memberId.isPresent() ? memberId.getAsLong() : null;
		return ApiResponse.success(planSearchService.searchPlanDetail(planId, memberIdOrNull));
	}

	// 좋아요 토글 (이미 눌렀으면 취소, 안 눌렀으면 등록)
	@PostMapping("/plans/{planId}/like")
	public ApiResponse<Boolean> toggleLike(@PathVariable Long planId) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		return ApiResponse.success(planSearchService.toggleLike(memberId, planId));
	}

	// 플랜 신고
	@PostMapping("/plans/{planId}/report")
	public ApiResponse<Void> reportPlan(@PathVariable Long planId, @RequestBody ReportRequestDTO request) {
		request.setPlanId(planId);
		long memberId = currentMemberProvider.getCurrentMemberId();
		planSearchService.reportPlan(memberId, request);
		return ApiResponse.successWithoutData();
	}

	// 탐색 플랜을 내 플랜으로 복사
	@PostMapping("/plans/{sourcePlanId}/copy")
	public ApiResponse<Long> copyPlan(@PathVariable Long sourcePlanId, @RequestBody PlanCopyRequestDTO request) {
		long memberId = currentMemberProvider.getCurrentMemberId();
		return ApiResponse.success(planSearchService.copyPlan(memberId, sourcePlanId, request));
	}
}
