import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import defaultPlanThumbnail from '@/assets/plan/default-plan-thumbnail.svg'
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

function mountView(pinia = createPinia()) {
  return mount(PlanSearchView, {
    global: {
      plugins: [pinia],
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
    page: 1,
    size: 8,
    totalCount: 1,
    totalPages: 1,
    hasNext: false,
    plans: [publicPlan],
  })
})

describe('PlanSearchView', () => {
  it('공개 일정을 불러와 카드를 표시하고 상세로 이동한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(searchPublicPlansMock).toHaveBeenCalledWith({ keyword: '', page: 1, size: 8 })
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
    expect(searchPublicPlansMock).toHaveBeenCalledWith({ keyword: '서울', page: 1, size: 8 })

    wrapper.unmount()
  })

  it('더 보기를 누르면 다음 서버 페이지를 이어 붙인다', async () => {
    searchPublicPlansMock
      .mockResolvedValueOnce({
        keyword: '',
        page: 1,
        size: 8,
        totalCount: 2,
        totalPages: 2,
        hasNext: true,
        plans: [publicPlan],
      })
      .mockResolvedValueOnce({
        keyword: '',
        page: 2,
        size: 8,
        totalCount: 2,
        totalPages: 2,
        hasNext: false,
        plans: [{ ...publicPlan, planId: '102', title: '부산 바다 여행' }],
      })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('.more-btn').trigger('click')
    await flushPromises()

    expect(searchPublicPlansMock).toHaveBeenLastCalledWith({ keyword: '', page: 2, size: 8 })
    expect(wrapper.text()).toContain('서울 미식 여행')
    expect(wrapper.text()).toContain('부산 바다 여행')
    expect(replaceMock).toHaveBeenLastCalledWith({ query: { page: 2 } })

    wrapper.unmount()
  })

  it('상세에서 돌아오면 같은 검색 페이지를 캐시로 복원한다', async () => {
    const pinia = createPinia()
    searchPublicPlansMock
      .mockResolvedValueOnce({
        keyword: '',
        page: 1,
        size: 8,
        totalCount: 2,
        totalPages: 2,
        hasNext: true,
        plans: [publicPlan],
      })
      .mockResolvedValueOnce({
        keyword: '',
        page: 2,
        size: 8,
        totalCount: 2,
        totalPages: 2,
        hasNext: false,
        plans: [{ ...publicPlan, planId: '102', title: '부산 바다 여행' }],
      })

    const firstView = mountView(pinia)
    await flushPromises()
    await firstView.get('.more-btn').trigger('click')
    await flushPromises()

    route.query = { page: '2' }
    firstView.unmount()
    searchPublicPlansMock.mockClear()

    const restoredView = mountView(pinia)
    await flushPromises()

    expect(searchPublicPlansMock).not.toHaveBeenCalled()
    expect(restoredView.text()).toContain('서울 미식 여행')
    expect(restoredView.text()).toContain('부산 바다 여행')

    restoredView.unmount()
  })

  it('더 보기 도중 새 검색을 실행하면 늦은 이전 응답을 무시한다', async () => {
    let resolveOldPage
    searchPublicPlansMock
      .mockResolvedValueOnce({
        keyword: '',
        page: 1,
        size: 8,
        totalCount: 2,
        totalPages: 2,
        hasNext: true,
        plans: [publicPlan],
      })
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveOldPage = resolve
          }),
      )
      .mockResolvedValueOnce({
        keyword: '부산',
        page: 1,
        size: 8,
        totalCount: 1,
        totalPages: 1,
        hasNext: false,
        plans: [{ ...publicPlan, planId: '201', title: '부산 새 검색 결과' }],
      })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('.more-btn').trigger('click')
    await Promise.resolve()

    await wrapper.get('.search-input').setValue('부산')
    await wrapper.get('.search-input').trigger('keyup.enter')
    await flushPromises()

    expect(wrapper.text()).toContain('부산 새 검색 결과')

    resolveOldPage({
      keyword: '',
      page: 2,
      size: 8,
      totalCount: 2,
      totalPages: 2,
      hasNext: false,
      plans: [{ ...publicPlan, planId: '102', title: '늦게 도착한 제주 일정' }],
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('늦게 도착한 제주 일정')
    expect(wrapper.text()).toContain('부산 새 검색 결과')

    wrapper.unmount()
  })

  it('검색 도중 초기화하면 늦은 검색 응답을 무시한다', async () => {
    let resolveOldSearch
    searchPublicPlansMock
      .mockResolvedValueOnce({
        keyword: '',
        page: 1,
        size: 8,
        totalCount: 1,
        totalPages: 1,
        hasNext: false,
        plans: [publicPlan],
      })
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveOldSearch = resolve
          }),
      )
      .mockResolvedValueOnce({
        keyword: '',
        page: 1,
        size: 8,
        totalCount: 1,
        totalPages: 1,
        hasNext: false,
        plans: [{ ...publicPlan, planId: '301', title: '초기화된 전체 일정' }],
      })

    const wrapper = mountView()
    await flushPromises()
    await wrapper.get('.search-input').setValue('경주')
    await wrapper.get('.search-input').trigger('keyup.enter')
    await Promise.resolve()

    window.dispatchEvent(new CustomEvent('plan-search:reset'))
    await flushPromises()

    resolveOldSearch({
      keyword: '경주',
      page: 1,
      size: 8,
      totalCount: 1,
      totalPages: 1,
      hasNext: false,
      plans: [{ ...publicPlan, planId: '302', title: '늦게 도착한 경주 일정' }],
    })
    await flushPromises()

    expect(wrapper.text()).toContain('초기화된 전체 일정')
    expect(wrapper.text()).not.toContain('늦게 도착한 경주 일정')

    wrapper.unmount()
  })

  it('썸네일이 없거나 로딩에 실패하면 로컬 기본 이미지를 사용한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('.card-img').attributes('src')).toBe(defaultPlanThumbnail)
    expect(wrapper.html()).not.toContain('picsum.photos')
    wrapper.unmount()

    searchPublicPlansMock.mockResolvedValueOnce({
      keyword: '',
      page: 1,
      size: 8,
      totalCount: 1,
      totalPages: 1,
      hasNext: false,
      plans: [{ ...publicPlan, thumbnailImageUrl: 'https://images.example/broken.jpg' }],
    })
    const failedImageView = mountView()
    await flushPromises()

    const image = failedImageView.get('.card-img')
    await image.trigger('error')
    expect(image.attributes('src')).toBe(defaultPlanThumbnail)

    failedImageView.unmount()
  })

  it('조회가 실패하면 재시도 가능한 오류 상태를 표시한다', async () => {
    searchPublicPlansMock.mockRejectedValue(new Error('network error'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('공개 일정을 불러오지 못했어요')

    wrapper.unmount()
  })
})
