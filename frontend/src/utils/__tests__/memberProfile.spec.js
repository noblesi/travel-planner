import { describe, expect, it } from 'vitest'

import {
  createProfileForm,
  formatBirthDate,
  mapServerFieldErrors,
  toProfileUpdatePayload,
  validatePasswordChange,
  validateProfileForm,
  validateWithdrawalPassword,
} from '@/utils/memberProfile'

describe('member profile utilities', () => {
  it('profile을 편집 form으로 복사하고 API payload를 정규화한다', () => {
    const form = createProfileForm({
      name: ' 김여행 ',
      nickname: ' 여행자 ',
      genderCode: 'F',
      birthDate: '',
      phoneNumber: ' 010-1234-5678 ',
    })

    expect(toProfileUpdatePayload(form)).toEqual({
      name: '김여행',
      nickname: '여행자',
      genderCode: 'F',
      birthDate: null,
      phoneNumber: '010-1234-5678',
    })
  })

  it('profile 입력 경계와 미래 생년월일을 검증한다', () => {
    const errors = validateProfileForm(
      {
        name: '',
        nickname: '여행자',
        genderCode: 'INVALID',
        birthDate: '2026-08-22',
        phoneNumber: 'invalid-phone',
      },
      '2026-08-22',
    )

    expect(errors).toEqual({
      name: '이름을 입력해 주세요.',
      genderCode: '성별을 선택해 주세요.',
      birthDate: '생년월일은 오늘보다 이전이어야 합니다.',
      phoneNumber: '전화번호 형식이 올바르지 않습니다.',
    })
  })

  it('서버 field error 중 유효한 항목만 form error로 변환한다', () => {
    expect(
      mapServerFieldErrors([
        { field: 'nickname', message: '이미 사용 중입니다.' },
        { field: '', message: '무시' },
        null,
      ]),
    ).toEqual({ nickname: '이미 사용 중입니다.' })
  })

  it('표시 날짜와 비밀번호 workflow를 검증한다', () => {
    expect(formatBirthDate('1998-04-02')).toBe('1998년 4월 2일')
    expect(
      validatePasswordChange({
        currentPassword: 'current-password',
        newPassword: 'new-password-value',
        newPasswordConfirm: 'different-password',
      }),
    ).toContain('일치하지 않습니다')
    expect(validateWithdrawalPassword('short')).toContain('현재 비밀번호')
  })
})
