package com.noblesi.travelplanner.admin.trip.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.trip.dto.AdminRecommendRuleDTO;

@Mapper
public interface AdminRecommendRuleMapper {

	/**
	 * 현재 활성화된 추천 점수 규칙을 조회합니다.
	 */
	AdminRecommendRuleDTO selectRecommendRule();
	
	/**
	 * 현재 활성화 되어 있는 규칙을 비활성화 합니다.
	 */
	int updateRecommendRule();
	
	/**
	 * 새로운 추천 규칙을 추가합니다.
	 */
	int insertRecommendRule(@Param("rule") AdminRecommendRuleDTO rule, @Param("adminId") long adminId); 
	
 }//interface
