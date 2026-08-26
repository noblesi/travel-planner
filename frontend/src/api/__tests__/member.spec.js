import { beforeEach, describe, expect, it, vi } from 'vitest'

import http from '@/api/http'
import {
  changeMyPassword,
  getMyProfile,
  updateProfileImage,
  updateMyProfile,
  withdrawMyAccount,
} from '@/api/member'
import { clearCsrfTokenCache } from '@/api/http'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
  clearCsrfTokenCache: vi.fn(),
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('member api', () => {
  it('인증된 현재 회원의 프로필을 조회한다', async () => {
    const profile = { memberId: '1', nickname: '여행자' }
    http.get.mockResolvedValue({ data: { data: profile } })

    await expect(getMyProfile()).resolves.toEqual(profile)
    expect(http.get).toHaveBeenCalledWith('/members/me')
  })

  it('인증된 현재 회원의 수정 가능한 프로필을 저장한다', async () => {
    const payload = { name: '김여행', nickname: '여행자', genderCode: 'N' }
    const profile = { memberId: '1', ...payload }
    http.patch.mockResolvedValue({ data: { data: profile } })

    await expect(updateMyProfile(payload)).resolves.toEqual(profile)
    expect(http.patch).toHaveBeenCalledWith('/members/me', payload)
  })

  it('현재 비밀번호로 회원탈퇴한 뒤 CSRF 캐시를 비운다', async () => {
    http.delete.mockResolvedValue({ data: { data: null } })

    await expect(withdrawMyAccount('current-password')).resolves.toBeNull()
    expect(http.delete).toHaveBeenCalledWith('/members/me', {
      data: { currentPassword: 'current-password' },
    })
    expect(clearCsrfTokenCache).toHaveBeenCalledOnce()
  })

  it('현재 비밀번호와 새 비밀번호로 비밀번호를 변경한다', async () => {
    const payload = { currentPassword: 'current-password', newPassword: 'new-password-value' }
    http.patch.mockResolvedValue({ data: { data: null } })

    await expect(changeMyPassword(payload)).resolves.toBeNull()
    expect(http.patch).toHaveBeenCalledWith('/members/me/password', payload)
  })

  it('프로필 이미지를 multipart 요청으로 변경한다', async () => {
    const file = new File(['image'], 'avatar.png', { type: 'image/png' })
    const profile = { memberId: '1', profileImageUrl: '/uploads/profile/avatar.png' }
    http.patch.mockResolvedValue({ data: { data: profile } })

    await expect(updateProfileImage(file)).resolves.toEqual(profile)
    const [url, formData] = http.patch.mock.calls[0]
    expect(url).toBe('/members/me/profile-image')
    expect(formData).toBeInstanceOf(FormData)
    expect(formData.get('file')).toBe(file)
  })
})
