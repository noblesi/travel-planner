import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PlanSearchView from '@/views/PlanSearch/PlanSearchView.vue'

const { pushMock, replaceMock, route, searchPublicPlansMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  replaceMock: vi.fn(),
  route: { query: {} },
  searchPublicPlansMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, replace: replaceMock }),
  useRoute: () => route,
}))

vi.mock('@/api/plans', () => ({
  searchPublicPlans: searchPublicPlansMock,
}))

const publicPlan = {
  planId: '101',
  title: '서울 미식 여행',
  regionName: '서울특별시',
  dayCount: 2,
  likeCount: 12,
  viewCount: 345,
  authorName: '길동',
  authorProfileImageUrl: null,
  thumbnailImageUrl: null,
}

function mountView() {
  return mount(PlanSearchView, {
    global: {
      stubs: {
        DefaultLayout: { template: '<main><slot /></main>' },
      },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  route.query = {}
  searchPublicPlansMock.mockResolvedValue({
    keyword: '',
    totalCount: 1,
    plans: [publicPlan],
  })
})

describe('PlanSearchView', () => {
  it('공개 일정을 불러와 카드를 표시하고 상세로 이동한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(searchPublicPlansMock).toHaveBeenCalledWith({ keyword: '', limit: 100 })
    expect(wrapper.text()).toContain('서울 미식 여행')

    await wrapper.get('.card').trigger('click')
    expect(pushMock).toHaveBeenCalledWith({ name: 'plan-detail', params: { id: '101' } })

    wrapper.unmount()
  })

  it('검색어를 정리해 API와 URL query에 반영한다', async () => {
    const wrapper = mountView()
    await flushPromises()
    searchPublicPlansMock.mockClear()

    await wrapper.get('.search-input').setValue('  서울  ')
    await wrapper.get('.search-input').trigger('keyup.enter')
    await flushPromises()

    expect(replaceMock).toHaveBeenCalledWith({ query: { keyword: '서울' } })
    expect(searchPublicPlansMock).toHaveBeenCalledWith({ keyword: '서울', limit: 100 })

    wrapper.unmount()
  })

  it('조회가 실패하면 재시도 가능한 오류 상태를 표시한다', async () => {
    searchPublicPlansMock.mockRejectedValue(new Error('network error'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('공개 일정을 불러오지 못했어요')

    wrapper.unmount()
  })
})
