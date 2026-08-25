package com.noblesi.travelplanner.controller;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.noblesi.travelplanner.common.api.ApiResponse;
import com.noblesi.travelplanner.domain.alarm.AlarmDomain;
import com.noblesi.travelplanner.dto.alarm.AlarmRequest;
import com.noblesi.travelplanner.service.AlarmService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/alarm")
public class AlarmController {
    
    @Autowired(required = false)
    AlarmService alarmService;

    @GetMapping("/alarmList")
    public ApiResponse<List<AlarmDomain>> getAlarmList(Authentication authentication) {
        return ApiResponse.success(alarmService.getAlarmList(authentication));
    }
    
    @GetMapping("/alarmCheck")
    public ApiResponse<List<AlarmDomain>> getAlarmCheck(Authentication authentication, @RequestParam long notificationId){
        //System.out.println(notificationId+" : notificationId");
        return ApiResponse.success(alarmService.getAlarmCheck(authentication, notificationId));
    }

}
