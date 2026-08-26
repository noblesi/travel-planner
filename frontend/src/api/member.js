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

export async function updateProfileImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.patch('/members/me/profile-image', formData)
  return response.data.data
}

export async function withdrawMyAccount(currentPassword) {
  const response = await http.delete('/members/me', {
    data: { currentPassword },
  })
  clearCsrfTokenCache()
  return response.data.data
}
