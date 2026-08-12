import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  addScheduleItem,
  deleteScheduleItem,
  deleteTravelPlan,
  getMyTravelPlans,
  getTravelPlanEditor,
  reorderScheduleItems,
  restoreTravelPlan,
  updateScheduleItem,
  updatePlanPublication,
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

describe('updateTravelPlanMetadata', () => {
  it('플랜 ID와 현재 Version을 포함해 Metadata 수정 요청을 전송한다', async () => {
    const payload = { title: '서울 맛집 여행', visibility: 'PUBLIC', versionNo: 3 }
    const editor = { plan: { planId: '101', ...payload, versionNo: 4 }, days: [] }
    http.patch.mockResolvedValue({ data: { data: editor } })

    await expect(updateTravelPlanMetadata('101', payload)).resolves.toEqual(editor)
    expect(http.patch).toHaveBeenCalledWith('/plans/101', payload)
  })
})

describe('plan management', () => {
  it('내 플랜 목록을 조회한다', async () => {
    const result = { plans: [{ planId: '101' }] }
    http.get.mockResolvedValue({ data: { data: result } })

    await expect(getMyTravelPlans()).resolves.toEqual(result)
    expect(http.get).toHaveBeenCalledWith('/plans/mine')
  })

  it('발행 상태를 변경하고 플랜을 삭제·복구한다', async () => {
    const payload = { publishStatus: 'PUBLISHED', versionNo: 2 }
    http.patch.mockResolvedValue({ data: { data: { plan: { planId: '101' } } } })
    http.delete.mockResolvedValue({ data: { data: { planStatus: 'DELETED' } } })
    http.post.mockResolvedValue({ data: { data: { planStatus: 'ACTIVE' } } })

    await updatePlanPublication('101', payload)
    await deleteTravelPlan('101', 3)
    await restoreTravelPlan('101', 4)

    expect(http.patch).toHaveBeenCalledWith('/plans/101/publication', payload)
    expect(http.delete).toHaveBeenCalledWith('/plans/101', { params: { versionNo: 3 } })
    expect(http.post).toHaveBeenCalledWith('/plans/101/restore', { versionNo: 4 })
  })
})
