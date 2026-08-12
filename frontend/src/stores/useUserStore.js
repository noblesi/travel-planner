import { defineStore } from 'pinia'
import { ref } from 'vue'


export const useUserStore = defineStore('user', () => {
  const userInfo = ref({
    email: '',
    password: ''
  })

  const setUserInfo = (email, password) => {
    userInfo.value.email = email
    userInfo.value.password = password
  }

  const clearData = () => {
    userInfo.value.email = ''
    userInfo.value.password = ''
  }

  return {
    userInfo,
    setUserInfo,
    clearData
  }
})
