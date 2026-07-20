import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref(null)
  const isAuthenticated = computed(() => currentUser.value !== null)

  function setCurrentUser(user) {
    currentUser.value = user
  }

  function clearSession() {
    currentUser.value = null
  }

  return { currentUser, isAuthenticated, setCurrentUser, clearSession }
})
