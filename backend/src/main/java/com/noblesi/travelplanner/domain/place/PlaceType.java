package com.noblesi.travelplanner.domain.place;

public enum PlaceType {
	ATTRACTION("관광지"),
	CULTURAL_FACILITY("문화시설"),
	FESTIVAL_EVENT("축제·공연·행사"),
	TRAVEL_COURSE("여행코스"),
	LEISURE_SPORTS("레포츠"),
	ACCOMMODATION("숙박"),
	SHOPPING("쇼핑"),
	RESTAURANT("음식점"),
	TOURIST_INFORMATION("관광정보");

	private final String categoryName;

	PlaceType(String categoryName) {
		this.categoryName = categoryName;
	}

	public String categoryName() {
		return categoryName;
	}

	public static PlaceType fromTourApiContentTypeId(String contentTypeId) {
		return switch (contentTypeId == null ? "" : contentTypeId) {
			case "12" -> ATTRACTION;
			case "14" -> CULTURAL_FACILITY;
			case "15" -> FESTIVAL_EVENT;
			case "25" -> TRAVEL_COURSE;
			case "28" -> LEISURE_SPORTS;
			case "32" -> ACCOMMODATION;
			case "38" -> SHOPPING;
			case "39" -> RESTAURANT;
			default -> TOURIST_INFORMATION;
		};
	}
}
