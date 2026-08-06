import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { usePlanEditorStore } from '@/stores/planEditor'

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
})
