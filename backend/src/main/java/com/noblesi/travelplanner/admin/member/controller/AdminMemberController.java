package com.noblesi.travelplanner.admin.member.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.noblesi.travelplanner.admin.member.dto.AdminMemberDetailDTO;
import com.noblesi.travelplanner.admin.member.dto.AdminMemberSearchDTO;
import com.noblesi.travelplanner.admin.member.service.AdminMemberService;

@RequestMapping("/admin/members")
@Controller
public class AdminMemberController {
	private static final int PAGE_SIZE = 10;

	@Autowired
	private AdminMemberService adminMemberService;
	
	/**
	 * 관리자 회원 목록 조회
	 * @return
	 */
	@GetMapping
	public String getMemberList(@RequestParam(name = "keyword", defaultValue = "") String keyword, 
			@RequestParam(name="memberStatus", defaultValue = "") String memberStatus,
			@RequestParam(name="page", defaultValue = "1") int page,
			Model model) {
		
		// 화면 상단에 표시될 제목
		model.addAttribute("pageTitle","회원 관리");
		
		// 검색 후에도 화면에 값이 남게 
		model.addAttribute("keyword",keyword);
		model.addAttribute("memberStatus",memberStatus);
		
		model.addAttribute("members", adminMemberService.getMemberList(
				new AdminMemberSearchDTO(keyword, memberStatus, page, PAGE_SIZE)));
		
		
		return "admin/member/memberFormView";
	}//getMemberList
	
	@GetMapping("/{memberId}")
	public String getMemberDetail(@PathVariable("memberId") Long memberId , Model model) {
		AdminMemberDetailDTO adDTO = adminMemberService.getMemberDetail(memberId);
		// 화면 상단에 표시될 제목
		model.addAttribute("pageTitle","회원 상세");
		model.addAttribute("member", adDTO);
		
		return "admin/member/memberDetailView";
	}//getMemberDetail
	
	@PostMapping("/{memberId}/status")
	public String removeMemberStatus(@PathVariable("memberId") Long memberId,
			@RequestParam("memberStatus") String memberStatus, RedirectAttributes redirectAttributes) {
		adminMemberService.removeMemberStatus(memberId, memberStatus);
		
		redirectAttributes.addFlashAttribute("message","회원 상태가 변경되었습니다.");
		
		
		
		return "redirect:/admin/members/" + memberId;
	}//removeMemberStatus
	
}//class
