import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const tempUserInfo = {
    tempEmail : "",
    tempPassword : "",
    tempBirth : "",
    tempName : "",
    tempGender : "",
    tempPhone : ""
  }
  
  const setStep1Data = (email, password) => {
    tempUserInfo.tempEmail = email
    tempUserInfo.tempPassword = password
  }

  const setStep2Data = (birth, name, gender, phone) => {
    tempUserInfo.tempBirth = birth
    tempUserInfo.tempName = name
    tempUserInfo.tempGender = gender
    tempUserInfo.tempPhone = phone
  }

  const clearData = () => {
    tempUserInfo.tempEmail = ''
    tempUserInfo.tempPassword = ''
    tempUserInfo.tempBirth = ''
    tempUserInfo.tempName = ''
    tempUserInfo.tempGender = ''
    tempUserInfo.tempPhone = ''
  }

  return { tempUserInfo, setStep1Data, setStep2Data, clearData }
}, {
  persist: true //localStorage에 자동 저장
})