import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import ImportModal from '@/views/PlanSearch/ImportModal.vue'

const { copyPlanMock, pushMock } = vi.hoisted(() => ({
  copyPlanMock: vi.fn(),
  pushMock: vi.fn(),
}))

vi.mock('@/api/planSearch', () => ({
  copyPlan: copyPlanMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

const plan = {
  id: '501',
  title: '서울 여행',
  days: [{ dayNumber: 1 }, { dayNumber: 2 }],
}

const BaseModalStub = {
  template: '<section><slot /><footer><slot name="footer" /></footer></section>',
}

function mountModal() {
  return mount(ImportModal, {
    props: { plan },
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

async function fillRequiredFields(wrapper) {
  const [titleInput, startDateInput, endDateInput] = wrapper.findAll('input')
  await titleInput.setValue('서울 여행 복사본')
  await startDateInput.setValue('2026-09-01')
  await endDateInput.setValue('2026-09-02')
}

beforeEach(() => {
  vi.clearAllMocks()
  copyPlanMock.mockResolvedValue('701')
  pushMock.mockResolvedValue(undefined)
})

describe('ImportModal', () => {
  it('필수 정보를 전송하고 생성한 플랜 편집 화면으로 이동한다', async () => {
    const wrapper = mountModal()
    const actionButton = () =>
      wrapper.findAll('button').find((button) => button.text().includes('플랜'))

    expect(actionButton().attributes()).toHaveProperty('disabled')
    await fillRequiredFields(wrapper)
    expect(actionButton().attributes()).not.toHaveProperty('disabled')

    await actionButton().trigger('click')
    await flushPromises()

    expect(copyPlanMock).toHaveBeenCalledWith('501', {
      title: '서울 여행 복사본',
      startDate: '2026-09-01',
      endDate: '2026-09-02',
    })
    expect(wrapper.text()).toContain('새 플랜이 만들어졌어요!')

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '내 플랜 보러 가기')
      .trigger('click')
    expect(pushMock).toHaveBeenCalledWith({
      name: 'plan-editor',
      params: { planId: '701' },
    })
  })

  it('생성 요청 중에는 중복 제출을 차단한다', async () => {
    const request = deferred()
    copyPlanMock.mockReturnValue(request.promise)
    const wrapper = mountModal()
    await fillRequiredFields(wrapper)

    const button = wrapper.findAll('button').find((candidate) => candidate.text() === '플랜 만들기')
    await button.trigger('click')
    await button.trigger('click')

    expect(copyPlanMock).toHaveBeenCalledOnce()
    expect(button.attributes()).toHaveProperty('disabled')
    expect(button.text()).toBe('만드는 중...')

    request.resolve('701')
    await flushPromises()
  })

  it('API 실패 메시지를 표시하고 다시 제출할 수 있게 한다', async () => {
    copyPlanMock.mockRejectedValueOnce({ response: { status: 401 } })
    const wrapper = mountModal()
    await fillRequiredFields(wrapper)

    await wrapper
      .findAll('button')
      .find((button) => button.text() === '플랜 만들기')
      .trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('로그인 후 일정을 가져올 수 있어요.')
    expect(
      wrapper
        .findAll('button')
        .find((button) => button.text() === '플랜 만들기')
        .attributes(),
    ).not.toHaveProperty('disabled')
  })
})
