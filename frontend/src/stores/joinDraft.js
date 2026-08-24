import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useJoinDraftStore = defineStore('joinDraft', () => {
  const hasCredentials = ref(false)
  let credentials = null

  function beginRegistration({ email, password }) {
    credentials = { email, password }
    hasCredentials.value = true
  }

  function buildRegistrationPayload(profile) {
    if (!credentials) {
      throw new Error('회원가입 1단계 정보가 없습니다.')
    }

    return {
      ...credentials,
      ...profile,
    }
  }

  function clearRegistration() {
    credentials = null
    hasCredentials.value = false
  }

  return {
    hasCredentials,
    beginRegistration,
    buildRegistrationPayload,
    clearRegistration,
  }
})
