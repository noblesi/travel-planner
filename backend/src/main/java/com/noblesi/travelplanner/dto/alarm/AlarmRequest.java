package com.noblesi.travelplanner.dto.alarm;

import org.apache.ibatis.type.Alias;

@Alias("alarmRequest")
public record AlarmRequest(
    long notificationId,
    long recipientMemberId
) {
    
}
