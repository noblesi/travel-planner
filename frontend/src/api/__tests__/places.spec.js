import { beforeEach, describe, expect, it, vi } from 'vitest'

import http from '@/api/http'
import { searchPlaces } from '@/api/places'

vi.mock('@/api/http', () => ({
  default: {
    get: vi.fn(),
  },
}))

beforeEach(() => {
  vi.clearAllMocks()
})

describe('searchPlaces', () => {
  it('검색 조건을 Query Parameter로 전달하고 응답 data를 반환한다', async () => {
    const result = { places: [], page: 2, size: 10, totalCount: 12, hasNext: false }
    http.get.mockResolvedValue({ data: { data: result } })

    await expect(
      searchPlaces({
        keyword: '  한강  ',
        regionCode: '1',
        category: '관광지',
        page: 2,
        size: 10,
      }),
    ).resolves.toEqual(result)
    expect(http.get).toHaveBeenCalledWith('/places/search', {
      params: { keyword: '한강', regionCode: '1', category: '관광지', page: 2, size: 10 },
    })
  })

  it('지역코드가 없으면 전국 검색으로 요청한다', async () => {
    http.get.mockResolvedValue({
      data: { data: { places: [], page: 1, size: 10, totalCount: 0, hasNext: false } },
    })

    await searchPlaces({ keyword: '박물관' })

    expect(http.get).toHaveBeenCalledWith('/places/search', {
      params: { keyword: '박물관', page: 1, size: 10 },
    })
  })
})
