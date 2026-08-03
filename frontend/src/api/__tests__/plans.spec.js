import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  addScheduleItem,
  deleteScheduleItem,
  getPublicTravelPlan,
  getTravelPlanEditor,
  reorderScheduleItems,
  searchPublicPlans,
  updateScheduleItem,
  updateTravelPlanMetadata,
} from '@/api/plans'
import http from '@/api/http'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('schedule item mutations', () => {
  const mutation = { operationId: 'operation-1', editor: { plan: {}, days: [] } }

  beforeEach(() => {
    http.post.mockResolvedValue({ data: { data: mutation } })
    http.patch.mockResolvedValue({ data: { data: mutation } })
    http.put.mockResolvedValue({ data: { data: mutation } })
    http.delete.mockResolvedValue({ data: { data: mutation } })
  })

  it('일정 추가 요청을 전송한다', async () => {
    const payload = { operationId: 'operation-1', scheduleVersion: 0 }

    await expect(addScheduleItem('101', '201', payload)).resolves.toEqual(mutation)
    expect(http.post).toHaveBeenCalledWith('/plans/101/days/201/items', payload)
  })

  it('일정 수정과 삭제 요청의 항목 ID를 경로에 포함한다', async () => {
    const payload = { operationId: 'operation-1', scheduleVersion: 1, itemVersion: 0 }

    await updateScheduleItem('101', '201', '301', payload)
    await deleteScheduleItem('101', '201', '301', payload)

    expect(http.patch).toHaveBeenCalledWith('/plans/101/days/201/items/301', payload)
    expect(http.delete).toHaveBeenCalledWith('/plans/101/days/201/items/301', { data: payload })
  })

  it('시간대별 정렬 요청을 전송한다', async () => {
    const payload = {
      operationId: 'operation-1',
      scheduleVersion: 2,
      timeSlot: 'MORNING',
      scheduleItemIds: ['302', '301'],
    }

    await reorderScheduleItems('101', '201', payload)

    expect(http.put).toHaveBeenCalledWith('/plans/101/days/201/items/order', payload)
  })
})

describe('getTravelPlanEditor', () => {
  it('플랜 ID를 경로에 포함하고 응답 data를 반환한다', async () => {
    const editor = {
      plan: { planId: '101', title: '서울특별시 여행' },
      days: [],
    }
    http.get.mockResolvedValue({ data: { data: editor } })

    await expect(getTravelPlanEditor('101')).resolves.toEqual(editor)
    expect(http.get).toHaveBeenCalledWith('/plans/101/editor')
  })
})

describe('public plan reads', () => {
  it('검색어와 제한 건수를 전달해 공개 플랜을 조회한다', async () => {
    const result = { keyword: '서울', totalCount: 1, plans: [{ planId: '11' }] }
    http.get.mockResolvedValue({ data: { data: result } })

    await expect(searchPublicPlans({ keyword: '서울', limit: 24 })).resolves.toEqual(result)
    expect(http.get).toHaveBeenCalledWith('/plans', {
      params: { keyword: '서울', limit: 24 },
    })
  })

  it('공개 플랜 ID를 인코딩해 상세를 조회한다', async () => {
    const detail = { plan: { planId: '11' }, days: [] }
    http.get.mockResolvedValue({ data: { data: detail } })

    await expect(getPublicTravelPlan('11/12')).resolves.toEqual(detail)
    expect(http.get).toHaveBeenCalledWith('/plans/11%2F12')
  })
})

describe('updateTravelPlanMetadata', () => {
  it('플랜 ID와 현재 Version을 포함해 Metadata 수정 요청을 전송한다', async () => {
    const payload = { title: '서울 맛집 여행', visibility: 'PUBLIC', versionNo: 3 }
    const editor = { plan: { planId: '101', ...payload, versionNo: 4 }, days: [] }
    http.patch.mockResolvedValue({ data: { data: editor } })

    await expect(updateTravelPlanMetadata('101', payload)).resolves.toEqual(editor)
    expect(http.patch).toHaveBeenCalledWith('/plans/101', payload)
  })
})
