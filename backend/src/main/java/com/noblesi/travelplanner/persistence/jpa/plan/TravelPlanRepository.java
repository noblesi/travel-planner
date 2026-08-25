package com.noblesi.travelplanner.persistence.jpa.plan;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.noblesi.travelplanner.domain.plan.TravelPlanStatus;

public interface TravelPlanRepository extends JpaRepository<TravelPlanEntity, Long> {

	Optional<TravelPlanEntity> findByPlanIdAndOwnerMemberIdAndPlanStatus(
			long planId,
			long ownerMemberId,
			TravelPlanStatus planStatus
	);

	Optional<TravelPlanEntity> findByPlanIdAndOwnerMemberId(long planId, long ownerMemberId);

}
