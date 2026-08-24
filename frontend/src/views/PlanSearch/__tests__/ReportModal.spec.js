import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ReportModal from '@/views/PlanSearch/ReportModal.vue'

const { reportPlanMock } = vi.hoisted(() => ({
  reportPlanMock: vi.fn(),
}))

vi.mock('@/api/planSearch', () => ({
  reportPlan: reportPlanMock,
}))

const BaseModalStub = {
  template: '<section><slot /><footer><slot name="footer" /></footer></section>',
}

function mountModal() {
  return mount(ReportModal, {
    props: { planId: '501' },
    global: { stubs: { BaseModal: BaseModalStub } },
  })
}

function deferred() {
  let resolve
  const promise = new Promise((resolvePromise) => {
    resolve = resolvePromise
  })
  return { promise, resolve }
}

beforeEach(() => {
  vi.clearAllMocks()
  reportPlanMock.mockResolvedValue(undefined)
})

describe('ReportModal', () => {
  it('선택한 신고 사유와 상세 내용을 전송하고 완료 상태를 표시한다', async () => {
    const wrapper = mountModal()
    const submitButton = () =>
      wrapper.findAll('button').find((button) => button.text().includes('신고'))

    expect(submitButton().attributes()).toHaveProperty('disabled')
    await wrapper.get('input[value="SPAM"]').setValue(true)
    await wrapper.get('#report-detail').setValue('반복 광고 게시물입니다.')
    await submitButton().trigger('click')
    await flushPromises()

    expect(reportPlanMock).toHaveBeenCalledWith('501', {
      reason: 'SPAM',
      detail: '반복 광고 게시물입니다.',
    })
    expect(wrapper.text()).toContain('신고가 접수됐어요.')
  })

  it('신고 요청 중에는 중복 제출을 차단한다', async () => {
    const request = deferred()
    reportPlanMock.mockReturnValue(request.promise)
    const wrapper = mountModal()
    await wrapper.get('input[value="FALSE_INFO"]').setValue(true)

    const button = wrapper.findAll('button').find((candidate) => candidate.text() === '신고하기')
    await button.trigger('click')
    await button.trigger('click')

    expect(reportPlanMock).toHaveBeenCalledOnce()
    expect(button.attributes()).toHaveProperty('disabled')
    expect(button.text()).toBe('접수 중...')

    request.resolve(undefined)
    await flushPromises()
  })

  it('API 실패 메시지를 표시하고 다시 제출할 수 있게 한다', async () => {
    reportPlanMock.mockRejectedValueOnce({
      response: { status: 409, data: { message: '이미 신고한 여행 플랜입니다.' } },
    })
    const wrapper = mountModal()
    await wrapper.get('input[value="OTHER"]').setValue(true)

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '신고하기')
      .trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('이미 신고한 여행 플랜입니다.')
    expect(
      wrapper
        .findAll('button')
        .find((button) => button.text() === '신고하기')
        .attributes(),
    ).not.toHaveProperty('disabled')
  })
})
