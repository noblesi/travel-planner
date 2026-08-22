import { describe, expect, it } from 'vitest'

import {
  createPlanRestoreRequests,
  formatCompactCount,
  getRegionColorKey,
  mapPublicPlan,
  parsePlanSearchRoute,
} from '@/utils/planSearch'

describe('plan search utilities', () => {
  it('route query를 안전한 검색 상태로 변환한다', () => {
    expect(parsePlanSearchRoute({ keyword: '제주', page: '3' })).toEqual({
      keyword: '제주',
      page: 3,
    })
    expect(parsePlanSearchRoute({ page: '-1' }).page).toBe(1)
    expect(parsePlanSearchRoute({ page: '2.8' }).page).toBe(2)
    expect(parsePlanSearchRoute({ page: '999' }).page).toBe(25)
    expect(parsePlanSearchRoute({ count: '24' }).page).toBe(3)
  })

  it('누적 25페이지 복원을 100건 단위 2개 요청으로 줄인다', () => {
    expect(createPlanRestoreRequests('제주', 25)).toEqual([
      { keyword: '제주', page: 1, size: 100 },
      { keyword: '제주', page: 2, size: 100 },
    ])
  })

  it('작은 누적 범위는 한 요청으로 정확한 레코드 수만 조회한다', () => {
    expect(createPlanRestoreRequests('', 2)).toEqual([{ keyword: '', page: 1, size: 16 }])
  })

  it('API plan을 안전한 card model로 변환한다', () => {
    expect(
      mapPublicPlan({
        planId: '101',
        title: '여행',
        region: null,
        days: 2,
        likeCount: 1,
        viewCount: 1200,
        authorName: null,
        authorImage: null,
        thumbnailImage: null,
      }),
    ).toEqual({
      id: '101',
      title: '여행',
      region: '지역 미정',
      days: 2,
      likeCount: 1,
      viewCount: 1200,
      authorInitials: '여행',
      authorName: '여행자',
      authorAvatar: null,
      thumbnailImage: null,
    })
    expect(formatCompactCount(1200)).toBe('1.2k')
    expect(getRegionColorKey(null)).toBe('neutral')
  })
})
