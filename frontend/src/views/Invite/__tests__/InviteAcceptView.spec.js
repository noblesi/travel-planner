import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import InviteAcceptView from '@/views/Invite/InviteAcceptView.vue'

const {
  acceptPlanInvitationMock,
  getPlanInvitationMock,
  pushMock,
  route,
} = vi.hoisted(() => ({
  acceptPlanInvitationMock: vi.fn(),
  getPlanInvitationMock: vi.fn(),
  pushMock: vi.fn(),
  route: { query: { token: 'token-1' }, fullPath: '/invite/accept?token=token-1' },
}))

vi.mock('@/api/invitations', () => ({
  acceptPlanInvitation: acceptPlanInvitationMock,
  getPlanInvitation: getPlanInvitationMock,
}))

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ push: pushMock }),
}))

const invitation = {
  invitationId: '201',
  planId: '101',
  planTitle: '서울특별시 여행',
  regionName: '서울특별시',
  startDate: '2026-08-10',
  endDate: '2026-08-11',
  inviteeEmail: 'friend@example.com',
  status: 'PENDING',
}

beforeEach(() => {
  route.query = { token: 'token-1' }
  route.fullPath = '/invite/accept?token=token-1'
  pushMock.mockReset()
  getPlanInvitationMock.mockReset().mockResolvedValue(invitation)
  acceptPlanInvitationMock.mockReset()
})

describe('InviteAcceptView', () => {
  it('초대 정보를 조회하고 수락 후 편집 화면으로 이동한다', async () => {
    acceptPlanInvitationMock.mockResolvedValue({ planId: '101', status: 'ACCEPTED' })
    const wrapper = mount(InviteAcceptView)
    await flushPromises()

    expect(getPlanInvitationMock).toHaveBeenCalledWith('token-1')
    expect(wrapper.text()).toContain('서울특별시 여행')
    expect(wrapper.text()).toContain('friend@example.com')

    await wrapper.get('.accept-btn').trigger('click')
    await flushPromises()

    expect(acceptPlanInvitationMock).toHaveBeenCalledWith('token-1')
    expect(pushMock).toHaveBeenCalledWith({
      name: 'plan-editor',
      params: { planId: '101' },
    })
  })

  it('만료된 Token은 만료 상태를 표시한다', async () => {
    getPlanInvitationMock.mockRejectedValue({ response: { status: 410 } })
    const wrapper = mount(InviteAcceptView)
    await flushPromises()

    expect(wrapper.text()).toContain('링크가 만료됐어요')
  })

  it('로그인이 필요하면 현재 초대 URL을 유지해 로그인 화면으로 보낸다', async () => {
    acceptPlanInvitationMock.mockRejectedValue({ response: { status: 401 } })
    const wrapper = mount(InviteAcceptView)
    await flushPromises()

    await wrapper.get('.accept-btn').trigger('click')
    await flushPromises()

    expect(pushMock).toHaveBeenCalledWith({
      name: 'login',
      query: { redirect: '/invite/accept?token=token-1' },
    })
  })
})
