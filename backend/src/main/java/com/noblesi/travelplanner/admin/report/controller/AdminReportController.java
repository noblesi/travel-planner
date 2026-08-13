package com.noblesi.travelplanner.admin.report.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.noblesi.travelplanner.admin.auth.dto.AdminDTO;
import com.noblesi.travelplanner.admin.report.dto.AdminReportDTO;
import com.noblesi.travelplanner.admin.report.dto.AdminReportProcessDTO;
import com.noblesi.travelplanner.admin.report.service.AdminReportService;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

	@Autowired
	private AdminReportService adminReportService;

	@GetMapping("/{reportId}")
	public String getReportDetail(@PathVariable("reportId") int reportId, Model model) {
		AdminReportDTO report = adminReportService.getReportDetail(reportId);
		AdminReportProcessDTO processForm = new AdminReportProcessDTO();
		processForm.setReportId(reportId);

		model.addAttribute("pageTitle", "신고 상세");
		model.addAttribute("report", report);
		model.addAttribute("processForm", processForm);
		return "admin/report/reportDetailView";
	}

	@PostMapping("/{reportId}/complete")
	public String completeReport(
			@PathVariable("reportId") int reportId,
			@ModelAttribute("processForm") AdminReportProcessDTO processForm,
			@SessionAttribute("loginAdmin") AdminDTO loginAdmin,
			RedirectAttributes redirectAttributes) {
		processForm.setReportId(reportId);
		adminReportService.completeReport(processForm, loginAdmin.getAdminId());
		redirectAttributes.addFlashAttribute("message", "신고 검토가 완료되었습니다.");
		return "redirect:/admin/reports/" + reportId;
	}
}
