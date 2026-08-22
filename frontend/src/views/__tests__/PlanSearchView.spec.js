import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import defaultPlanThumbnail from '@/assets/plan/default-plan-thumbnail.svg'
import PlanSearchView from '@/views/PlanSearch/PlanSearchView.vue'

const { pushMock, replaceMock, route, getPlanListMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
  replaceMock: vi.fn(),
  route: { query: {} },
  getPlanListMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, replace: replaceMock }),
  useRoute: () => route,
}))

// 화면이 신규 공개 탐색 계약을 사용하므로 테스트도 동일 모듈과 응답 필드를 mock해 회귀를 정확히 검출한다.
vi.mock('@/api/planSearch', () => ({
  getPlanList: getPlanListMock,
}))

const publicPlan = {
  planId: '101',
  title: '서울 미식 여행',
  region: '서울특별시',
  days: 2,
  likeCount: 12,
  viewCount: 345,
  authorName: '길동',
  authorImage: null,
  thumbnailImage: null,
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
  getPlanListMock.mockResolvedValue({
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

    expect(getPlanListMock).toHaveBeenCalledWith({ keyword: '', page: 1, size: 8 })
    expect(wrapper.text()).toContain('서울 미식 여행')

    await wrapper.get('.card').trigger('click')
    expect(pushMock).toHaveBeenCalledWith({ name: 'plan-detail', params: { id: '101' } })

    wrapper.unmount()
  })

  it('검색어를 정리해 API와 URL query에 반영한다', async () => {
    const wrapper = mountView()
    await flushPromises()
    getPlanListMock.mockClear()

    await wrapper.get('.search-input').setValue('  서울  ')
    await wrapper.get('.search-input').trigger('keyup.enter')
    await flushPromises()

    expect(replaceMock).toHaveBeenCalledWith({ query: { keyword: '서울' } })
    expect(getPlanListMock).toHaveBeenCalledWith({ keyword: '서울', page: 1, size: 8 })

    wrapper.unmount()
  })

  it('직접 접근한 누적 25페이지를 2개의 batch 요청으로 복원한다', async () => {
    route.query = { keyword: '제주', page: '25' }
    getPlanListMock.mockImplementation(({ page }) =>
      Promise.resolve({
        page,
        totalCount: 200,
        hasNext: page < 2,
        plans: Array.from({ length: 100 }, (_, index) => ({
          ...publicPlan,
          planId: String((page - 1) * 100 + index + 1),
        })),
      }),
    )

    const wrapper = mountView()
    await flushPromises()

    expect(getPlanListMock).toHaveBeenCalledTimes(2)
    expect(getPlanListMock).toHaveBeenNthCalledWith(1, {
      keyword: '제주',
      page: 1,
      size: 100,
    })
    expect(getPlanListMock).toHaveBeenNthCalledWith(2, {
      keyword: '제주',
      page: 2,
      size: 100,
    })
    expect(wrapper.findAll('.card')).toHaveLength(200)

    wrapper.unmount()
  })

  it('더 보기를 누르면 다음 서버 페이지를 이어 붙인다', async () => {
    getPlanListMock
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

    expect(getPlanListMock).toHaveBeenLastCalledWith({ keyword: '', page: 2, size: 8 })
    expect(wrapper.text()).toContain('서울 미식 여행')
    expect(wrapper.text()).toContain('부산 바다 여행')
    expect(replaceMock).toHaveBeenLastCalledWith({ query: { page: 2 } })

    wrapper.unmount()
  })

  it('상세에서 돌아오면 같은 검색 페이지를 캐시로 복원한다', async () => {
    const pinia = createPinia()
    getPlanListMock
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
    getPlanListMock.mockClear()

    const restoredView = mountView(pinia)
    await flushPromises()

    expect(getPlanListMock).not.toHaveBeenCalled()
    expect(restoredView.text()).toContain('서울 미식 여행')
    expect(restoredView.text()).toContain('부산 바다 여행')

    restoredView.unmount()
  })

  it('더 보기 도중 새 검색을 실행하면 늦은 이전 응답을 무시한다', async () => {
    let resolveOldPage
    getPlanListMock
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
    getPlanListMock
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

    getPlanListMock.mockResolvedValueOnce({
      keyword: '',
      page: 1,
      size: 8,
      totalCount: 1,
      totalPages: 1,
      hasNext: false,
      plans: [{ ...publicPlan, thumbnailImage: 'https://images.example/broken.jpg' }],
    })
    const failedImageView = mountView()
    await flushPromises()

    const image = failedImageView.get('.card-img')
    await image.trigger('error')
    expect(image.attributes('src')).toBe(defaultPlanThumbnail)

    failedImageView.unmount()
  })

  it('조회가 실패하면 재시도 가능한 오류 상태를 표시한다', async () => {
    getPlanListMock.mockRejectedValue(new Error('network error'))

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('공개 일정을 불러오지 못했어요')

    wrapper.unmount()
  })
})
