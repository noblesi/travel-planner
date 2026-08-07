package com.noblesi.travelplanner.admin.trip.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.noblesi.travelplanner.admin.trip.dto.AdminTripDetailDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripListDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripReportDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripScheduleDTO;
import com.noblesi.travelplanner.admin.trip.service.AdminTripService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/trips")
public class AdminTripController {
	private static final int PAGE_SIZE = 10;

	private final AdminTripService adminTripService;

	@Value("${KAKAO_JAVASCRIPT_KEY:}")
	private String kakaoJavascriptKey;

	@GetMapping
	public String getTripList(
			@RequestParam(name = "keyword", defaultValue = "") String keyword,
			@RequestParam(name = "visibility", defaultValue = "") String visibility,
			@RequestParam(name = "regionCode", defaultValue = "") String regionCode,
			@RequestParam(name = "reportedOnly", defaultValue = "false") boolean reportedOnly,
			@RequestParam(name = "page", defaultValue = "1") int page,
			Model model
	) {
		List<AdminTripListDTO> allTrips = adminTripService.getTripList(keyword, visibility, regionCode, reportedOnly);
		int totalPages = Math.max(1, (int) Math.ceil((double) allTrips.size() / PAGE_SIZE));
		int currentPage = Math.min(Math.max(page, 1), totalPages);
		int fromIndex = (currentPage - 1) * PAGE_SIZE;
		int toIndex = Math.min(fromIndex + PAGE_SIZE, allTrips.size());

		model.addAttribute("pageTitle", "여행 플랜 관리");
		model.addAttribute("keyword", keyword);
		model.addAttribute("visibility", visibility);
		model.addAttribute("regionCode", regionCode);
		model.addAttribute("reportedOnly", reportedOnly);
		model.addAttribute("trips", allTrips.subList(fromIndex, toIndex));
		model.addAttribute("totalCount", allTrips.size());
		model.addAttribute("currentPage", currentPage);
		model.addAttribute("totalPages", totalPages);

		return "admin/trip/tripFormView";
	}

	@GetMapping("/{planId}")
	public String getTripDetail(@PathVariable("planId") int planId, Model model) {
		AdminTripDetailDTO trip = adminTripService.getTripDetail(planId);
		List<AdminTripScheduleDTO> schedules = adminTripService.getTripSchedules(planId);
		List<AdminTripReportDTO> reports = adminTripService.getTripReports(planId);

		model.addAttribute("pageTitle", "여행 플랜 상세");
		model.addAttribute("trip", trip);
		model.addAttribute("schedules", schedules);
		model.addAttribute("reports", reports);
		model.addAttribute("kakaoJavascriptKey", kakaoJavascriptKey);

		return "admin/trip/tripDetailView";
	}
}
