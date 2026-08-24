import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useToastStore } from '@/stores/toast'
import PlanDetailView from '@/views/PlanSearch/PlanDetailView.vue'

const { getPlanDetailMock, toggleLikeMock } = vi.hoisted(() => ({
  getPlanDetailMock: vi.fn(),
  toggleLikeMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '501' }, query: {} }),
  useRouter: () => ({ back: vi.fn(), replace: vi.fn() }),
}))

vi.mock('@/api/planSearch', () => ({
  getPlanDetail: getPlanDetailMock,
  toggleLike: toggleLikeMock,
}))

function mountView() {
  const pinia = createPinia()
  const wrapper = mount(PlanDetailView, {
    props: { id: '501' },
    global: {
      plugins: [pinia],
      stubs: {
        DefaultLayout: { template: '<main><slot /></main>' },
        PublicPlanDetailHeader: {
          props: ['plan', 'likePending'],
          template:
            '<div><span class="plan-title">{{ plan.title }}</span><span class="like-count">{{ plan.likeCount }}</span><button class="like-button" :disabled="likePending" @click="$emit(\'toggle-like\')">좋아요</button></div>',
        },
        PublicPlanSchedule: true,
        PublicPlanDayMap: true,
        ReportModal: true,
        ImportModal: true,
      },
    },
  })
  return { pinia, wrapper }
}

function publicPlanDetail(planId, title) {
  return {
    planId,
    title,
    authorName: '여행자',
    startDate: '2026-08-10',
    endDate: '2026-08-10',
    likeCount: 3,
    viewCount: 10,
    liked: false,
    days: [{ dayNumber: 1, visitDate: '2026-08-10', places: [] }],
  }
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

beforeEach(() => {
  vi.clearAllMocks()
  getPlanDetailMock.mockResolvedValue(publicPlanDetail('501', '공개 플랜'))
  toggleLikeMock.mockReset().mockResolvedValue(true)
})

describe('PlanDetailView', () => {
  it('이전 플랜 응답이 먼저 도착해도 마지막 route 조회가 끝날 때까지 적용하지 않는다', async () => {
    const firstRequest = deferred()
    const secondRequest = deferred()
    getPlanDetailMock
      .mockReset()
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    const { wrapper } = mountView()

    await wrapper.setProps({ id: '502' })
    expect(getPlanDetailMock).toHaveBeenNthCalledWith(1, '501')
    expect(getPlanDetailMock).toHaveBeenNthCalledWith(2, '502')

    firstRequest.resolve(publicPlanDetail('501', '이전 공개 플랜'))
    await flushPromises()

    expect(wrapper.get('[role="status"]').text()).toContain('불러오는 중')
    expect(wrapper.find('.plan-title').exists()).toBe(false)

    secondRequest.resolve(publicPlanDetail('502', '최신 공개 플랜'))
    await flushPromises()

    expect(wrapper.get('.plan-title').text()).toBe('최신 공개 플랜')
    wrapper.unmount()
  })

  it('최신 플랜 성공 후 도착한 이전 요청 실패가 화면을 오류 상태로 덮지 않는다', async () => {
    const firstRequest = deferred()
    const secondRequest = deferred()
    getPlanDetailMock
      .mockReset()
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    const { wrapper } = mountView()

    await wrapper.setProps({ id: '502' })
    secondRequest.resolve(publicPlanDetail('502', '최신 공개 플랜'))
    await flushPromises()
    firstRequest.reject(new Error('old request failed'))
    await flushPromises()

    expect(wrapper.get('.plan-title').text()).toBe('최신 공개 플랜')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('좋아요 요청 중 버튼을 잠가 중복 API 호출과 count 중복 반영을 막는다', async () => {
    const likeRequest = deferred()
    toggleLikeMock.mockReturnValueOnce(likeRequest.promise)
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.get('.like-button').trigger('click')
    expect(wrapper.get('.like-button').attributes()).toHaveProperty('disabled')
    await wrapper.get('.like-button').trigger('click')
    expect(toggleLikeMock).toHaveBeenCalledOnce()

    likeRequest.resolve(true)
    await flushPromises()

    expect(wrapper.get('.like-count').text()).toBe('4')
    expect(wrapper.get('.like-button').attributes()).not.toHaveProperty('disabled')
    wrapper.unmount()
  })

  it('이전 플랜의 좋아요 응답이 route 변경 후 새 플랜을 수정하지 않는다', async () => {
    const likeRequest = deferred()
    getPlanDetailMock
      .mockReset()
      .mockResolvedValueOnce(publicPlanDetail('501', '이전 공개 플랜'))
      .mockResolvedValueOnce(publicPlanDetail('502', '최신 공개 플랜'))
    toggleLikeMock.mockReturnValueOnce(likeRequest.promise)
    const { wrapper } = mountView()
    await flushPromises()

    await wrapper.get('.like-button').trigger('click')
    await wrapper.setProps({ id: '502' })
    await flushPromises()
    likeRequest.resolve(true)
    await flushPromises()

    expect(wrapper.get('.plan-title').text()).toBe('최신 공개 플랜')
    expect(wrapper.get('.like-count').text()).toBe('3')
    expect(wrapper.get('.like-button').attributes()).not.toHaveProperty('disabled')
    expect(toggleLikeMock).toHaveBeenCalledOnce()
    wrapper.unmount()
  })

  it('좋아요 실패를 blocking alert 대신 공통 Toast로 안내한다', async () => {
    toggleLikeMock
      .mockRejectedValueOnce({ response: { status: 401 } })
      .mockRejectedValueOnce(new Error('network error'))

    const { pinia, wrapper } = mountView()
    await flushPromises()

    await wrapper.get('.like-button').trigger('click')
    await flushPromises()
    expect(useToastStore(pinia).toasts.at(-1)).toMatchObject({
      type: 'info',
      message: '로그인 후 좋아요를 누를 수 있습니다.',
    })

    await wrapper.get('.like-button').trigger('click')
    await flushPromises()
    expect(useToastStore(pinia).toasts.at(-1)).toMatchObject({
      type: 'error',
      message: '좋아요 처리에 실패했어요. 잠시 후 다시 시도해 주세요.',
    })

    useToastStore(pinia).clear()
    wrapper.unmount()
  })
})
