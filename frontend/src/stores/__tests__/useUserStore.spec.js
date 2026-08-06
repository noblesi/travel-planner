import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useUserStore } from '@/stores/useUserStore'

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('user store', () => {
  it('회원가입 단계별 입력값을 저장한다', () => {
    const store = useUserStore()

    store.setStep1Data('member@example.com', 'password-value')
    store.setStep2Data('20000101', '홍길동', 'F', '010-1234-5678')

    expect(store.tempEmail).toBe('member@example.com')
    expect(store.tempPassword).toBe('password-value')
    expect(store.tempBirth).toBe('20000101')
    expect(store.tempName).toBe('홍길동')
    expect(store.tempGender).toBe('F')
    expect(store.tempPhone).toBe('010-1234-5678')
  })

  it('임시 회원가입 정보를 모두 초기화한다', () => {
    const store = useUserStore()
    store.setStep1Data('member@example.com', 'password-value')
    store.setStep2Data('20000101', '홍길동', 'F', '010-1234-5678')

    store.clearData()

    expect(store.tempEmail).toBe('')
    expect(store.tempPassword).toBe('')
    expect(store.tempBirth).toBe('')
    expect(store.tempName).toBe('')
    expect(store.tempGender).toBe('')
    expect(store.tempPhone).toBe('')
  })
})
