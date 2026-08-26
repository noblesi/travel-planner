import { beforeEach, describe, expect, it, vi } from 'vitest'

import { findEmail, resetRecoveredPassword, verifyPasswordRecovery } from '@/api/find'
import http from '@/api/http'

vi.mock('@/api/http', () => ({
  default: {
    post: vi.fn(),
    patch: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('account recovery api', () => {
  it('회원정보로 마스킹된 이메일을 찾는다', async () => {
    const payload = {
      memberName: '김여행',
      birthDate: '1990-05-12',
      phoneNumber: '010-1234-5678',
    }
    http.post.mockResolvedValue({ data: { data: 't***@example.com' } })

    await expect(findEmail(payload)).resolves.toBe('t***@example.com')
    expect(http.post).toHaveBeenCalledWith('/account-recovery/email', payload)
  })

  it('비밀번호 복구 전 회원정보를 현재 세션에서 확인한다', async () => {
    const payload = {
      email: 'traveler@example.com',
      birthDate: '1990-05-12',
      phoneNumber: '010-1234-5678',
    }
    http.post.mockResolvedValue({ data: { data: null } })

    await expect(verifyPasswordRecovery(payload)).resolves.toBeNull()
    expect(http.post).toHaveBeenCalledWith('/account-recovery/password/verify', payload)
  })

  it('확인된 세션에서만 새 비밀번호를 보낸다', async () => {
    http.patch.mockResolvedValue({ data: { data: null } })

    await expect(resetRecoveredPassword('Recovered-WithTrip-2026!')).resolves.toBeNull()
    expect(http.patch).toHaveBeenCalledWith('/account-recovery/password', {
      newPassword: 'Recovered-WithTrip-2026!',
    })
  })
})
