package com.noblesi.travelplanner.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noblesi.travelplanner.common.exception.BusinessException;
import com.noblesi.travelplanner.domain.place.PlaceCatalogEntry;
import com.noblesi.travelplanner.integration.kakao.KakaoLocalSearchResult.KakaoPlace;
import com.noblesi.travelplanner.integration.tourapi.TourApiSearchResult.TourApiPlace;
import com.noblesi.travelplanner.mapper.PlaceMasterMapper;

@Service
public class PlaceCatalogService {

	static final String TOUR_API_PROVIDER = "TOUR_API";
	static final String KAKAO_PROVIDER = "KAKAO";

	private final PlaceMasterMapper placeMasterMapper;
	private final ExternalImageUrlPolicy imageUrlPolicy;

	public PlaceCatalogService(
			PlaceMasterMapper placeMasterMapper,
			ExternalImageUrlPolicy imageUrlPolicy
	) {
		this.placeMasterMapper = placeMasterMapper;
		this.imageUrlPolicy = imageUrlPolicy;
	}

	@Transactional
	public void rememberTourApiPlaces(Iterable<TourApiPlace> places) {
		for (TourApiPlace place : places) {
			PlaceCatalogEntry entry = new PlaceCatalogEntry(
					TOUR_API_PROVIDER,
					place.externalPlaceId(),
					place.placeType(),
					place.placeName(),
					place.categoryName(),
					place.address(),
					null,
					place.latitude(),
					place.longitude(),
					imageUrlPolicy.sanitize(place.imageUrl())
			);
			upsert(entry);
		}
	}

	@Transactional
	public void rememberKakaoPlaces(Iterable<KakaoPlace> places) {
		for (KakaoPlace place : places) {
			PlaceCatalogEntry entry = new PlaceCatalogEntry(
					KAKAO_PROVIDER,
					place.externalPlaceId(),
					place.placeType(),
					place.placeName(),
					place.categoryName(),
					place.address(),
					null,
					place.latitude(),
					place.longitude(),
					imageUrlPolicy.sanitize(place.imageUrl())
			);
			upsert(entry);
		}
	}

	@Transactional(readOnly = true)
	public PlaceCatalogEntry requireActivePlace(String placeProvider, String externalPlaceId) {
		PlaceCatalogEntry place = placeMasterMapper.findActiveByProviderAndExternalId(
				placeProvider,
				externalPlaceId
		);
		if (place == null) {
			throw new BusinessException(
					HttpStatus.BAD_REQUEST,
					"PLACE_REFERENCE_NOT_FOUND",
					"장소 검색 결과를 다시 조회한 후 일정에 추가해 주세요."
			);
		}
		return place;
	}

	private void upsert(PlaceCatalogEntry place) {
		if (placeMasterMapper.updatePlace(place) == 1) {
			return;
		}
		try {
			placeMasterMapper.insertPlace(place);
		} catch (DuplicateKeyException exception) {
			placeMasterMapper.updatePlace(place);
		}
	}
}
