import http from './http'

export async function findEmail(payload) {
  const response = await http.post('/account-recovery/email', payload)
  return response.data.data
}

export async function verifyPasswordRecovery(payload) {
  const response = await http.post('/account-recovery/password/verify', payload)
  return response.data.data
}

export async function resetRecoveredPassword(newPassword) {
  const response = await http.patch('/account-recovery/password', { newPassword })
  return response.data.data
}
