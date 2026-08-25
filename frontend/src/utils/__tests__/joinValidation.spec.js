import { describe, expect, it } from 'vitest'

import {
  normalizeJoinEmail,
  validateJoinCredentials,
  validateJoinProfile,
} from '@/utils/joinValidation'

describe('join validation', () => {
  it('이메일을 backend 계약과 동일하게 정규화한다', () => {
    expect(normalizeJoinEmail('  MEMBER@Example.COM ')).toBe('member@example.com')
  })

  it('비밀번호 길이와 확인 값을 검증한다', () => {
    expect(
      validateJoinCredentials({
        email: 'member@example.com',
        password: 'short',
        passwordConfirmation: 'short',
      }),
    ).toContain('10자 이상 72자 이하')

    expect(
      validateJoinCredentials({
        email: 'member@example.com',
        password: 'password-value',
        passwordConfirmation: 'different-value',
      }),
    ).toContain('일치하지 않습니다')
  })

  it('존재하지 않거나 오늘 이후인 생년월일을 거부한다', () => {
    const profile = {
      name: '홍길동',
      birth: '20260230',
      phone: '010-1234-5678',
      gender: 'N',
      privacy: true,
    }

    expect(validateJoinProfile(profile, new Date(2026, 7, 22))).toContain('생년월일')
    expect(validateJoinProfile({ ...profile, birth: '20260822' }, new Date(2026, 7, 22))).toContain(
      '생년월일',
    )
    expect(validateJoinProfile({ ...profile, birth: '20000101' }, new Date(2026, 7, 22))).toBe('')
  })
})
