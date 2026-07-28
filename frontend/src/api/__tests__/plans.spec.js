import { beforeEach, describe, expect, it, vi } from 'vitest'

import { getTravelPlanEditor } from '@/api/plans'
import http from '@/api/http'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
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
