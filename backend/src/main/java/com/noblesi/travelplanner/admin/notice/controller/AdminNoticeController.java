package com.noblesi.travelplanner.admin.notice.controller;

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
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notices")
public class AdminNoticeController {
	private final AdminNoticeService adminNoticeService;

	@GetMapping
	public String getNoticeList(
			@RequestParam(name = "keyword", defaultValue = "") String keyword,
			@RequestParam(name = "categoryCode", defaultValue = "") String categoryCode,
			@RequestParam(name = "page", defaultValue = "1") int page,
			Model model) {
		AdminNoticeSearchDTO search = new AdminNoticeSearchDTO(keyword, categoryCode, page, 10);
		model.addAttribute("pageTitle", "공지사항 관리");
		model.addAttribute("keyword", keyword);
		model.addAttribute("categoryCode", categoryCode);
		model.addAttribute("notices", adminNoticeService.getNoticeList(search));
		return "admin/notice/noticeFormView";
	}

	@GetMapping("/{noticeId}")
	public String getNoticeDetail(@PathVariable("noticeId") Long noticeId, Model model) {
		model.addAttribute("pageTitle", "공지사항 상세");
		model.addAttribute("notice", adminNoticeService.getNoticeDetail(noticeId));
		return "admin/notice/noticeDetailView";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		model.addAttribute("pageTitle", "공지사항 등록");
		model.addAttribute("editMode", false);
		model.addAttribute("noticeForm", new AdminNoticeFormDTO());
		return "admin/notice/noticeWriteView";
	}

	@PostMapping
	public String createNotice(@Valid @ModelAttribute("noticeForm") AdminNoticeFormDTO noticeForm,
			BindingResult bindingResult, @SessionAttribute("loginAdmin") AdminDTO loginAdmin,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "공지사항 등록");
			model.addAttribute("editMode", false);
			return "admin/notice/noticeWriteView";
		}
		adminNoticeService.createNotice(noticeForm, (long) loginAdmin.getAdminId());
		redirectAttributes.addFlashAttribute("message", "공지사항이 등록되었습니다.");
		return "redirect:/admin/notices";
	}

	@GetMapping("/{noticeId}/edit")
	public String updateForm(@PathVariable("noticeId") Long noticeId, Model model) {
		model.addAttribute("pageTitle", "공지사항 수정");
		model.addAttribute("editMode", true);
		model.addAttribute("noticeId", noticeId);
		model.addAttribute("noticeForm", adminNoticeService.getNoticeForm(noticeId));
		return "admin/notice/noticeWriteView";
	}

	@PostMapping("/{noticeId}")
	public String updateNotice(@PathVariable("noticeId") Long noticeId,
			@Valid @ModelAttribute("noticeForm") AdminNoticeFormDTO noticeForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
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

	@PostMapping("/{noticeId}/delete")
	public String deleteNotice(@PathVariable("noticeId") Long noticeId,
			RedirectAttributes redirectAttributes) {
		adminNoticeService.deleteNotice(noticeId);
		redirectAttributes.addFlashAttribute("message", "공지사항이 삭제되었습니다.");
		return "redirect:/admin/notices";
	}
}
