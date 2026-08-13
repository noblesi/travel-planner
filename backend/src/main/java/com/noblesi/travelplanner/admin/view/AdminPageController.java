package com.noblesi.travelplanner.admin.view;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

	@GetMapping("/tour-data")
	public String tourData(Model model) {
		model.addAttribute("pageTitle", "관광 데이터 관리");
		model.addAttribute("summary", List.of("관광지|14,500", "음식점|8,321", "숙박|3,432", "최근 갱신|321"));
		model.addAttribute("history", List.of(
				new SyncView("S-20260726-03", "2026.07.26 14:20", 4826, 0, "성공", "홍길동"),
				new SyncView("S-20260724-02", "2026.07.24 11:05", 864, 0, "성공", "김관리"),
				new SyncView("S-20260721-01", "2026.07.21 09:30", 1237, 3, "부분 성공", "홍길동")
		));
		return "admin/tour/tourDataFormView";
	}

	public record SyncView(String id, String startedAt, int changedCount, int failedCount,
			String status, String manager) { }
}
