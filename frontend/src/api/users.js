import http from "./http"

export async function postMemberJoin(userInfo) {
  const response = await http.post('/users/join', userInfo)
  return response.data.data
}