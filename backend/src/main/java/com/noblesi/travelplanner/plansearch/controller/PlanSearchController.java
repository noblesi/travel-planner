package com.noblesi.travelplanner.plansearch.controller;

import java.util.OptionalLong;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import com.noblesi.travelplanner.service.PositiveIdParser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
public class PlanSearchController {

	private static final String VIEW_COOKIE_PREFIX = "plan_viewed_";
	private static final int VIEW_COOKIE_MAX_AGE_SECONDS = 24 * 60 * 60;

	private final PlanSearchService planSearchService;
	private final CurrentMemberProvider currentMemberProvider;
	private final SecurityMemberResolver securityMemberResolver;
	private final PositiveIdParser idParser;

	public PlanSearchController(
			PlanSearchService planSearchService,
			CurrentMemberProvider currentMemberProvider,
			SecurityMemberResolver securityMemberResolver,
			PositiveIdParser idParser
	) {
		this.planSearchService = planSearchService;
		this.currentMemberProvider = currentMemberProvider;
		this.securityMemberResolver = securityMemberResolver;
		this.idParser = idParser;
	}

	// 공개 플랜 목록 조회
	@GetMapping
	public ApiResponse<PageResponse<PlanListResponseDTO>> getPlanList(
			@RequestParam(defaultValue = "") String keyword,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) Integer limit
	) {
		PlanSearchRequestDTO request = new PlanSearchRequestDTO();
		request.setKeyword(keyword);
		request.setPage(page);
		request.setSize(limit == null ? size : limit);
		return ApiResponse.success(planSearchService.searchPlanList(request));
	}

	// 공개 플랜 상세 조회 (같은 브라우저에서 24시간 안에 다시 보면 조회수 증가 안 함)
	@GetMapping("/{planId}")
	public ApiResponse<PlanDetailResponseDTO> getPlanDetail(
			@PathVariable String planId,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		long parsedPlanId = idParser.parse(planId, "planId");
		if (!hasViewedRecently(httpRequest, parsedPlanId)) {
			planSearchService.increasePlanViewCount(parsedPlanId);
			markViewed(httpResponse, parsedPlanId);
		}
		OptionalLong memberId = securityMemberResolver.getAuthenticatedMemberId();
		Long memberIdOrNull = memberId.isPresent() ? memberId.getAsLong() : null;
		PlanDetailResponseDTO detail = planSearchService.searchPlanDetail(parsedPlanId, memberIdOrNull);
		return ApiResponse.success(detail);
	}

	private boolean hasViewedRecently(HttpServletRequest request, long planId) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return false;
		}
		String cookieName = VIEW_COOKIE_PREFIX + planId;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(cookieName)) {
				return true;
			}
		}
		return false;
	}

	private void markViewed(HttpServletResponse response, long planId) {
		Cookie cookie = new Cookie(VIEW_COOKIE_PREFIX + planId, "1");
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setMaxAge(VIEW_COOKIE_MAX_AGE_SECONDS);
		response.addCookie(cookie);
	}

	// 좋아요 토글 (이미 눌렀으면 취소, 안 눌렀으면 등록)
	@PostMapping("/{planId}/like")
	public ApiResponse<Boolean> toggleLike(@PathVariable String planId) {
		long parsedPlanId = idParser.parse(planId, "planId");
		long memberId = currentMemberProvider.getCurrentMemberId();
		return ApiResponse.success(planSearchService.toggleLike(memberId, parsedPlanId));
	}

	// 플랜 신고
	@PostMapping("/{planId}/report")
	public ApiResponse<Void> reportPlan(@PathVariable String planId, @Valid @RequestBody ReportRequestDTO request) {
		long parsedPlanId = idParser.parse(planId, "planId");
		long memberId = currentMemberProvider.getCurrentMemberId();
		// 식별자는 request body가 아닌 path variable만 신뢰해 서로 다른 planId가 전달될 여지를 제거한다.
		planSearchService.reportPlan(memberId, parsedPlanId, request);
		return ApiResponse.successWithoutData();
	}

	// 탐색 플랜을 내 플랜으로 복사
	@PostMapping("/{sourcePlanId}/copy")
	public ApiResponse<String> copyPlan(
			@PathVariable String sourcePlanId,
			@Valid @RequestBody PlanCopyRequestDTO request
	) {
		long parsedSourcePlanId = idParser.parse(sourcePlanId, "sourcePlanId");
		long memberId = currentMemberProvider.getCurrentMemberId();
		return ApiResponse.success(Long.toString(planSearchService.copyPlan(memberId, parsedSourcePlanId, request)));
	}
}
