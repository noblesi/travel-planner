import http from "./http";

export async function getAlarmList() {
    const response = await http.get("/alarm/alarmList");
    return response.data.data;
}

export async function markAlarmAsRead(notificationId) {
    const response = await http.post("/alarm/alarmCheck", null, { params: {notificationId} });
    return response.data.data;
}
