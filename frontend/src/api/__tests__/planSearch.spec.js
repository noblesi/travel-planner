import { beforeEach, describe, expect, it, vi } from 'vitest'

import http from '@/api/http'
import { copyPlan, getPlanDetail, getPlanList, reportPlan, toggleLike } from '@/api/planSearch'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('public plan API', () => {
  it('단일 plans endpoint에서 공개 플랜 목록을 조회한다', async () => {
    http.get.mockResolvedValue({
      data: {
        data: {
          content: [{ planId: '101' }],
          pagination: { page: 2, totalCount: 9, totalPages: 3 },
        },
      },
    })

    await expect(getPlanList({ keyword: '서울', page: 2, size: 4 })).resolves.toEqual({
      plans: [{ planId: '101' }],
      page: 2,
      totalCount: 9,
      hasNext: true,
    })
    expect(http.get).toHaveBeenCalledWith('/plans', {
      params: { keyword: '서울', page: 2, size: 4 },
    })
  })

  it('상세 ID를 인코딩해 plans endpoint에서 조회한다', async () => {
    const detail = { planId: '101', days: [] }
    http.get.mockResolvedValue({ data: { data: detail } })

    await expect(getPlanDetail('101/102')).resolves.toEqual(detail)
    expect(http.get).toHaveBeenCalledWith('/plans/101%2F102')
  })

  it('좋아요·신고·복사를 동일한 plan resource 하위로 전송한다', async () => {
    http.post
      .mockResolvedValueOnce({ data: { data: true } })
      .mockResolvedValueOnce({ data: { data: null } })
      .mockResolvedValueOnce({ data: { data: '202' } })

    await expect(toggleLike('101')).resolves.toBe(true)
    await reportPlan('101', { reason: 'SPAM', detail: '' })
    await expect(copyPlan('101', { title: '복사본' })).resolves.toBe('202')

    expect(http.post).toHaveBeenNthCalledWith(1, '/plans/101/like')
    expect(http.post).toHaveBeenNthCalledWith(2, '/plans/101/report', {
      reason: 'SPAM',
      detail: '',
    })
    expect(http.post).toHaveBeenNthCalledWith(3, '/plans/101/copy', { title: '복사본' })
  })
})
