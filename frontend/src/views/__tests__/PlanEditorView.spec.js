import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PlanEditorView from '@/views/PlanEditorView.vue'
import { useToastStore } from '@/stores/toast'

const {
  addScheduleItemMock,
  deleteScheduleItemMock,
  getTravelPlanEditorMock,
  reorderScheduleItemsMock,
  searchPlacesMock,
  updateScheduleItemMock,
  updateTravelPlanDatesMock,
  updateTravelPlanMetadataMock,
  updatePlanPublicationMock,
  routeLeaveState,
} = vi.hoisted(() => ({
  addScheduleItemMock: vi.fn(),
  deleteScheduleItemMock: vi.fn(),
  getTravelPlanEditorMock: vi.fn(),
  reorderScheduleItemsMock: vi.fn(),
  searchPlacesMock: vi.fn(),
  updateScheduleItemMock: vi.fn(),
  updateTravelPlanDatesMock: vi.fn(),
  updateTravelPlanMetadataMock: vi.fn(),
  updatePlanPublicationMock: vi.fn(),
  routeLeaveState: { guard: null, updateGuard: null },
}))

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    onBeforeRouteLeave: (guard) => {
      routeLeaveState.guard = guard
    },
    onBeforeRouteUpdate: (guard) => {
      routeLeaveState.updateGuard = guard
    },
  }
})

vi.mock('@/api/plans', () => ({
  addScheduleItem: addScheduleItemMock,
  deleteScheduleItem: deleteScheduleItemMock,
  getTravelPlanEditor: getTravelPlanEditorMock,
  reorderScheduleItems: reorderScheduleItemsMock,
  updateScheduleItem: updateScheduleItemMock,
  updateTravelPlanDates: updateTravelPlanDatesMock,
  updateTravelPlanMetadata: updateTravelPlanMetadataMock,
  updatePlanPublication: updatePlanPublicationMock,
}))

vi.mock('@/api/places', () => ({
  searchPlaces: searchPlacesMock,
}))

const KakaoMapStub = {
  name: 'KakaoMap',
  props: ['places', 'selectedPlaceId', 'emptyMessage'],
  emits: ['select'],
  template: '<div class="kakao-map-stub" />',
}

const editor = {
  plan: {
    planId: '101',
    title: '서울특별시 여행',
    regionCode: '1',
    regionName: '서울특별시',
    startDate: '2026-08-10',
    endDate: '2026-08-11',
    visibility: 'PRIVATE',
    publishStatus: 'DRAFT',
    canManagePlan: true,
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

function mountView(planId = '101', mountOptions = {}) {
  const pinia = createPinia()
  const wrapper = mount(PlanEditorView, {
    props: { planId },
    global: {
      plugins: [pinia],
      stubs: {
        KakaoMap: KakaoMapStub,
        RouterLink: {
          name: 'RouterLink',
          props: ['to'],
          template: '<a><slot /></a>',
        },
      },
    },
    ...mountOptions,
  })
  wrapper.pinia = pinia
  return wrapper
}

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] })
  vi.setSystemTime(new Date('2026-08-04T00:00:00+09:00'))
  getTravelPlanEditorMock.mockReset().mockResolvedValue(editor)
  searchPlacesMock.mockReset()
  updateTravelPlanDatesMock.mockReset()
  updateTravelPlanMetadataMock.mockReset()
  updatePlanPublicationMock.mockReset()
  addScheduleItemMock.mockReset()
  updateScheduleItemMock.mockReset()
  deleteScheduleItemMock.mockReset()
  reorderScheduleItemsMock.mockReset()
  routeLeaveState.guard = null
  routeLeaveState.updateGuard = null
})

afterEach(() => {
  vi.restoreAllMocks()
  vi.useRealTimers()
})

describe('PlanEditorView', () => {
  it('라우트로 전달된 플랜 ID를 조회하고 제작 화면 정보를 표시한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenCalledWith('101')
    expect(wrapper.text()).toContain('서울특별시 여행')
    expect(wrapper.text()).toContain('여행 일정')
    expect(wrapper.text()).toContain('2일')
    expect(wrapper.get('.editor-toolbar .plan-management').text()).toContain('서울특별시')
    expect(wrapper.get('.editor-toolbar .invite-toolbar-link').text()).toContain('동행자 초대')
    expect(wrapper.find('.schedule-panel .plan-summary').exists()).toBe(false)
    expect(wrapper.find('.schedule-panel .invite-panel-link').exists()).toBe(false)
    expect(wrapper.findAll('.day-tab')).toHaveLength(2)
    expect(wrapper.text()).toContain('DAY 1에 등록된 장소가 없습니다.')
    expect(wrapper.text()).toContain('서울특별시의 관광정보를 TourAPI에서 검색합니다.')
    expect(wrapper.get('.editor-skip-link').attributes('href')).toBe('#plan-editor-main')
    expect(wrapper.get('main').attributes()).toMatchObject({
      id: 'plan-editor-main',
      tabindex: '-1',
    })
  })

  it('제작 완료 실패를 자동 저장 실패와 분리해 안내한다', async () => {
    updatePlanPublicationMock.mockRejectedValueOnce({
      response: {
        status: 409,
        data: {
          code: 'PLAN_PUBLISH_REQUIRES_SCHEDULE',
          message: '일정을 한 곳 이상 추가한 후 제작을 완료해 주세요.',
        },
      },
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.complete-button').trigger('click')
    await flushPromises()

    expect(wrapper.get('.save-state').text()).toContain('자동 저장 준비')
    expect(wrapper.text()).not.toContain('자동 저장이 중단되었습니다.')
    expect(useToastStore(wrapper.pinia).toasts.at(-1)).toMatchObject({
      type: 'error',
      message: '일정을 한 곳 이상 추가한 후 제작을 완료해 주세요.',
    })
  })

  it('선택한 DAY의 오전·오후 일정 카드와 해당 DAY의 빈 상태를 표시한다', async () => {
    getTravelPlanEditorMock.mockResolvedValueOnce({
      ...editor,
      days: [
        {
          ...editor.days[0],
          items: [
            {
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              placeName: '경복궁',
              categoryName: '관광지',
              address: '서울 종로구 사직로 161',
              latitude: 37.5796,
              longitude: 126.977,
              description: '조선 시대의 법궁',
            },
            {
              scheduleItemId: '302',
              timeSlot: 'AFTERNOON',
              positionNo: 1,
              placeName: '북촌한옥마을',
              categoryName: '문화마을',
              address: '서울 종로구 계동길 37',
              description: '한옥 골목 산책',
            },
          ],
        },
        editor.days[1],
      ],
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('오전')
    expect(wrapper.text()).toContain('경복궁')
    expect(wrapper.text()).toContain('오후')
    expect(wrapper.text()).toContain('북촌한옥마을')
    expect(wrapper.findAll('.schedule-card')).toHaveLength(2)
    expect(wrapper.getComponent(KakaoMapStub).props('places')[0]).toMatchObject({
      mapPlaceId: 'schedule:301',
      markerSource: 'SCHEDULE',
      placeName: '경복궁',
    })

    await wrapper.findAll('.day-tab')[1].trigger('click')

    expect(wrapper.findAll('.day-tab')[1].attributes('aria-pressed')).toBe('true')
    expect(wrapper.text()).toContain('DAY 2에 등록된 장소가 없습니다.')
    expect(wrapper.text()).not.toContain('경복궁')
    expect(wrapper.findAll('.schedule-card')).toHaveLength(0)
  })

  it('플랜 ID prop이 변경되면 새로운 편집 데이터를 조회한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.setProps({ planId: '102' })
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenNthCalledWith(1, '101')
    expect(getTravelPlanEditorMock).toHaveBeenNthCalledWith(2, '102')
  })

  it('플랜 제목과 공개 범위를 수정하고 최신 Version을 화면에 반영한다', async () => {
    const updatedEditor = {
      ...editor,
      plan: {
        ...editor.plan,
        title: '서울 맛집 여행',
        visibility: 'PUBLIC',
        versionNo: 1,
      },
    }
    updateTravelPlanMetadataMock.mockResolvedValueOnce(updatedEditor)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.metadata-editor__open').trigger('click')
    await wrapper.get('[name="editTitle"]').setValue('  서울 맛집 여행  ')
    await wrapper.get('[name="editVisibility"]').setValue('PUBLIC')
    await wrapper.get('.metadata-editor__form').trigger('submit')
    await flushPromises()

    expect(updateTravelPlanMetadataMock).toHaveBeenCalledWith('101', {
      title: '서울 맛집 여행',
      visibility: 'PUBLIC',
      versionNo: 0,
    })
    expect(wrapper.text()).toContain('서울 맛집 여행')
    expect(wrapper.text()).toContain('공개')
    expect(wrapper.find('.metadata-editor__form').exists()).toBe(false)
  })

  it('뒤로가기 링크는 새 플랜 설정이 아니라 홈으로 이동한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    const backLink = wrapper
      .findAllComponents({ name: 'RouterLink' })
      .find((link) => link.classes().includes('back-button'))

    expect(backLink.props('to')).toEqual({ name: 'home' })
    expect(backLink.attributes('aria-label')).toBe('홈으로 돌아가기')
  })

  it('메타정보 저장 중 이탈 요청은 저장 완료 후 진행한다', async () => {
    const updatedEditor = {
      ...editor,
      plan: { ...editor.plan, title: '서울 맛집 여행', versionNo: 1 },
    }
    let resolveSave
    updateTravelPlanMetadataMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveSave = resolve
        }),
    )
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.metadata-editor__open').trigger('click')
    await wrapper.get('[name="editTitle"]').setValue('서울 맛집 여행')
    await wrapper.get('.metadata-editor__form').trigger('submit')
    await Promise.resolve()

    let leaveFinished = false
    const leaveRequest = routeLeaveState.guard().then((result) => {
      leaveFinished = true
      return result
    })
    await Promise.resolve()

    expect(leaveFinished).toBe(false)
    expect(wrapper.get('.exit-button').text()).toContain('저장 후 나가기')

    resolveSave(updatedEditor)
    await expect(leaveRequest).resolves.toBe(true)
    expect(confirmSpy).not.toHaveBeenCalled()
  })

  it('공개 상태 저장 중 같은 Editor route의 플랜 변경을 저장 완료까지 기다린다', async () => {
    const publishedEditor = {
      ...editor,
      plan: { ...editor.plan, publishStatus: 'PUBLISHED', versionNo: 1 },
    }
    let resolvePublication
    updatePlanPublicationMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolvePublication = resolve
        }),
    )
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.complete-button').trigger('click')
    await Promise.resolve()

    let updateFinished = false
    const updateRequest = routeLeaveState.updateGuard().then((result) => {
      updateFinished = true
      return result
    })
    await Promise.resolve()

    expect(updateFinished).toBe(false)
    expect(wrapper.get('.exit-button').text()).toContain('저장 후 나가기')

    resolvePublication(publishedEditor)
    await expect(updateRequest).resolves.toBe(true)
  })

  it('저장 실패 후 이탈과 브라우저 종료를 경고한다', async () => {
    updateTravelPlanMetadataMock.mockRejectedValueOnce(new Error('metadata save failed'))
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.metadata-editor__open').trigger('click')
    await wrapper.get('[name="editTitle"]').setValue('서울 맛집 여행')
    await wrapper.get('.metadata-editor__form').trigger('submit')
    await flushPromises()

    await expect(routeLeaveState.guard()).resolves.toBe(false)
    expect(confirmSpy).toHaveBeenCalledOnce()

    const unloadEvent = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(unloadEvent)
    expect(unloadEvent.defaultPrevented).toBe(true)
  })

  it('빈 플랜 제목은 API를 호출하지 않고 Validation 오류를 표시한다', async () => {
    const wrapper = mountView('101', { attachTo: document.body })
    await flushPromises()

    await wrapper.get('.metadata-editor__open').trigger('click')
    const titleInput = wrapper.get('[name="editTitle"]')
    await titleInput.setValue('   ')
    await wrapper.get('.metadata-editor__form').trigger('submit')
    await flushPromises()

    expect(updateTravelPlanMetadataMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('플랜 제목을 입력해 주세요.')
    expect(titleInput.attributes('aria-invalid')).toBe('true')
    expect(titleInput.attributes('aria-describedby')).toBe('metadata-editor-error')
    expect(document.activeElement).toBe(titleInput.element)

    wrapper.unmount()
  })

  it('장소 검색 결과와 선택 장소를 지도에 전달한다', async () => {
    const place = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '1001',
      placeName: '여의도 한강공원',
      categoryName: '관광지',
      address: '서울 영등포구 여의동로 330',
      latitude: 37.5284,
      longitude: 126.934,
      imageUrl: null,
    }
    searchPlacesMock.mockResolvedValueOnce({
      places: [place],
      page: 1,
      size: 10,
      totalCount: 1,
      hasNext: false,
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[name="placeKeyword"]').setValue('한강')
    await wrapper.get('.place-search-panel__form').trigger('submit')
    await flushPromises()
    await wrapper.get('.place-search-panel__results button').trigger('click')

    const map = wrapper.getComponent(KakaoMapStub)
    expect(searchPlacesMock).toHaveBeenCalledWith({
      keyword: '한강',
      regionCode: '1',
      page: 1,
      size: 10,
    })
    expect(map.props('places')).toEqual([
      {
        ...place,
        mapPlaceId: 'search:TOUR_API:1001',
        markerSource: 'SEARCH',
      },
    ])
    expect(map.props('selectedPlaceId')).toBe('search:TOUR_API:1001')
    expect(wrapper.text()).toContain('여의도 한강공원')
  })

  it('검색한 장소를 오전 일정에 추가하고 자동 저장 상태를 표시한다', async () => {
    const place = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '1001',
      placeName: '여의도 한강공원',
      categoryName: '관광지',
      address: '서울 영등포구 여의동로 330',
      latitude: 37.5284,
      longitude: 126.934,
      imageUrl: null,
    }
    const updated = {
      ...editor,
      days: [
        {
          ...editor.days[0],
          scheduleVersion: 1,
          items: [
            {
              ...place,
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              itemVersion: 0,
            },
          ],
        },
        editor.days[1],
      ],
    }
    searchPlacesMock.mockResolvedValue({
      places: [place],
      page: 1,
      size: 10,
      totalCount: 1,
      hasNext: false,
    })
    addScheduleItemMock.mockResolvedValue({
      operationId: 'operation-id',
      scheduleItemId: '301',
      resultScheduleVersion: 1,
      editor: updated,
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('[name="placeKeyword"]').setValue('한강')
    await wrapper.get('.place-search-panel__form').trigger('submit')
    await flushPromises()
    await wrapper.get('.place-search-panel__results button').trigger('click')
    const addButton = wrapper
      .findAll('.place-detail-card__actions button')
      .find((button) => button.text().includes('오전에 추가'))
    await addButton.trigger('click')
    await flushPromises()

    expect(addScheduleItemMock).toHaveBeenCalledWith(
      '101',
      '201',
      expect.objectContaining({
        scheduleVersion: 0,
        timeSlot: 'MORNING',
        externalPlaceId: '1001',
      }),
    )
    expect(wrapper.text()).toContain('모든 변경사항이 자동 저장되었습니다.')
    expect(wrapper.text()).toContain('여의도 한강공원')
    expect(wrapper.findAll('.schedule-card')).toHaveLength(1)
  })

  it('일정 카드에서 시간대를 이동하고 삭제한다', async () => {
    const item = {
      scheduleItemId: '301',
      timeSlot: 'MORNING',
      positionNo: 1,
      itemVersion: 0,
      placeProvider: 'TOUR_API',
      externalPlaceId: '100',
      placeName: '경복궁',
    }
    const withItem = {
      ...editor,
      days: [{ ...editor.days[0], items: [item] }, editor.days[1]],
    }
    const moved = {
      ...editor,
      days: [
        {
          ...editor.days[0],
          scheduleVersion: 1,
          items: [{ ...item, timeSlot: 'AFTERNOON', itemVersion: 1 }],
        },
        editor.days[1],
      ],
    }
    const deleted = {
      ...editor,
      days: [{ ...editor.days[0], scheduleVersion: 2, items: [] }, editor.days[1]],
    }
    getTravelPlanEditorMock.mockResolvedValueOnce(withItem)
    updateScheduleItemMock.mockResolvedValueOnce({ editor: moved, resultScheduleVersion: 1 })
    deleteScheduleItemMock.mockResolvedValueOnce({ editor: deleted, resultScheduleVersion: 2 })
    const wrapper = mountView()
    await flushPromises()

    await wrapper
      .findAll('.schedule-card__actions button')
      .find((button) => button.text().includes('오후로'))
      .trigger('click')
    await flushPromises()

    expect(updateScheduleItemMock).toHaveBeenCalledWith(
      '101',
      '201',
      '301',
      expect.objectContaining({ scheduleVersion: 0, itemVersion: 0, timeSlot: 'AFTERNOON' }),
    )

    await wrapper.get('[aria-label="경복궁 일정 삭제"]').trigger('click')
    await flushPromises()

    expect(deleteScheduleItemMock).toHaveBeenCalledWith(
      '101',
      '201',
      '301',
      expect.objectContaining({ scheduleVersion: 1, itemVersion: 1 }),
    )
    expect(wrapper.text()).toContain('DAY 1에 등록된 장소가 없습니다.')
  })

  it('조회 실패 메시지를 표시하고 다시 시도한다', async () => {
    getTravelPlanEditorMock.mockRejectedValueOnce({
      response: { status: 404, data: { message: '여행 플랜을 찾을 수 없습니다.' } },
    })
    const wrapper = mountView('999')
    await flushPromises()

    expect(wrapper.text()).toContain('여행 플랜을 찾을 수 없습니다.')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('서울특별시 여행')
  })

  it('제작 페이지에서 여행 날짜를 변경하고 응답 DAY를 반영한다', async () => {
    const updatedEditor = {
      plan: {
        ...editor.plan,
        startDate: '2026-08-09',
        endDate: '2026-08-11',
        versionNo: 1,
      },
      days: [
        {
          planDayId: '200',
          dayNo: 1,
          travelDate: '2026-08-09',
          scheduleVersion: 0,
          items: [],
        },
        { ...editor.days[0], dayNo: 2 },
        { ...editor.days[1], dayNo: 3 },
      ],
    }
    updateTravelPlanDatesMock.mockResolvedValueOnce(updatedEditor)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-09')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-11')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()

    expect(updateTravelPlanDatesMock).toHaveBeenCalledWith('101', {
      startDate: '2026-08-09',
      endDate: '2026-08-11',
      versionNo: 0,
      force: false,
    })
    expect(wrapper.text()).toContain('3일')
    expect(wrapper.findAll('.day-tab')).toHaveLength(3)
    expect(wrapper.find('.date-editor__form').exists()).toBe(false)
  })

  it('일정이 있는 DAY가 제외되면 확인 후 강제로 날짜를 변경한다', async () => {
    updateTravelPlanDatesMock
      .mockRejectedValueOnce({
        response: {
          status: 409,
          data: {
            code: 'PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED',
            message: '변경 범위에서 제외되는 날짜에 일정이 있습니다.',
          },
        },
      })
      .mockResolvedValueOnce({
        plan: { ...editor.plan, startDate: '2026-08-11', versionNo: 1 },
        days: [{ ...editor.days[1], dayNo: 1 }],
      })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-11')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-11')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alertdialog"]').exists()).toBe(true)

    const confirmButton = wrapper
      .findAll('.confirmation-dialog__actions button')
      .find((button) => button.text().includes('일정 삭제 후 변경'))
    await confirmButton.trigger('click')
    await flushPromises()

    expect(updateTravelPlanDatesMock).toHaveBeenNthCalledWith(2, '101', {
      startDate: '2026-08-11',
      endDate: '2026-08-11',
      versionNo: 0,
      force: true,
    })
    expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('1일')
  })

  it('날짜 삭제 확인창의 포커스를 가두고 Escape 후 저장 버튼으로 복귀한다', async () => {
    updateTravelPlanDatesMock.mockRejectedValueOnce({
      response: {
        status: 409,
        data: {
          code: 'PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED',
          message: '변경 범위에서 제외되는 날짜에 일정이 있습니다.',
        },
      },
    })
    const wrapper = mountView('101', { attachTo: document.body })
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-11')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-11')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()

    const dialog = wrapper.get('[role="alertdialog"]')
    const buttons = wrapper.findAll('.confirmation-dialog__actions button')
    expect(document.activeElement).toBe(buttons[0].element)

    buttons[1].element.focus()
    await dialog.trigger('keydown', { key: 'Tab' })
    expect(document.activeElement).toBe(buttons[0].element)

    await dialog.trigger('keydown', { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(buttons[1].element)

    await dialog.trigger('keydown', { key: 'Escape' })
    await flushPromises()

    expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
    expect(document.activeElement).toBe(
      wrapper.get('.date-editor__actions button[type="submit"]').element,
    )

    wrapper.unmount()
  })

  it('14일을 초과한 날짜 변경은 API를 호출하지 않는다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-05')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-19')
    await wrapper.get('.date-editor__form').trigger('submit')

    expect(updateTravelPlanDatesMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('여행 기간은 최대 14일까지 설정할 수 있습니다.')
  })

  it('진행 중인 여행은 시작일을 고정하고 종료일만 변경한다', async () => {
    const ongoingEditor = {
      ...editor,
      plan: { ...editor.plan, startDate: '2026-08-01', endDate: '2026-08-06' },
    }
    updateTravelPlanDatesMock.mockResolvedValueOnce({
      ...ongoingEditor,
      plan: { ...ongoingEditor.plan, endDate: '2026-08-07', versionNo: 1 },
    })
    getTravelPlanEditorMock.mockResolvedValueOnce(ongoingEditor)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')

    expect(wrapper.get('[name="editStartDate"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[name="editEndDate"]').attributes('min')).toBe('2026-08-04')

    await wrapper.get('[name="editEndDate"]').setValue('2026-08-07')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()

    expect(updateTravelPlanDatesMock).toHaveBeenCalledWith('101', {
      startDate: '2026-08-01',
      endDate: '2026-08-07',
      versionNo: 0,
      force: false,
    })
  })

  it('종료된 여행은 날짜 변경을 비활성화하고 이유를 안내한다', async () => {
    getTravelPlanEditorMock.mockResolvedValueOnce({
      ...editor,
      plan: { ...editor.plan, startDate: '2026-07-01', endDate: '2026-07-02' },
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.get('.date-editor__open').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('종료된 여행은 날짜를 변경할 수 없습니다.')
  })
})
