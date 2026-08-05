import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const tempEmail = ref('')
  const tempPassword = ref('')
  const tempBirth = ref('')
  const tempName = ref('')
  const tempGender = ref('')
  const tempPhone = ref('')

  const setStep1Data = (email, password) => {
    tempEmail.value = email
    tempPassword.value = password
  }

  const setStep2Data = (birth, name, gender, phone) => {
    tempBirth.value = birth
    tempName.value = name
    tempGender.value = gender
    tempPhone.value = phone
  }

  const clearData = () => {
    tempEmail.value = ''
    tempPassword.value = ''
    tempBirth.value = ''
    tempName.value = ''
    tempGender.value = ''
    tempPhone.value = ''
  }

  return {
    tempEmail,
    tempPassword,
    tempBirth,
    tempName,
    tempGender,
    tempPhone,
    setStep1Data,
    setStep2Data,
    clearData,
  }
})
