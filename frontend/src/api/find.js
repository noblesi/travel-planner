import http from "./http"

export async function getEmailFind(userFindInfo){
  const response = await http.post('/find/emailFind',userFindInfo)
  return response.data.data
}

export async function getPasswordFindReword(userFindPassword){
  const response = await http.post('/find/passwordFind', userFindPassword)
  return response.data.data
}

export async function getPasswordReword(rewordPass){
  const response = await http.post('/find/passwordReword', rewordPass)
  return response.data.data
}
