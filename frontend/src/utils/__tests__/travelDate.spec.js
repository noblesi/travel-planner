import { describe, expect, it } from 'vitest'

import {
  addDaysToDate,
  formatKoreanTravelDate,
  formatPeriodDate,
  formatShortTravelDate,
  inclusiveDayCount,
  todayInKorea,
} from '@/utils/travelDate'

describe('travelDate', () => {
  it('한국 시간 자정 전후를 서로 다른 오늘 날짜로 계산한다', () => {
    expect(todayInKorea(new Date('2026-01-01T14:59:59Z'))).toBe('2026-01-01')
    expect(todayInKorea(new Date('2026-01-01T15:00:00Z'))).toBe('2026-01-02')
  })

  it('윤년을 포함해 날짜 덧셈과 포함 일수를 계산한다', () => {
    expect(addDaysToDate('2024-02-20', 13)).toBe('2024-03-04')
    expect(inclusiveDayCount('2024-02-28', '2024-03-01')).toBe(3)
    expect(inclusiveDayCount('2025-02-28', '2025-03-01')).toBe(2)
  })

  it('실행 환경 시간대와 관계없이 여행 날짜를 한국어로 표시한다', () => {
    expect(formatPeriodDate('2026-08-04')).toBe('08.04')
    expect(formatShortTravelDate('2026-08-04')).toBe('8/4(화)')
    expect(formatKoreanTravelDate('2026-08-04')).toContain('8월 4일')
  })

  it('잘못된 날짜 입력에는 안전한 기본값을 반환한다', () => {
    expect(addDaysToDate('invalid', 13)).toBe('')
    expect(inclusiveDayCount('', '2026-01-01')).toBe(0)
    expect(formatKoreanTravelDate()).toBe('')
  })
})
