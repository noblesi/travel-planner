import http from "./http"

export async function getMemberJoin() {
  const response = await http.get('/users/join')
  return response.data.data
}