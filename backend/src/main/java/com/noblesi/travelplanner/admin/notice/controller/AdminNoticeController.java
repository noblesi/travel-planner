package com.noblesi.travelplanner.admin.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeFormDTO;
import com.noblesi.travelplanner.admin.notice.dto.AdminNoticeSearchDTO;
import com.noblesi.travelplanner.admin.notice.service.AdminNoticeService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/notices")
public class AdminNoticeController {
	@Autowired
	private AdminNoticeService adminNoticeService;

	/** 검색 조건과 페이지 번호를 받아 공지사항 목록 화면을 구성합니다. */
	@GetMapping
	public String getNoticeList(@RequestParam(name = "keyword", defaultValue = "") String keyword,
			@RequestParam(name = "categoryCode", defaultValue = "") String categoryCode,
			@RequestParam(name = "page", defaultValue = "1") int page, Model model) {
		AdminNoticeSearchDTO search = new AdminNoticeSearchDTO(keyword, categoryCode, page, 10);
		model.addAttribute("pageTitle", "공지사항 관리");
		model.addAttribute("keyword", keyword);
		model.addAttribute("categoryCode", categoryCode);
		model.addAttribute("notices", adminNoticeService.getNoticeList(search));
		return "admin/notice/noticeFormView";
	}

	/** 공지사항 번호로 상세 정보를 조회하여 상세 화면에 전달합니다. */
	@GetMapping("/{noticeId}")
	public String getNoticeDetail(@PathVariable("noticeId") Long noticeId, Model model) {
		model.addAttribute("pageTitle", "공지사항 상세");
		model.addAttribute("notice", adminNoticeService.getNoticeDetail(noticeId));
		return "admin/notice/noticeDetailView";
	}

	/** 공지사항 등록에 사용할 빈 입력 객체와 작성 화면을 제공합니다. */
	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("pageTitle", "공지사항 등록");
		model.addAttribute("editMode", false);
		model.addAttribute("noticeForm", new AdminNoticeFormDTO());
		return "admin/notice/noticeWriteView";
	}

	/** 입력값을 검증한 뒤 로그인한 관리자 명의로 새 공지사항을 등록합니다. */
	@PostMapping
	public String createNotice(@Valid @ModelAttribute("noticeForm") AdminNoticeFormDTO noticeForm,
			BindingResult bindingResult, @SessionAttribute("loginAdmin") AdminDTO loginAdmin, Model model,
			RedirectAttributes redirectAttributes) {
		// 검증 오류가 있으면 사용자가 입력한 값을 유지한 채 작성 화면을 다시 보여줍니다.
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "공지사항 등록");
			model.addAttribute("editMode", false);
			return "admin/notice/noticeWriteView";
		}
		adminNoticeService.createNotice(noticeForm, (long) loginAdmin.getAdminId());
		redirectAttributes.addFlashAttribute("message", "공지사항이 등록되었습니다.");
		return "redirect:/admin/notices";
	}

	/** 기존 공지사항을 입력 객체로 변환하여 수정 화면에 전달합니다. */
	@GetMapping("/{noticeId}/edit")
	public String updateForm(@PathVariable("noticeId") Long noticeId, Model model) {
		model.addAttribute("pageTitle", "공지사항 수정");
		model.addAttribute("editMode", true);
		model.addAttribute("noticeId", noticeId);
		model.addAttribute("noticeForm", adminNoticeService.getNoticeForm(noticeId));
		return "admin/notice/noticeWriteView";
	}

	/** 수정 입력값을 검증하고 해당 공지사항을 변경합니다. */
	@PostMapping("/{noticeId}")
	public String updateNotice(@PathVariable("noticeId") Long noticeId,
			@Valid @ModelAttribute("noticeForm") AdminNoticeFormDTO noticeForm, BindingResult bindingResult,
			Model model, RedirectAttributes redirectAttributes) {
		// 수정 검증 실패 시 수정 여부와 공지 번호를 다시 전달해야 같은 폼을 사용할 수 있습니다.
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "공지사항 수정");
			model.addAttribute("editMode", true);
			model.addAttribute("noticeId", noticeId);
			return "admin/notice/noticeWriteView";
		}
		adminNoticeService.updateNotice(noticeId, noticeForm);
		redirectAttributes.addFlashAttribute("message", "공지사항이 수정되었습니다.");
		return "redirect:/admin/notices/" + noticeId;
	}

	/** 공지사항을 DB에서 실제 삭제하고 목록 화면으로 이동합니다. */
	@PostMapping("/{noticeId}/delete")
	public String deleteNotice(@PathVariable("noticeId") Long noticeId, RedirectAttributes redirectAttributes) {
		adminNoticeService.deleteNotice(noticeId);
		redirectAttributes.addFlashAttribute("message", "공지사항이 삭제되었습니다.");
		return "redirect:/admin/notices";
	}
}
