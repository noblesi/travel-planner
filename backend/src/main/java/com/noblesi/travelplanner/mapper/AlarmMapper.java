package com.noblesi.travelplanner.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.noblesi.travelplanner.domain.alarm.AlarmDomain;
import com.noblesi.travelplanner.dto.alarm.AlarmRequest;

@Mapper
public interface AlarmMapper {
    
    public List<AlarmDomain> selectAlarm(long memberId);
    //public int selectAlarm(long memberId);

    public int updateAlarm(AlarmRequest alarmRequest);

}
