package com.noblesi.travelplanner.persistence.jpa.plan;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.noblesi.travelplanner.domain.plan.TravelPlanStatus;

public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {

	Optional<TravelPlanEntity> findByPlanIdAndOwnerMemberIdAndPlanStatus(
			long planId,
			long ownerMemberId,
			TravelPlanStatus planStatus
	);

	Optional<TravelPlanEntity> findByPlanIdAndOwnerMemberId(long planId, long ownerMemberId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			update TravelPlanEntity plan
			   set plan.thumbnailImageUrl = :thumbnailImageUrl
			 where plan.planId = :planId
			""")
	int updateDerivedThumbnail(
			@Param("planId") long planId,
			@Param("thumbnailImageUrl") String thumbnailImageUrl
	);
}
