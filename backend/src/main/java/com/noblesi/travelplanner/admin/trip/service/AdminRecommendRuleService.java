package com.noblesi.travelplanner.admin.trip.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.trip.dto.AdminRecommendRuleDTO;
import com.noblesi.travelplanner.admin.trip.mapper.AdminRecommendRuleMapper;

@Service
public class AdminRecommendRuleService {

	private static final BigDecimal TOTAL_WEIGHT = new BigDecimal("100");
	
	@Autowired
	private AdminRecommendRuleMapper adminRecommendRuleMapper;
	
	public AdminRecommendRuleDTO searchRecommendRule() {
		
		AdminRecommendRuleDTO rule = adminRecommendRuleMapper.selectRecommendRule();
		
		if(rule == null) {
			
		}
		
		return rule;
	}
	
	
	
}//class
