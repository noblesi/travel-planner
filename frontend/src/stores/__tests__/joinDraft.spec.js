import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useJoinDraftStore } from '@/stores/joinDraft'

beforeEach(() => {
  setActivePinia(createPinia())
})

describe('join draft store', () => {
  it('회원가입 credential을 reactive state에 노출하지 않고 payload를 조립한다', () => {
    const store = useJoinDraftStore()

    store.beginRegistration({ email: 'member@example.com', password: 'password-value' })

    expect(store.hasCredentials).toBe(true)
    expect(store.$state).toEqual({ hasCredentials: true })
    expect(
      store.buildRegistrationPayload({ name: '홍길동', privacy: 'Y' }),
    ).toEqual({
      email: 'member@example.com',
      password: 'password-value',
      name: '홍길동',
      privacy: 'Y',
    })
  })

  it('회원가입 draft를 초기화하면 기존 credential을 다시 사용할 수 없다', () => {
    const store = useJoinDraftStore()
    store.beginRegistration({ email: 'member@example.com', password: 'password-value' })

    store.clearRegistration()

    expect(store.hasCredentials).toBe(false)
    expect(() => store.buildRegistrationPayload({ name: '홍길동' })).toThrow(
      '회원가입 1단계 정보가 없습니다.',
    )
  })
})
