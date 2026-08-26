package com.noblesi.travelplanner.domain.alarm;

import java.time.LocalDateTime;

public record AlarmDomain(
    long notificationId,
    long recipientMemberId,
    String notificationType, 
    String title,
    String content,
    String readYn,
    Long relatedReportId,
    LocalDateTime createdAt
) {
    
}
