import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  acceptPlanInvitation,
  createPlanInvitations,
  getPlanInvitation,
} from '@/api/invitations'
import http from '@/api/http'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('plan invitation API', () => {
  it('플랜 초대 링크 생성 요청을 전송한다', async () => {
    const payload = { inviteeEmails: ['friend@example.com'] }
    const response = { planId: '101', invitations: [{ invitationId: '201' }] }
    http.post.mockResolvedValue({ data: { data: response } })

    await expect(createPlanInvitations('101', payload)).resolves.toEqual(response)
    expect(http.post).toHaveBeenCalledWith('/plans/101/invitations', payload)
  })

  it('Token을 경로에 포함해 초대 조회와 수락 요청을 전송한다', async () => {
    const invitation = { invitationId: '201', planId: '101' }
    const accepted = { invitationId: '201', planId: '101', status: 'ACCEPTED' }
    http.get.mockResolvedValue({ data: { data: invitation } })
    http.post.mockResolvedValue({ data: { data: accepted } })

    await expect(getPlanInvitation('token/value')).resolves.toEqual(invitation)
    await expect(acceptPlanInvitation('token/value')).resolves.toEqual(accepted)

    expect(http.get).toHaveBeenCalledWith('/plan-invitations/token%2Fvalue')
    expect(http.post).toHaveBeenCalledWith('/plan-invitations/token%2Fvalue/accept')
  })
})
