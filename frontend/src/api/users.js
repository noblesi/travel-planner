import http from './http'

export async function getMemberEmailCheck(email) {
  const response = await http.get('/users/emailCheck', { params: { email } })
  return response.data.data
}

export async function postMemberJoin(userInfo) {
  const response = await http.post('/users/join', userInfo)
  return response.data.data
}
