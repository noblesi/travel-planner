package com.noblesi.travelplanner.admin.trip.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.admin.trip.dto.AdminTripDetailDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripListDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripReportDTO;
import com.noblesi.travelplanner.admin.trip.dto.AdminTripScheduleDTO;

@Mapper
public interface AdminTripMapper {

	List<AdminTripListDTO> selectTripList(
			@Param("keyword") String keyword,
			@Param("visibility") String visibility,
			@Param("regionCode") String regionCode,
			@Param("reportedOnly") boolean reportedOnly,
			@Param("reportStatus") String reportStatus
	);

	AdminTripDetailDTO selectTripDetail(@Param("planId") int planId);

	List<AdminTripScheduleDTO> selectTripSchedules(@Param("planId") int planId);

	List<AdminTripReportDTO> selectTripReports(@Param("planId") int planId);
}
