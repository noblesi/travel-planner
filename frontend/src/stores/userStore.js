import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const tempEmail = ref('')
  const tempPassword = ref('')

  const setStep1Data = (email, password) => {
    tempEmail.value = email
    tempPassword.value = password
  }

  const clearData = () => {
    tempEmail.value = ''
    tempPassword.value = ''
  }

  return { tempEmail, tempPassword, setStep1Data, clearData }
})