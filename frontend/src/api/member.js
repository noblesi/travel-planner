import http from "./http"

export async function postMemberInfo(memberId) {
  console.log("postMemberInfo in")
  const response = await http.post('/Member/myPage', memberId)
  return response.data.data
}