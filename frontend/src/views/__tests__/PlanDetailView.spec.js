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
          template: '<button class="like-button" @click="$emit(\'toggle-like\')">좋아요</button>',
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

beforeEach(() => {
  vi.clearAllMocks()
  getPlanDetailMock.mockResolvedValue({
    planId: '501',
    title: '공개 플랜',
    authorName: '여행자',
    startDate: '2026-08-10',
    endDate: '2026-08-10',
    likeCount: 3,
    viewCount: 10,
    liked: false,
    days: [{ dayNumber: 1, visitDate: '2026-08-10', places: [] }],
  })
})

describe('PlanDetailView', () => {
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
