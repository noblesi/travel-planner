package com.noblesi.travelplanner.admin.trip.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.admin.trip.mapper.AdminTripMapper;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripDetailDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripListDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripReportDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripScheduleDTO;
import com.noblesi.travelplanner.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminTripService {

	private static final Set<String> VISIBILITIES = Set.of("", "PUBLIC", "PRIVATE");
	private static final Set<String> REPORT_STATUSES = Set.of("", "COMPLETED", "INCOMPLETE");

	private final AdminTripMapper adminTripMapper;

	public List<AdminTripListDTO> getTripList(
			String keyword,
			String visibility,
			String regionCode,
			boolean reportedOnly,
			String reportStatus
	) {
		String normalizedKeyword = keyword == null ? "" : keyword.strip();
		String normalizedVisibility = visibility == null ? "" : visibility.strip().toUpperCase();
		String normalizedRegionCode = regionCode == null ? "" : regionCode.strip();
		String normalizedReportStatus = reportStatus == null ? "" : reportStatus.strip().toUpperCase();

		if (!VISIBILITIES.contains(normalizedVisibility)) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"INVALID_PLAN_VISIBILITY",
					"올바르지 않은 플랜 공개 상태입니다."
			);
		}
		if (!REPORT_STATUSES.contains(normalizedReportStatus)) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"INVALID_REPORT_STATUS",
					"올바르지 않은 신고 처리 상태입니다."
			);
		}

		return adminTripMapper.selectTripList(
				normalizedKeyword,
				normalizedVisibility,
				normalizedRegionCode,
				reportedOnly,
				normalizedReportStatus
		);
	}

	public AdminTripDetailDTO getTripDetail(Long planId) {
		AdminTripDetailDTO trip = adminTripMapper.selectTripDetail(planId);

		if (trip == null) {
			throw tripNotFound();
		}

		return trip;
	}

	public List<AdminTripScheduleDTO> getTripSchedules(Long planId) {
		return adminTripMapper.selectTripSchedules(planId);
	}

	public List<AdminTripReportDTO> getTripReports(Long planId) {
		return adminTripMapper.selectTripReports(planId);
	}

	private BusinessException tripNotFound() {
		return new BusinessException(
				HttpStatus.NOT_FOUND,
				"TRIP_NOT_FOUND",
				"여행 플랜을 찾을 수 없습니다."
		);
	}
}
