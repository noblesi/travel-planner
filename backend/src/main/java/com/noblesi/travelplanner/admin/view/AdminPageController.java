package com.noblesi.travelplanner.admin.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.tour.dto.TourSyncHistoryDTO;
import com.noblesi.travelplanner.admin.tour.service.AdminTourSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminPageController {

	private final AdminTourSyncService adminTourSyncService;

	@GetMapping("/tour-data")
	public String tourData(Model model) {
		model.addAttribute("pageTitle", "관광 데이터 관리");
		model.addAttribute("summary", adminTourSyncService.getSummary());
		model.addAttribute("history", adminTourSyncService.getHistory());
		model.addAttribute("lastSyncedAt", adminTourSyncService.getLastSyncedAt());
		model.addAttribute("syncing", adminTourSyncService.isSyncing());
		return "admin/tour/tourDataFormView";
	}

	@PostMapping("/tour-data/sync")
	public String synchronize(
			@SessionAttribute("loginAdmin") AdminDTO loginAdmin,
			RedirectAttributes redirectAttributes
	) {
		try {
			TourSyncHistoryDTO result = adminTourSyncService.synchronize(loginAdmin.getName());
			redirectAttributes.addFlashAttribute(
					"message",
					"TOUR API 데이터 " + result.changedCount() + "건을 동기화했습니다."
			);
		} catch (IllegalStateException exception) {
			redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
		} catch (RuntimeException exception) {
			log.error("TOUR API 동기화 실패", exception);
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"TOUR API 동기화에 실패했습니다. API 키와 서버 로그를 확인해 주세요."
			);
		}
		return "redirect:/admin/tour-data";
	}
}
