import { defineStore } from 'pinia'


export const useUserStore = defineStore('user', () => {
  const userInfo = {
    email : "",
    password : "",
  }
}, {
  actions: {
    setStep1Data(email, password) { 
      this.userInfo.email = email
      this.userInfo.password = password
    },
    clearData() {
      this.userInfo.email = ''
      this.userInfo.password = ''
    }
  }
})