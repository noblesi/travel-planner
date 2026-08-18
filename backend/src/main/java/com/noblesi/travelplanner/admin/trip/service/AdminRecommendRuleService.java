package com.noblesi.travelplanner.admin.trip.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.admin.trip.dto.AdminRecommendRuleDTO;
import com.noblesi.travelplanner.admin.trip.mapper.AdminRecommendRuleMapper;
import com.noblesi.travelplanner.common.exception.BusinessException;

@Service
public class AdminRecommendRuleService {

	private static final BigDecimal TOTAL_WEIGHT = new BigDecimal("100");
	
	@Autowired
	private AdminRecommendRuleMapper adminRecommendRuleMapper;
	
	/**
	 * 추천 점수 규칙 전체 조회 메서드
	 */
	public AdminRecommendRuleDTO searchRecommendRule() {
		
		AdminRecommendRuleDTO rule = adminRecommendRuleMapper.selectRecommendRule();
		
		if(rule == null) {
			rule = createDefaultRule();
		}//end if
		
		return rule;
	}//searchRecommendRule
	
	
	/**
	 * 추천 점수 규칙 저장 메소드
	 * @param rule
	 * @param loginId
	 */
	@Transactional
	public void saveRecommendRule(AdminRecommendRuleDTO rule, Long loginId) {
		
		validateWeight(rule);
		
		adminRecommendRuleMapper.updateRecommendRule();
		
		int insertedCount = adminRecommendRuleMapper.insertRecommendRule(rule, loginId);
		
		if(insertedCount != 1) {
			throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, 
					"RECOMMEND_RULE_SAVE_FAILED", 
					"추천 점수 규칙에 저장을 실패헸습니다.");
		}//end if
	}//saveRecommendRule
	
	public void validateWeight(AdminRecommendRuleDTO rule) {
		if(rule.getLikeWeight()==null || rule.getCopyWeight()==null || rule.getViewWeight()==null){
			throw invalidWeight();
		}//end if
		
		BigDecimal total = rule.getLikeWeight().add(rule.getViewWeight()).add(rule.getCopyWeight());
		
		if(total.compareTo(TOTAL_WEIGHT) != 0) {
			throw invalidWeight();
		}//end if
	}//validateWeight
	
	public AdminRecommendRuleDTO createDefaultRule() {
		AdminRecommendRuleDTO rule = new AdminRecommendRuleDTO();
		rule.setLikeWeight(new BigDecimal("40"));
		rule.setViewWeight(new BigDecimal("20"));
		rule.setCopyWeight(new BigDecimal("40"));
		rule.setActiveYn("Y");
		return rule;
	}//createDefaultRule
	
	private BusinessException invalidWeight() {
		return new BusinessException(HttpStatus.BAD_REQUEST, 
				"INVALID_RECOMMEND_WEIGHT", 
				"추천 점수 가중치의 합계는 100이여야 합니다.");
	}//invalidWeight
	
}//class
