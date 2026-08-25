package com.noblesi.travelplanner.admin.tour.mapper;

import java.time.OffsetDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.tour.domain.TourSyncHistoryDomain;

@Mapper
public interface AdminTourSyncMapper {

	/** 동기화가 끝난 시점의 결과를 성공 여부와 관계없이 영구 보관합니다. */
	int insertHistory(
			@Param("managerLoginId") String managerLoginId,
			@Param("startedAt") OffsetDateTime startedAt,
			@Param("finishedAt") OffsetDateTime finishedAt,
			@Param("successCount") int successCount,
			@Param("failCount") int failCount,
			@Param("processStatus") String processStatus,
			@Param("errorMessage") String errorMessage
	);

	/** 관리 화면에 노출할 최근 실행 결과만 최신순으로 조회합니다. */
	List<TourSyncHistoryDomain> selectRecentHistory(@Param("limit") int limit);
}
