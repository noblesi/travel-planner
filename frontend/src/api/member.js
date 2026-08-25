import http, { clearCsrfTokenCache } from './http'

export async function getMyProfile() {
  const response = await http.get('/members/me')
  return response.data.data
}

export async function updateMyProfile(payload) {
  const response = await http.patch('/members/me', payload)
  return response.data.data
}

export async function changeMyPassword(payload) {
  const response = await http.patch('/members/me/password', payload)
  return response.data.data
}

export async function withdrawMyAccount(currentPassword) {
  const response = await http.delete('/members/me', {
    data: { currentPassword },
  })
  clearCsrfTokenCache()
import http from "./http"

export async function getMemberInfo() { 
  const response = await http.get('/member/myPage')
  return response.data.data
}

export async function postModifyMemberInfo(requestData) { 
  const response = await http.post('/member/modifyMemberInfo', requestData)
  return response.data.data
}

export async function getModifyNickname(nickname) { 
  const response = await http.get('/member/modifyNickname', {params:{nickname}})
  return response.data.data
}

export async function postModifyProfileImage(formData) { 
  const response = await http.post('/member/modifyProfileImage', formData, {
      headers: {
          'Content-Type': 'multipart/form-data'
      }
  })
  return response.data.data
}

export async function postModifyPassword(rewordPass) { 
  const response = await http.post('/member/modifyPassword',rewordPass)
  return response.data.data
}

export async function getDeleteAccount() { 
  const response = await http.get('/member/deleteAccount')
  return response.data.data
}
