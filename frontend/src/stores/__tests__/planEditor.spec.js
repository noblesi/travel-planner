import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { usePlanEditorStore } from '@/stores/planEditor'

const { getTravelPlanEditorMock, updateTravelPlanDatesMock } = vi.hoisted(() => ({
  getTravelPlanEditorMock: vi.fn(),
  updateTravelPlanDatesMock: vi.fn(),
}))

vi.mock('@/api/plans', () => ({
  getTravelPlanEditor: getTravelPlanEditorMock,
  updateTravelPlanDates: updateTravelPlanDatesMock,
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
})
