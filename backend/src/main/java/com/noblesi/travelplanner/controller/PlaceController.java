package com.noblesi.travelplanner.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.dto.place.PlaceSearchResponse;
import com.noblesi.travelplanner.service.PlaceSearchService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

	private final PlaceSearchService placeSearchService;

	public PlaceController(PlaceSearchService placeSearchService) {
		this.placeSearchService = placeSearchService;
	}

	@GetMapping("/search")
	public ApiResponse<PlaceSearchResponse> searchPlaces(
			@RequestParam
			@NotBlank(message = "검색어는 필수 값입니다.")
			@Size(max = 100, message = "검색어는 100자 이하여야 합니다.")
			String keyword,
			@RequestParam(required = false)
			@Pattern(regexp = "[1-9]\\d?", message = "지역 코드는 1~2자리 양의 정수여야 합니다.")
			String regionCode,
			@RequestParam(required = false)
			@Size(max = 100, message = "카테고리는 100자 이하여야 합니다.")
			String category,
			@RequestParam(defaultValue = "1")
			@Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
			@Max(value = 45, message = "페이지는 45 이하여야 합니다.")
			int page,
			@RequestParam(defaultValue = "10")
			@Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
			@Max(value = 15, message = "페이지 크기는 15 이하여야 합니다.")
			int size
	) {
		return ApiResponse.success(placeSearchService.search(
				keyword,
				regionCode,
				category,
				page,
				size
		));
	}
}
