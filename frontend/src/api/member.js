import http from "./http"

export async function getMemberInfo() { 
  const response = await http.get('/member/myPage')
  console.log(response.data.data.memberName + "response")
  return response.data.data
}