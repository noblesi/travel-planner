package com.noblesi.travelplanner.admin.trip.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminTripScheduleDTO {

	private Long scheduleItemId;
	private int dayNo;
	private Date travelDate;
	private String timeSlot;
	private int positionNo;
	private String placeName;
	private String category;
	private String address;
	private String latitude;
	private String longitude;

	public String getTimeSlotLabel() {
		if (timeSlot == null) {
			return "";
		}
		return switch (timeSlot.toUpperCase()) {
			case "MORNING" -> "오전";
			case "AFTERNOON" -> "오후";
			default -> timeSlot;
		};
	}
}
