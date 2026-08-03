import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getAuthenticationSession,
  loginWithLocalAccount,
  logoutAuthenticationSession,
} from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref(null)
  const initialized = ref(false)
  const pending = ref(false)
  const errorMessage = ref('')
  const isAuthenticated = computed(() => currentUser.value !== null)
  let restorePromise = null

  function setCurrentUser(user) {
    currentUser.value = user
  }

  function clearSession() {
    currentUser.value = null
  }

  async function restoreSession() {
    if (restorePromise) {
      return restorePromise
    }

    restorePromise = (async () => {
      pending.value = true
      try {
        const session = await getAuthenticationSession()
        currentUser.value = session.authenticated ? session.member : null
      } catch {
        currentUser.value = null
      } finally {
        pending.value = false
        initialized.value = true
        restorePromise = null
      }
    })()
    return restorePromise
  }

  async function login(credentials) {
    if (restorePromise) {
      await restorePromise
    }
    pending.value = true
    errorMessage.value = ''
    try {
      const session = await loginWithLocalAccount(credentials)
      currentUser.value = session.member
      initialized.value = true
      return session.member
    } catch (error) {
      currentUser.value = null
      errorMessage.value =
        error.response?.data?.message || '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.'
      throw error
    } finally {
      pending.value = false
    }
  }

  async function logout() {
    if (restorePromise) {
      await restorePromise
    }
    pending.value = true
    errorMessage.value = ''
    try {
      await logoutAuthenticationSession()
      clearSession()
      initialized.value = true
    } catch (error) {
      errorMessage.value =
        error.response?.data?.message || '로그아웃하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      throw error
    } finally {
      pending.value = false
    }
  }

  return {
    currentUser,
    isAuthenticated,
    initialized,
    pending,
    errorMessage,
    setCurrentUser,
    clearSession,
    restoreSession,
    login,
    logout,
  }
})
