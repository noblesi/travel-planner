import http from "./http";

export async function getAlarmList() {
    const response = await http.get("/alarm/alarmList");
    return response.data.data;
}

export async function getAlarmCheck(notificationId) {
    console.log(notificationId + " : notificationId")
    const response = await http.get("/alarm/alarmCheck", { params: {notificationId:notificationId }} );
    return response.data.data;
}