package com.noblesi.travelplanner.plansearch.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlanLikeDAO {

	// 좋아요 등록
	int insertLike(@Param("memberId") Long memberId, @Param("planId") Long planId);

	// 좋아요 삭제
	int deleteLike(@Param("memberId") Long memberId, @Param("planId") Long planId);

	// 좋아요 여부 확인
	boolean existsLike(@Param("memberId") Long memberId, @Param("planId") Long planId);
}
