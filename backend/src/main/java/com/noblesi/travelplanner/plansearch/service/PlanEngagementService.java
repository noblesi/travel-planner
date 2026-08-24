package com.noblesi.travelplanner.plansearch.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.plansearch.dao.PlanLikeDAO;
import com.noblesi.travelplanner.plansearch.dao.ReportDAO;
import com.noblesi.travelplanner.plansearch.dto.PublishedPlanTargetDTO;
import com.noblesi.travelplanner.plansearch.dto.ReportRequestDTO;

@Service
public class PlanEngagementService {

	private final PlanLikeDAO planLikeDAO;
	private final ReportDAO reportDAO;
	private final PublishedPlanResolver publishedPlanResolver;

	public PlanEngagementService(
			PlanLikeDAO planLikeDAO,
			ReportDAO reportDAO,
			PublishedPlanResolver publishedPlanResolver
	) {
		this.planLikeDAO = planLikeDAO;
		this.reportDAO = reportDAO;
		this.publishedPlanResolver = publishedPlanResolver;
	}

	@Transactional
	public boolean toggleLike(long memberId, long planId) {
		publishedPlanResolver.require(planId);
		if (planLikeDAO.existsLike(memberId, planId)) {
			planLikeDAO.deleteLike(memberId, planId);
			return false;
		}
		try {
			planLikeDAO.insertLike(memberId, planId);
		} catch (DuplicateKeyException exception) {
			return true;
		}
		return true;
	}

	@Transactional
	public void report(long memberId, long planId, ReportRequestDTO request) {
		PublishedPlanTargetDTO target = publishedPlanResolver.require(planId);
		if (memberId == target.getOwnerMemberId()) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"SELF_PLAN_REPORT_NOT_ALLOWED",
					"본인의 여행 플랜은 신고할 수 없습니다."
			);
		}
		if (reportDAO.existsReport(memberId, planId)) {
			throw reportAlreadyExists();
		}

		if (request.getDetail() != null) {
			String normalizedDetail = request.getDetail().trim();
			request.setDetail(normalizedDetail.isEmpty() ? null : normalizedDetail);
		}
		try {
			reportDAO.insertReport(memberId, planId, request);
		} catch (DuplicateKeyException exception) {
			throw reportAlreadyExists();
		}
	}

	private BusinessException reportAlreadyExists() {
		return new BusinessException(
				HttpStatus.CONFLICT,
				"REPORT_ALREADY_EXISTS",
				"이미 신고한 여행 플랜입니다."
		);
	}
}
