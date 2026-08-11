import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import LoginView from '@/views/loginView/LoginView.vue'
import PlanEditorView from '@/views/PlanEditorView.vue'
import PlanDetailView from '@/views/PlanSearch/PlanDetailView.vue'
import PlanSearchView from '@/views/PlanSearch/PlanSearchView.vue'
import PlanSetupView from '@/views/PlanSetupView.vue'

const {
  addScheduleItemMock,
  createTravelPlanMock,
  getPlanDetailMock,
  getRegionsMock,
  getTravelPlanEditorMock,
  loginWithLocalAccountMock,
  searchPlacesMock,
  getPlanListMock,
  updateTravelPlanDatesMock,
  updateTravelPlanMetadataMock,
} = vi.hoisted(() => ({
  addScheduleItemMock: vi.fn(),
  createTravelPlanMock: vi.fn(),
  getPlanDetailMock: vi.fn(),
  getRegionsMock: vi.fn(),
  getTravelPlanEditorMock: vi.fn(),
  loginWithLocalAccountMock: vi.fn(),
  searchPlacesMock: vi.fn(),
  getPlanListMock: vi.fn(),
  updateTravelPlanDatesMock: vi.fn(),
  updateTravelPlanMetadataMock: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  getAuthenticationSession: vi.fn(),
  loginWithLocalAccount: loginWithLocalAccountMock,
  logoutAuthenticationSession: vi.fn(),
}))

vi.mock('@/api/regions', () => ({
  getRegions: getRegionsMock,
}))

vi.mock('@/api/places', () => ({
  searchPlaces: searchPlacesMock,
}))

vi.mock('@/api/plans', () => ({
  addScheduleItem: addScheduleItemMock,
  createTravelPlan: createTravelPlanMock,
  deleteScheduleItem: vi.fn(),
  getTravelPlanEditor: getTravelPlanEditorMock,
  reorderScheduleItems: vi.fn(),
  updateScheduleItem: vi.fn(),
  updateTravelPlanDates: updateTravelPlanDatesMock,
  updateTravelPlanMetadata: updateTravelPlanMetadataMock,
}))

// 공개 탐색 화면이 planSearch API로 분리되었으므로 사용자 흐름 테스트도 실제 화면과 같은 계약을 사용한다.
vi.mock('@/api/planSearch', () => ({
  getPlanDetail: getPlanDetailMock,
  getPlanList: getPlanListMock,
  toggleLike: vi.fn(),
}))

const EmptyView = defineComponent({ template: '<div />' })
const RouterHost = defineComponent({ components: { RouterView }, template: '<RouterView />' })

const baseEditor = {
  plan: {
    planId: '101',
    title: '서울특별시 여행',
    regionCode: '1',
    regionName: '서울특별시',
    startDate: '2026-08-10',
    endDate: '2026-08-11',
    visibility: 'PUBLIC',
    versionNo: 0,
  },
  days: [
    {
      planDayId: '201',
      dayNo: 1,
      travelDate: '2026-08-10',
      scheduleVersion: 0,
      items: [],
    },
    {
      planDayId: '202',
      dayNo: 2,
      travelDate: '2026-08-11',
      scheduleVersion: 0,
      items: [],
    },
  ],
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: EmptyView },
      { path: '/login', name: 'login', component: LoginView },
      { path: '/join', name: 'join', component: EmptyView },
      { path: '/email-find', name: 'emailFind', component: EmptyView },
      { path: '/password-find', name: 'passwordFind', component: EmptyView },
      { path: '/plans/new', name: 'plan-setup', component: PlanSetupView },
      {
        path: '/plans/:planId/edit',
        name: 'plan-editor',
        component: PlanEditorView,
        props: true,
      },
      { path: '/plans', name: 'plan-search', component: PlanSearchView },
      { path: '/plans/:id', name: 'plan-detail', component: PlanDetailView, props: true },
      { path: '/plans/:id/invite', name: 'invite', component: EmptyView },
    ],
  })
}

async function mountAt(initialRoute) {
  const router = createTestRouter()
  await router.push(initialRoute)
  await router.isReady()
  const wrapper = mount(RouterHost, {
    global: {
      plugins: [createPinia(), router],
      stubs: {
        DefaultLayout: { template: '<main><slot /></main>' },
        KakaoMap: { props: ['places', 'selectedPlaceId'], template: '<div class="map-stub" />' },
      },
    },
  })
  await flushPromises()
  return { router, wrapper }
}

function publicPlan(planId, title) {
  return {
    planId,
    title,
    region: '제주특별자치도',
    days: 2,
    likeCount: 3,
    viewCount: 12,
    authorName: '여행자',
    authorImage: null,
    thumbnailImage: null,
  }
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-08-04T00:00:00+09:00'))
  vi.clearAllMocks()
  loginWithLocalAccountMock.mockResolvedValue({
    authenticated: true,
    member: { memberId: '1', email: 'traveler@example.com', nickname: '여행자' },
  })
  getRegionsMock.mockResolvedValue([{ regionCode: '1', regionName: '서울특별시' }])
  createTravelPlanMock.mockResolvedValue({ planId: '101' })
  getTravelPlanEditorMock.mockResolvedValue(baseEditor)
})

afterEach(() => {
  vi.useRealTimers()
})

describe('여행 플랜 사용자 흐름', () => {
  it('로그인부터 플랜 생성, 정보·날짜 변경, 장소 자동 저장까지 이어진다', async () => {
    const metadataEditor = {
      ...baseEditor,
      plan: { ...baseEditor.plan, title: '서울 맛집 여행', versionNo: 1 },
    }
    const datedEditor = {
      plan: {
        ...metadataEditor.plan,
        startDate: '2026-08-11',
        endDate: '2026-08-12',
        versionNo: 2,
      },
      days: baseEditor.days.map((day, index) => ({
        ...day,
        travelDate: `2026-08-${11 + index}`,
      })),
    }
    const place = {
      externalPlaceId: '1001',
      placeName: '서울숲',
      categoryName: '관광지',
      address: '서울특별시 성동구',
      latitude: 37.5444,
      longitude: 127.0374,
    }
    const savedEditor = {
      ...datedEditor,
      days: [
        {
          ...datedEditor.days[0],
          scheduleVersion: 1,
          items: [
            {
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              itemVersion: 0,
              placeProvider: 'TOUR_API',
              ...place,
            },
          ],
        },
        datedEditor.days[1],
      ],
    }
    updateTravelPlanMetadataMock.mockResolvedValue(metadataEditor)
    updateTravelPlanDatesMock.mockResolvedValue(datedEditor)
    searchPlacesMock.mockResolvedValue({
      places: [place],
      page: 1,
      size: 10,
      totalCount: 1,
      hasNext: false,
    })
    addScheduleItemMock.mockResolvedValue({
      operationId: 'flow-operation',
      scheduleItemId: '301',
      resultScheduleVersion: 1,
      editor: savedEditor,
    })

    const { router, wrapper } = await mountAt('/login?redirect=/plans/new')
    await wrapper.get('input[type="email"]').setValue('traveler@example.com')
    await wrapper.get('input[type="password"]').setValue('password123!')
    await wrapper.get('.login-form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('plan-setup')
    await wrapper.get('#regionCode').trigger('click')
    await wrapper.get('[role="option"]').trigger('click')
    await wrapper.get('#startDate').setValue('2026-08-10')
    await wrapper.get('#endDate').setValue('2026-08-11')
    await wrapper.get('.setup-form').trigger('submit')
    await flushPromises()

    expect(createTravelPlanMock).toHaveBeenCalledWith({
      regionCode: '1',
      startDate: '2026-08-10',
      endDate: '2026-08-11',
      visibility: 'PUBLIC',
    })
    expect(router.currentRoute.value.fullPath).toBe('/plans/101/edit')

    await wrapper.get('.metadata-editor__open').trigger('click')
    await wrapper.get('[name="editTitle"]').setValue('서울 맛집 여행')
    await wrapper.get('.metadata-editor__form').trigger('submit')
    await flushPromises()
    expect(updateTravelPlanMetadataMock).toHaveBeenCalledWith('101', {
      title: '서울 맛집 여행',
      visibility: 'PUBLIC',
      versionNo: 0,
    })

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-11')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-12')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()
    expect(updateTravelPlanDatesMock).toHaveBeenCalledWith('101', {
      startDate: '2026-08-11',
      endDate: '2026-08-12',
      versionNo: 1,
      force: false,
    })

    await wrapper.get('[name="placeKeyword"]').setValue('서울숲')
    await wrapper.get('.place-search-panel__form').trigger('submit')
    await flushPromises()
    await wrapper.get('.place-search-panel__results button').trigger('click')
    await wrapper.get('.place-detail-card__actions button').trigger('click')
    await flushPromises()

    expect(addScheduleItemMock).toHaveBeenCalledWith(
      '101',
      '201',
      expect.objectContaining({
        externalPlaceId: '1001',
        scheduleVersion: 0,
        timeSlot: 'MORNING',
      }),
    )
    expect(wrapper.text()).toContain('서울 맛집 여행')
    expect(wrapper.text()).toContain('모든 변경사항이 자동 저장되었습니다.')
    expect(wrapper.findAll('.schedule-card')).toHaveLength(1)
  })

  it('상세 화면에서 돌아오면 페이지를 다시 요청하지 않고 검색 결과를 복원한다', async () => {
    getPlanListMock.mockImplementation(({ page }) =>
      Promise.resolve(
        page === 1
          ? { plans: [publicPlan('501', '제주 첫날')], page: 1, totalCount: 2, hasNext: true }
          : { plans: [publicPlan('502', '제주 둘째 날')], page: 2, totalCount: 2, hasNext: false },
      ),
    )
    getPlanDetailMock.mockResolvedValue({
      ...publicPlan('502', '제주 둘째 날'),
      startDate: '2026-08-10',
      endDate: '2026-08-11',
      liked: false,
      days: [
        {
          dayNumber: 1,
          visitDate: '2026-08-10',
          places: [],
        },
      ],
    })

    const { router, wrapper } = await mountAt('/plans?keyword=제주')
    expect(wrapper.findAll('.card')).toHaveLength(1)
    await wrapper.get('.more-btn').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.card')).toHaveLength(2)

    await wrapper.findAll('.card')[1].trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.fullPath).toBe('/plans/502')
    expect(wrapper.text()).toContain('제주 둘째 날')

    await wrapper.get('.back-link').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('plan-search')
    expect(router.currentRoute.value.query).toEqual({ keyword: '제주', page: '2' })
    expect(wrapper.findAll('.card')).toHaveLength(2)
    expect(getPlanListMock).toHaveBeenCalledTimes(2)
  })

  it('진행 중 플랜의 시작일과 종료된 플랜의 전체 날짜 변경을 제한한다', async () => {
    const ongoingEditor = {
      ...baseEditor,
      plan: {
        ...baseEditor.plan,
        planId: 'ongoing',
        startDate: '2026-08-01',
        endDate: '2026-08-06',
      },
    }
    const completedEditor = {
      ...baseEditor,
      plan: {
        ...baseEditor.plan,
        planId: 'completed',
        startDate: '2026-07-01',
        endDate: '2026-07-03',
      },
    }
    getTravelPlanEditorMock.mockImplementation((planId) =>
      Promise.resolve(planId === 'ongoing' ? ongoingEditor : completedEditor),
    )

    const { router, wrapper } = await mountAt('/plans/ongoing/edit')
    await wrapper.get('.date-editor__open').trigger('click')
    expect(wrapper.get('[name="editStartDate"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[name="editEndDate"]').attributes('min')).toBe('2026-08-04')

    await wrapper.get('[aria-label="날짜 변경 닫기"]').trigger('click')
    await router.push('/plans/completed/edit')
    await flushPromises()
    expect(wrapper.get('.date-editor__open').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('종료된 여행은 날짜를 변경할 수 없습니다.')
  })
})
