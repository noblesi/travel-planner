package com.noblesi.travelplanner.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.noblesi.travelplanner.domain.alarm.AlarmDomain;
import com.noblesi.travelplanner.dto.alarm.AlarmRequest;
import com.noblesi.travelplanner.mapper.AlarmMapper;
import com.noblesi.travelplanner.security.MemberPrincipal;

@Service
public class AlarmService {
    
    @Autowired(required = false)
    AlarmMapper alarmMapper;

    public List<AlarmDomain> getAlarmList(Authentication authentication){
        List<AlarmDomain> list = null;

        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        list = alarmMapper.selectAlarm(principal.memberId());
        //System.out.println(alarmMapper.selectAlarm(principal.memberId()));
        return list;
    }

    public List<AlarmDomain> getAlarmCheck(Authentication authentication, long notificationId){
        List<AlarmDomain> list = null;

        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        AlarmRequest alarmRequest = new AlarmRequest(notificationId, principal.memberId());
        if(alarmMapper.updateAlarm(alarmRequest) > 0){
            //list = alarmMapper.selectAlarm(principal.memberId());
        } else {
            list = null;
        }

        return list;
    }

}
