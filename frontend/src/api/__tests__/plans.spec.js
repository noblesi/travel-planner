import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  addScheduleItem,
  deleteScheduleItem,
  getTravelPlanEditor,
  reorderScheduleItems,
  updateScheduleItem,
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
