import http from "./http"

export async function postMemberEmailCheck(email){
  const response = await http.post('/users/emailCheck', email)
  return response.data.data
}

export async function postMemberJoin(userInfo) {
  const response = await http.post('/users/join', userInfo)
  return response.data.data
}