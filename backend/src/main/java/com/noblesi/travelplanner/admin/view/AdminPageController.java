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

	private static final List<TripView> TRIPS = List.of(
			new TripView("P-5412", "서울 궁궐 여행", "서울", "김민수", "1박 2일", 142, 233, "공개", null),
			new TripView("P-1122", "제주 카페 투어", "제주", "이서연", "2박 3일", 52, 73, "공개", null),
			new TripView("P-7898", "부산 서핑 드라이브", "부산", "최지호", "당일 치기", 0, 0, "비공개", null),
			new TripView("P-9041", "제주 숨은 명소 완전 정복", "제주", "박여행", "3박 4일", 31, 128, "검토 대기", "R-221133"),
			new TripView("P-9052", "서울 야경 명소 모음", "서울", "정하늘", "1박 2일", 18, 96, "검토 완료", "R-221144")
	);

	private final NoticeService noticeService;

	public AdminPageController(NoticeService noticeService) {
		this.noticeService = noticeService;
	}

	@GetMapping("/trips")
	public String trips(@RequestParam(name = "keyword", defaultValue = "") String keyword, Model model) {
		// 제목 또는 작성자가 검색어를 포함하는 공개 플랜만 화면에 전달합니다.
		String query = keyword.strip().toLowerCase();
		List<TripView> filtered = TRIPS.stream()
				.filter(trip -> query.isEmpty() || trip.title().toLowerCase().contains(query)
						|| trip.author().toLowerCase().contains(query))
				.toList();
		model.addAttribute("pageTitle", "여행 플랜 관리");
		model.addAttribute("keyword", keyword);
		model.addAttribute("trips", filtered);
		return "admin/trip/tripFormView";
	}

	@GetMapping("/trips/{tripId}")
	public String tripDetail(@PathVariable("tripId") String tripId, Model model) {
		model.addAttribute("pageTitle", "여행 플랜 상세");
		model.addAttribute("tripId", tripId);
		model.addAttribute("schedules", List.of(
				new ScheduleView(1, "경복궁", "오전", "37.5796, 126.9770"),
				new ScheduleView(2, "토속촌 삼계탕", "오후", "37.5775, 126.9715"),
				new ScheduleView(3, "창덕궁", "오후", "37.5794, 126.9910")
		));
		model.addAttribute("reports", List.of(
				new ReportView("R-221133", "2026-07-13", "부적절한 내용", "검토 대기"),
				new ReportView("R-221144", "2026-07-16", "과도한 광고", "검토 완료")
		));
		model.addAttribute("metrics", List.of(
				new MetricView("좋아요", "1,067", 92),
				new MetricView("조회", "74", 28),
				new MetricView("저장", "60", 76)
		));
		return "admin/trip/tripDetailView";
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
	public record TripView(String id, String title, String region, String author, String duration,
			int likes, int views, String status, String reportId) { }
	public record SyncView(String id, String startedAt, int changedCount, int failedCount,
			String status, String manager) { }
	public record ScheduleView(int order, String name, String timeSlot, String coordinates) { }
	public record ReportView(String id, String date, String reason, String status) { }
	public record MetricView(String label, String value, int score) { }
}
