import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import InviteView from '@/views/Invite/InviteView.vue'

const { createPlanInvitationsMock, pushMock, resolveMock } = vi.hoisted(() => ({
  createPlanInvitationsMock: vi.fn(),
  pushMock: vi.fn(),
  resolveMock: vi.fn(),
}))

vi.mock('@/api/invitations', () => ({
  createPlanInvitations: createPlanInvitationsMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, resolve: resolveMock }),
}))

function mountView() {
  return mount(InviteView, {
    props: { id: '101' },
    global: {
      stubs: {
        DefaultLayout: { template: '<div><slot /></div>' },
      },
    },
  })
}

beforeEach(() => {
  createPlanInvitationsMock.mockReset()
  pushMock.mockReset()
  resolveMock.mockReset().mockReturnValue({ href: '/invite/accept?token=token-1' })
})

describe('InviteView', () => {
  it('이메일 목록으로 초대 링크를 생성한다', async () => {
    createPlanInvitationsMock.mockResolvedValue({
      planId: '101',
      invitations: [
        {
          invitationId: '201',
          inviteeEmail: 'friend@example.com',
          token: 'token-1',
          expiresAt: '2026-08-02T00:00:00Z',
        },
      ],
    })
    const wrapper = mountView()

    await wrapper.get('#invite-email').setValue('Friend@Example.com')
    await wrapper.get('.input-row').trigger('submit')
    await wrapper.get('.send-btn').trigger('click')
    await flushPromises()

    expect(createPlanInvitationsMock).toHaveBeenCalledWith('101', {
      inviteeEmails: ['friend@example.com'],
    })
    expect(wrapper.text()).toContain('초대 링크를 만들었습니다.')
    expect(wrapper.get('.link-row input').element.value).toContain(
      '/invite/accept?token=token-1',
    )
  })

  it('잘못되거나 중복된 이메일을 Client에서 차단한다', async () => {
    const wrapper = mountView()

    await wrapper.get('#invite-email').setValue('invalid')
    await wrapper.get('.input-row').trigger('submit')
    expect(wrapper.text()).toContain('올바른 이메일 형식이 아니에요.')

    await wrapper.get('#invite-email').setValue('friend@example.com')
    await wrapper.get('.input-row').trigger('submit')
    await wrapper.get('#invite-email').setValue('FRIEND@example.com')
    await wrapper.get('.input-row').trigger('submit')

    expect(wrapper.text()).toContain('이미 추가된 이메일이에요.')
    expect(wrapper.findAll('.email-item')).toHaveLength(1)
    expect(createPlanInvitationsMock).not.toHaveBeenCalled()
  })

  it('플랜 편집 화면으로 돌아간다', async () => {
    const wrapper = mountView()

    await wrapper.get('.back-btn').trigger('click')

    expect(pushMock).toHaveBeenCalledWith({
      name: 'plan-editor',
      params: { planId: '101' },
    })
  })
})
