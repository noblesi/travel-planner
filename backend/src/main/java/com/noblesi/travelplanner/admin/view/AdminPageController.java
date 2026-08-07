package com.noblesi.travelplanner.admin.view;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.noblesi.travelplanner.notice.dto.NoticeSearchRequestDTO;
import com.noblesi.travelplanner.notice.service.NoticeService;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

	private final NoticeService noticeService;

	public AdminPageController(NoticeService noticeService) {
		this.noticeService = noticeService;
	}

	@GetMapping("/reports/{reportId}")
	public String reportDetail(@PathVariable("reportId") String reportId, Model model) {
		model.addAttribute("pageTitle", "신고 상세");
		model.addAttribute("reportId", reportId);
		return "admin/report/reportDetailView";
	}

	@GetMapping("/notices")
	public String notices(@RequestParam(name = "category", required = false) String category,
			@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
		// 공지사항은 구현된 Service를 사용하며, 페이지 번호는 화면 기준으로 1부터 시작합니다.
		model.addAttribute("pageTitle", "공지사항 관리");
		model.addAttribute("category", category);
		model.addAttribute("notices", noticeService.getNoticeList(new NoticeSearchRequestDTO(category, page, 10)));
		return "admin/notice/noticeFormView";
	}

	@GetMapping("/notices/new")
	public String newNotice(Model model) {
		model.addAttribute("pageTitle", "공지사항 작성");
		model.addAttribute("editMode", false);
		model.addAttribute("notice", null);
		return "admin/notice/noticeWriteView";
	}

	@GetMapping("/notices/{noticeId}")
	public String noticeDetail(@PathVariable("noticeId") long noticeId, Model model) {
		model.addAttribute("pageTitle", "공지사항 상세");
		model.addAttribute("notice", noticeService.getNoticeDetail(noticeId));
		return "admin/notice/noticeDetailView";
	}

	@GetMapping("/notices/{noticeId}/edit")
	public String editNotice(@PathVariable("noticeId") long noticeId, Model model) {
		model.addAttribute("pageTitle", "공지사항 수정");
		model.addAttribute("editMode", true);
		model.addAttribute("notice", noticeService.getNoticeDetail(noticeId));
		return "admin/notice/noticeWriteView";
	}

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

	// Thymeleaf 전용 읽기 모델입니다. 영속 Domain과 화면 표시 데이터를 분리합니다.
	public record SyncView(String id, String startedAt, int changedCount, int failedCount,
			String status, String manager) { }
}
