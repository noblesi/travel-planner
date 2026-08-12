import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useUserStore } from '@/stores/useUserStore'

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('user store', () => {
  it('회원가입 1단계 입력값을 저장한다', () => {
    const store = useUserStore()

    store.setUserInfo('member@example.com', 'password-value')

    expect(store.userInfo).toEqual({
      email: 'member@example.com',
      password: 'password-value',
    })
  })

  it('회원가입 1단계 입력값을 초기화한다', () => {
    const store = useUserStore()
    store.setUserInfo('member@example.com', 'password-value')

    store.clearData()

    expect(store.userInfo).toEqual({
      email: '',
      password: '',
    })
  })
})
