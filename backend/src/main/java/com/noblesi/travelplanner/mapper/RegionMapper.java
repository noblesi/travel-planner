package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.region.Region;

@Mapper
public interface RegionMapper {

	List<Region> findActiveSidoRegions();

	Region findActiveSidoRegionByCode(@Param("regionCode") String regionCode);
}
