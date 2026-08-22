import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { usePlanEditorStore } from '@/stores/planEditor'
import { usePlanSearchStore } from '@/stores/planSearch'

const {
  addScheduleItemMock,
  deleteScheduleItemMock,
  getTravelPlanEditorMock,
  reorderScheduleItemsMock,
  updateScheduleItemMock,
  updateTravelPlanDatesMock,
  updateTravelPlanMetadataMock,
  updatePlanPublicationMock,
} = vi.hoisted(() => ({
  addScheduleItemMock: vi.fn(),
  deleteScheduleItemMock: vi.fn(),
  getTravelPlanEditorMock: vi.fn(),
  reorderScheduleItemsMock: vi.fn(),
  updateScheduleItemMock: vi.fn(),
  updateTravelPlanDatesMock: vi.fn(),
  updateTravelPlanMetadataMock: vi.fn(),
  updatePlanPublicationMock: vi.fn(),
}))

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

const plan = {
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
}

beforeEach(() => {
  setActivePinia(createPinia())
  getTravelPlanEditorMock.mockReset()
  updateTravelPlanDatesMock.mockReset()
  updateTravelPlanMetadataMock.mockReset()
  updatePlanPublicationMock.mockReset()
  addScheduleItemMock.mockReset()
  updateScheduleItemMock.mockReset()
  deleteScheduleItemMock.mockReset()
  reorderScheduleItemsMock.mockReset()
})

describe('planEditor store', () => {
  it('편집 데이터를 조회하고 첫 일차와 시간대별 일정을 선택한다', async () => {
    const days = [
      {
        planDayId: '201',
        dayNo: 1,
        travelDate: '2026-08-10',
        scheduleVersion: 0,
        items: [
          { scheduleItemId: '301', timeSlot: 'MORNING', positionNo: 1 },
          { scheduleItemId: '302', timeSlot: 'AFTERNOON', positionNo: 1 },
        ],
      },
      {
        planDayId: '202',
        dayNo: 2,
        travelDate: '2026-08-11',
        scheduleVersion: 0,
        items: [],
      },
    ]
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    const store = usePlanEditorStore()

    const request = store.loadPlanEditor('101')
    expect(store.status).toBe('loading')
    expect(store.isLoading).toBe(true)

    await request

    expect(getTravelPlanEditorMock).toHaveBeenCalledWith('101')
    expect(store.status).toBe('success')
    expect(store.plan).toEqual(plan)
    expect(store.days).toEqual(days)
    expect(store.selectedDayId).toBe('201')
    expect(store.selectedDay).toEqual(days[0])
    expect(store.morningItems).toEqual([days[0].items[0]])
    expect(store.afternoonItems).toEqual([days[0].items[1]])
    expect(store.isSelectedDayEmpty).toBe(false)
    expect(store.isReady).toBe(true)

    store.selectDay('202')

    expect(store.isEmpty).toBe(false)
    expect(store.isSelectedDayEmpty).toBe(true)
  })

  it('모든 일차에 일정이 없으면 빈 일정 상태가 된다', async () => {
    const days = [
      {
        planDayId: '201',
        dayNo: 1,
        travelDate: '2026-08-10',
        scheduleVersion: 0,
        items: [],
      },
    ]
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    const store = usePlanEditorStore()

    await store.loadPlanEditor('101')

    expect(store.status).toBe('empty')
    expect(store.isEmpty).toBe(true)
    expect(store.selectedDay).toEqual(days[0])
    expect(store.scheduleItems).toEqual([])
    expect(store.isSelectedDayEmpty).toBe(true)
  })

  it('플랜 Metadata 수정 응답을 반영하고 선택 DAY를 유지한다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] },
      { planDayId: '202', dayNo: 2, travelDate: '2026-08-11', items: [] },
    ]
    const updated = {
      plan: { ...plan, title: '서울 맛집 여행', visibility: 'PUBLIC', versionNo: 1 },
      days,
    }
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    updateTravelPlanMetadataMock.mockResolvedValue(updated)
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')
    store.selectDay('202')

    const payload = { title: '서울 맛집 여행', visibility: 'PUBLIC', versionNo: 0 }
    await expect(store.savePlanMetadata(payload)).resolves.toEqual(updated)

    expect(updateTravelPlanMetadataMock).toHaveBeenCalledWith('101', payload)
    expect(store.plan).toEqual(updated.plan)
    expect(store.selectedDayId).toBe('202')
  })

  it('공개 플랜 수정 후 탐색 캐시를 무효화한다', async () => {
    const days = [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }]
    const publishedPlan = { ...plan, publishStatus: 'PUBLISHED' }
    const updated = {
      plan: { ...publishedPlan, title: '서울 궁궐 여행', versionNo: 1 },
      days,
    }
    getTravelPlanEditorMock.mockResolvedValue({ plan: publishedPlan, days })
    updateTravelPlanMetadataMock.mockResolvedValue(updated)
    const searchStore = usePlanSearchStore()
    searchStore.cacheSearch({
      keyword: '',
      searchedKeyword: '',
      currentPage: 1,
      plans: [{ id: '101', title: publishedPlan.title }],
      totalCount: 1,
      hasNextPage: false,
      hasSearched: false,
    })
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    await store.savePlanMetadata({
      title: '서울 궁궐 여행',
      visibility: 'PUBLIC',
      versionNo: 0,
    })

    expect(searchStore.restoreSearch({ searchedKeyword: '', currentPage: 1 })).toBeNull()
  })

  it('플랜 Version 충돌 시 최신 Editor Snapshot을 복구하고 오류를 유지한다', async () => {
    const days = [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }]
    const latest = { plan: { ...plan, title: '동료가 변경한 제목', versionNo: 1 }, days }
    const conflict = {
      response: { status: 409, data: { code: 'PLAN_VERSION_CONFLICT', message: '버전 충돌' } },
    }
    getTravelPlanEditorMock.mockResolvedValueOnce({ plan, days }).mockResolvedValueOnce(latest)
    updateTravelPlanMetadataMock.mockRejectedValue(conflict)
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    await expect(
      store.savePlanMetadata({ title: '서울 맛집 여행', visibility: 'PUBLIC', versionNo: 0 }),
    ).rejects.toBe(conflict)

    expect(getTravelPlanEditorMock).toHaveBeenCalledTimes(2)
    expect(store.plan).toEqual(latest.plan)
    expect(store.hasUnsavedChanges).toBe(true)
  })

  it('메타정보 저장도 pending 저장으로 추적하고 완료될 때까지 기다린다', async () => {
    const days = [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }]
    const updated = {
      plan: { ...plan, title: '서울 맛집 여행', versionNo: 1 },
      days,
    }
    let resolveSave
    updateTravelPlanMetadataMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveSave = resolve
        }),
    )
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const saveRequest = store.savePlanMetadata({
      title: '서울 맛집 여행',
      visibility: 'PRIVATE',
      versionNo: 0,
    })
    await Promise.resolve()

    expect(store.isSaving).toBe(true)
    expect(store.pendingSaveCount).toBe(1)

    let waitFinished = false
    const waitRequest = store.waitForPendingSaves().then((result) => {
      waitFinished = true
      return result
    })
    await Promise.resolve()
    expect(waitFinished).toBe(false)

    resolveSave(updated)
    await expect(saveRequest).resolves.toEqual(updated)
    await expect(waitRequest).resolves.toBe(true)
    expect(store.isSaving).toBe(false)
    expect(store.pendingSaveCount).toBe(0)
    expect(store.hasUnsavedChanges).toBe(false)
  })

  it('직접 저장 실패를 이탈 확인이 필요한 변경으로 유지하고 재저장 시 해제한다', async () => {
    const days = [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }]
    const failure = new Error('metadata save failed')
    const updated = {
      plan: { ...plan, title: '서울 맛집 여행', versionNo: 1 },
      days,
    }
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    updateTravelPlanMetadataMock.mockRejectedValueOnce(failure).mockResolvedValueOnce(updated)
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const payload = { title: '서울 맛집 여행', visibility: 'PRIVATE', versionNo: 0 }
    await expect(store.savePlanMetadata(payload)).rejects.toBe(failure)

    expect(store.hasUnsavedChanges).toBe(true)
    await expect(store.waitForPendingSaves()).resolves.toBe(false)

    await expect(store.savePlanMetadata(payload)).resolves.toEqual(updated)
    expect(store.hasUnsavedChanges).toBe(false)
  })

  it('제작 완료 실패를 자동 저장 실패 상태로 기록하지 않는다', async () => {
    const days = [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }]
    const failure = {
      response: {
        status: 409,
        data: {
          code: 'PLAN_PUBLISH_REQUIRES_SCHEDULE',
          message: '일정을 한 곳 이상 추가한 후 제작을 완료해 주세요.',
        },
      },
    }
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    updatePlanPublicationMock.mockRejectedValue(failure)
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    await expect(store.savePlanPublication('PUBLISHED')).rejects.toBe(failure)

    expect(updatePlanPublicationMock).toHaveBeenCalledWith('101', {
      publishStatus: 'PUBLISHED',
      versionNo: 0,
    })
    expect(store.saveStatus).toBe('idle')
    expect(store.saveMessage).toBe('자동 저장 준비')
    expect(store.hasSaveError).toBe(false)
    expect(store.hasUnsavedChanges).toBe(false)
  })

  it('조회 실패 메시지를 저장하고 기존 편집 데이터를 비운다', async () => {
    getTravelPlanEditorMock.mockRejectedValue({
      response: { status: 404, data: { message: '여행 플랜을 찾을 수 없습니다.' } },
    })
    const store = usePlanEditorStore()

    const result = await store.loadPlanEditor('999')

    expect(result).toBeNull()
    expect(store.status).toBe('error')
    expect(store.hasError).toBe(true)
    expect(store.errorMessage).toBe('여행 플랜을 찾을 수 없습니다.')
    expect(store.plan).toBeNull()
    expect(store.days).toEqual([])
  })

  it('존재하는 일차만 선택하고 초기 상태로 재설정한다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
      { planDayId: '202', dayNo: 2, travelDate: '2026-08-11', scheduleVersion: 0, items: [] },
    ]
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    expect(store.selectDay('999')).toBe(false)
    expect(store.selectedDayId).toBe('201')
    expect(store.selectDay('202')).toBe(true)
    expect(store.selectedDayId).toBe('202')

    store.resetEditor()

    expect(store.status).toBe('idle')
    expect(store.plan).toBeNull()
    expect(store.days).toEqual([])
    expect(store.selectedDayId).toBeNull()
  })

  it('날짜 변경 결과를 반영하고 남아 있는 선택 DAY를 유지한다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
      { planDayId: '202', dayNo: 2, travelDate: '2026-08-11', scheduleVersion: 0, items: [] },
    ]
    const updated = {
      plan: { ...plan, startDate: '2026-08-09', endDate: '2026-08-11', versionNo: 1 },
      days: [
        { planDayId: '200', dayNo: 1, travelDate: '2026-08-09', scheduleVersion: 0, items: [] },
        { ...days[0], dayNo: 2 },
        { ...days[1], dayNo: 3 },
      ],
    }
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    updateTravelPlanDatesMock.mockResolvedValue(updated)
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')
    store.selectDay('202')

    await store.savePlanDates({
      startDate: '2026-08-09',
      endDate: '2026-08-11',
      versionNo: 0,
      force: false,
    })

    expect(updateTravelPlanDatesMock).toHaveBeenCalledWith('101', {
      startDate: '2026-08-09',
      endDate: '2026-08-11',
      versionNo: 0,
      force: false,
    })
    expect(store.plan).toEqual(updated.plan)
    expect(store.days).toEqual(updated.days)
    expect(store.selectedDayId).toBe('202')
  })

  it('선택 장소를 일정에 추가하고 자동 저장 응답을 반영한다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
    ]
    const place = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '100',
      placeName: '경복궁',
      categoryName: '관광지',
      address: '서울 종로구',
      latitude: 37.5796,
      longitude: 126.977,
      imageUrl: null,
    }
    const updated = {
      plan,
      days: [
        {
          ...days[0],
          scheduleVersion: 1,
          items: [
            {
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              itemVersion: 0,
              ...place,
            },
          ],
        },
      ],
    }
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    addScheduleItemMock.mockResolvedValue({
      operationId: 'operation-id',
      scheduleItemId: '301',
      resultScheduleVersion: 1,
      editor: updated,
    })
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const request = store.addPlaceToSchedule(place, 'MORNING')
    expect(store.saveStatus).toBe('saving')
    expect(store.pendingSaveCount).toBe(1)
    await request

    expect(addScheduleItemMock).toHaveBeenCalledWith(
      '101',
      '201',
      expect.objectContaining({
        operationId: expect.stringMatching(
          /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/,
        ),
        scheduleVersion: 0,
        timeSlot: 'MORNING',
        externalPlaceId: '100',
      }),
    )
    expect(store.days).toEqual(updated.days)
    expect(store.saveStatus).toBe('saved')
    expect(store.pendingSaveCount).toBe(0)
  })

  it('여러 작업을 직렬화하고 앞 작업의 최신 Version으로 다음 요청을 보낸다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
    ]
    const firstPlace = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '100',
      placeName: '경복궁',
    }
    const secondPlace = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '101',
      placeName: '창덕궁',
    }
    let resolveFirst
    let resolveSecond
    addScheduleItemMock
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveFirst = resolve
          }),
      )
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveSecond = resolve
          }),
      )
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const firstRequest = store.addPlaceToSchedule(firstPlace, 'MORNING')
    const secondRequest = store.addPlaceToSchedule(secondPlace, 'AFTERNOON')
    await Promise.resolve()
    expect(addScheduleItemMock).toHaveBeenCalledTimes(1)

    const afterFirst = {
      plan,
      days: [
        {
          ...days[0],
          scheduleVersion: 1,
          items: [
            {
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              itemVersion: 0,
              ...firstPlace,
            },
          ],
        },
      ],
    }
    resolveFirst({ editor: afterFirst, resultScheduleVersion: 1 })
    await firstRequest
    await Promise.resolve()

    expect(addScheduleItemMock).toHaveBeenCalledTimes(2)
    expect(addScheduleItemMock.mock.calls[1][2].scheduleVersion).toBe(1)

    resolveSecond({
      editor: {
        plan,
        days: [{ ...afterFirst.days[0], scheduleVersion: 2 }],
      },
      resultScheduleVersion: 2,
    })
    await secondRequest

    expect(store.pendingSaveCount).toBe(0)
    expect(store.saveStatus).toBe('saved')
  })

  it('Version 충돌 시 최신 Editor를 다시 불러오고 같은 작업을 재시도한다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
    ]
    const latest = {
      plan,
      days: [{ ...days[0], scheduleVersion: 1 }],
    }
    const resolved = {
      plan,
      days: [{ ...days[0], scheduleVersion: 2 }],
    }
    const place = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '100',
      placeName: '경복궁',
    }
    getTravelPlanEditorMock.mockResolvedValueOnce({ plan, days }).mockResolvedValueOnce(latest)
    addScheduleItemMock
      .mockRejectedValueOnce({
        response: {
          status: 409,
          data: { code: 'SCHEDULE_VERSION_CONFLICT', message: '일정 버전 충돌' },
        },
      })
      .mockResolvedValueOnce({ editor: resolved, resultScheduleVersion: 2 })
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    await expect(store.addPlaceToSchedule(place, 'MORNING')).rejects.toBeTruthy()

    expect(getTravelPlanEditorMock).toHaveBeenCalledTimes(2)
    expect(store.days).toEqual(latest.days)
    expect(store.saveStatus).toBe('conflict')
    expect(store.canRetrySave).toBe(true)
    const firstOperationId = addScheduleItemMock.mock.calls[0][2].operationId

    await store.retryLastSave()

    expect(addScheduleItemMock.mock.calls[1][2]).toMatchObject({
      operationId: firstOperationId,
      scheduleVersion: 1,
    })
    expect(store.days).toEqual(resolved.days)
    expect(store.saveStatus).toBe('saved')
    expect(store.canRetrySave).toBe(false)
  })

  it('새 플랜을 불러오면 queue에서 대기하던 이전 플랜 저장을 전송하지 않는다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
    ]
    const nextEditor = {
      plan: { ...plan, planId: '202', title: '부산 여행' },
      days: [
        { planDayId: '301', dayNo: 1, travelDate: '2026-09-01', scheduleVersion: 0, items: [] },
      ],
    }
    const place = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '100',
      placeName: '경복궁',
    }
    let resolveScheduleSave
    getTravelPlanEditorMock
      .mockResolvedValueOnce({ plan, days })
      .mockResolvedValueOnce(nextEditor)
    addScheduleItemMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveScheduleSave = resolve
        }),
    )
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const scheduleRequest = store.addPlaceToSchedule(place, 'MORNING')
    await Promise.resolve()
    const metadataRequest = store.savePlanMetadata({
      title: '서울 궁궐 여행',
      visibility: 'PRIVATE',
      versionNo: 0,
    })
    await Promise.resolve()

    await store.loadPlanEditor('202')
    resolveScheduleSave({ editor: { plan, days }, resultScheduleVersion: 1 })

    await expect(scheduleRequest).resolves.toBeNull()
    await expect(metadataRequest).resolves.toBeNull()
    expect(updateTravelPlanMetadataMock).not.toHaveBeenCalled()
    expect(store.plan).toEqual(nextEditor.plan)
  })

  it('이전 플랜에서 이미 전송된 저장 응답은 새 플랜 snapshot에 적용하지 않는다', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
    ]
    const oldUpdatedEditor = {
      plan: { ...plan, title: '늦게 저장된 서울 여행', versionNo: 1 },
      days,
    }
    const nextEditor = {
      plan: { ...plan, planId: '202', title: '부산 여행' },
      days: [
        { planDayId: '301', dayNo: 1, travelDate: '2026-09-01', scheduleVersion: 0, items: [] },
      ],
    }
    let resolveMetadataSave
    getTravelPlanEditorMock
      .mockResolvedValueOnce({ plan, days })
      .mockResolvedValueOnce(nextEditor)
    updateTravelPlanMetadataMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveMetadataSave = resolve
        }),
    )
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const saveRequest = store.savePlanMetadata({
      title: '늦게 저장된 서울 여행',
      visibility: 'PRIVATE',
      versionNo: 0,
    })
    await Promise.resolve()
    expect(updateTravelPlanMetadataMock).toHaveBeenCalledWith(
      '101',
      expect.objectContaining({ title: '늦게 저장된 서울 여행' }),
    )

    await store.loadPlanEditor('202')
    resolveMetadataSave(oldUpdatedEditor)

    await expect(saveRequest).resolves.toBeNull()
    expect(store.plan).toEqual(nextEditor.plan)
    expect(store.selectedDayId).toBe('301')
  })

  it('A에서 B를 거쳐 다시 A로 진입해도 이전 A session 응답을 적용하지 않는다', async () => {
    const initialEditor = {
      plan,
      days: [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }],
    }
    const nextEditor = {
      plan: { ...plan, planId: '202', title: '부산 여행' },
      days: [{ planDayId: '301', dayNo: 1, travelDate: '2026-09-01', items: [] }],
    }
    const latestEditor = {
      plan: { ...plan, title: '다시 불러온 최신 서울 여행', versionNo: 2 },
      days: initialEditor.days,
    }
    const oldSavedEditor = {
      plan: { ...plan, title: '첫 번째 서울 session 응답', versionNo: 1 },
      days: initialEditor.days,
    }
    let resolveOldSave
    getTravelPlanEditorMock
      .mockResolvedValueOnce(initialEditor)
      .mockResolvedValueOnce(nextEditor)
      .mockResolvedValueOnce(latestEditor)
    updateTravelPlanMetadataMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveOldSave = resolve
        }),
    )
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const oldSave = store.savePlanMetadata({
      title: '첫 번째 서울 session 응답',
      visibility: 'PRIVATE',
      versionNo: 0,
    })
    await Promise.resolve()

    await store.loadPlanEditor('202')
    await store.loadPlanEditor('101')
    resolveOldSave(oldSavedEditor)

    await expect(oldSave).resolves.toBeNull()
    expect(store.plan).toEqual(latestEditor.plan)
  })

  it('서로 겹친 Editor 조회에서는 마지막 session의 응답만 적용한다', async () => {
    const oldEditor = {
      plan,
      days: [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }],
    }
    const nextEditor = {
      plan: { ...plan, planId: '202', title: '부산 여행' },
      days: [{ planDayId: '301', dayNo: 1, travelDate: '2026-09-01', items: [] }],
    }
    let resolveOldLoad
    getTravelPlanEditorMock
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveOldLoad = resolve
          }),
      )
      .mockResolvedValueOnce(nextEditor)
    const store = usePlanEditorStore()

    const oldLoad = store.loadPlanEditor('101')
    const nextLoad = store.loadPlanEditor('202')
    await expect(nextLoad).resolves.toEqual(nextEditor)
    resolveOldLoad(oldEditor)

    await expect(oldLoad).resolves.toBeNull()
    expect(store.plan).toEqual(nextEditor.plan)
  })

  it('공개 상태 저장도 pending 저장으로 추적한다', async () => {
    const days = [{ planDayId: '201', dayNo: 1, travelDate: '2026-08-10', items: [] }]
    const published = {
      plan: { ...plan, publishStatus: 'PUBLISHED', versionNo: 1 },
      days,
    }
    let resolvePublication
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    updatePlanPublicationMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolvePublication = resolve
        }),
    )
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const publicationRequest = store.savePlanPublication('PUBLISHED')
    await Promise.resolve()

    expect(store.isSaving).toBe(true)
    expect(store.pendingSaveCount).toBe(1)
    resolvePublication(published)

    await expect(publicationRequest).resolves.toEqual(published)
    expect(store.isSaving).toBe(false)
    expect(store.hasUnsavedChanges).toBe(false)
  })

  it('serializes schedule and metadata saves so an older response cannot overwrite newer state', async () => {
    const days = [
      { planDayId: '201', dayNo: 1, travelDate: '2026-08-10', scheduleVersion: 0, items: [] },
    ]
    const place = {
      placeProvider: 'TOUR_API',
      externalPlaceId: '100',
      placeName: 'Gyeongbokgung Palace',
    }
    const scheduled = {
      plan,
      days: [
        {
          ...days[0],
          scheduleVersion: 1,
          items: [
            {
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              itemVersion: 0,
              ...place,
            },
          ],
        },
      ],
    }
    const metadataSaved = {
      plan: { ...plan, title: 'Seoul palace trip', versionNo: 1 },
      days: scheduled.days,
    }
    let resolveScheduleSave
    getTravelPlanEditorMock.mockResolvedValue({ plan, days })
    addScheduleItemMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveScheduleSave = resolve
        }),
    )
    updateTravelPlanMetadataMock.mockResolvedValue(metadataSaved)
    const store = usePlanEditorStore()
    await store.loadPlanEditor('101')

    const scheduleRequest = store.addPlaceToSchedule(place, 'MORNING')
    await Promise.resolve()
    const metadataRequest = store.savePlanMetadata({
      title: 'Seoul palace trip',
      visibility: 'PRIVATE',
      versionNo: 0,
    })
    await Promise.resolve()

    expect(addScheduleItemMock).toHaveBeenCalledTimes(1)
    expect(updateTravelPlanMetadataMock).not.toHaveBeenCalled()

    resolveScheduleSave({ editor: scheduled, resultScheduleVersion: 1 })
    await scheduleRequest
    await metadataRequest

    expect(updateTravelPlanMetadataMock).toHaveBeenCalledTimes(1)
    expect(store.plan.title).toBe('Seoul palace trip')
    expect(store.days[0].items).toHaveLength(1)
    expect(store.days[0].items[0].scheduleItemId).toBe('301')
  })
})
