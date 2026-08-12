package com.noblesi.travelplanner.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noblesi.travelplanner.domain.place.PlaceCatalogEntry;

@Mapper
public interface PlaceMasterMapper {

	PlaceCatalogEntry findActiveByProviderAndExternalId(
			@Param("placeProvider") String placeProvider,
			@Param("externalPlaceId") String externalPlaceId
	);

	int updatePlace(PlaceCatalogEntry place);

	int insertPlace(PlaceCatalogEntry place);
}
