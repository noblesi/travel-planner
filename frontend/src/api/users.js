import http from "./http"

export async function getMemberEmailCheck(email){
  console.log(" users js 이메일 : " + email)
  const response = await http.get('/users/emailCheck', { params: { email } })
  return response.data.data
}

export async function postMemberJoin(userInfo) {
  console.log("============postMemberJoin : " + JSON.stringify(userInfo));
  const response = await http.post('/users/join', userInfo)
  return response.data.data
}