import { describe, expect, it } from 'vitest'

import {
  dateSaveErrorMessage,
  getPlanDateStatus,
  metadataSaveErrorMessage,
  validatePlanDates,
  validatePlanMetadata,
} from '@/utils/planEditorSettings'

const plan = {
  startDate: '2026-08-10',
  endDate: '2026-08-11',
}

describe('planEditorSettings', () => {
  it('플랜 제목과 공개 범위를 검증한다', () => {
    expect(validatePlanMetadata({ title: '   ', visibility: 'PRIVATE' })).toEqual({
      field: 'title',
      message: '플랜 제목을 입력해 주세요.',
    })
    expect(validatePlanMetadata({ title: '서울 여행', visibility: 'UNKNOWN' })).toEqual({
      field: 'visibility',
      message: '공개 범위를 다시 선택해 주세요.',
    })
    expect(validatePlanMetadata({ title: '서울 여행', visibility: 'PUBLIC' })).toBeNull()
  })

  it('예정·진행 중·종료된 플랜 상태를 계산한다', () => {
    expect(getPlanDateStatus(plan, '2026-08-04')).toEqual({
      isCompleted: false,
      isOngoing: false,
    })
    expect(getPlanDateStatus(plan, '2026-08-10')).toEqual({
      isCompleted: false,
      isOngoing: true,
    })
    expect(getPlanDateStatus(plan, '2026-08-12')).toEqual({
      isCompleted: true,
      isOngoing: false,
    })
  })

  it('여행 날짜의 상태별 제약과 최대 기간을 검증한다', () => {
    expect(
      validatePlanDates({
        startDate: '2026-08-03',
        endDate: '2026-08-11',
        plan,
        today: '2026-08-04',
      }),
    ).toBe('여행 시작일은 오늘보다 빠를 수 없습니다.')
    expect(
      validatePlanDates({
        startDate: '2026-08-05',
        endDate: '2026-08-19',
        plan,
        today: '2026-08-04',
      }),
    ).toBe('여행 기간은 최대 14일까지 설정할 수 있습니다.')
    expect(
      validatePlanDates({
        startDate: '2026-08-05',
        endDate: '2026-08-18',
        plan,
        today: '2026-08-04',
      }),
    ).toBe('')
  })

  it('서버 오류 메시지가 없으면 기능별 기본 메시지를 반환한다', () => {
    expect(metadataSaveErrorMessage(new Error('failed'))).toContain('플랜 정보를 변경하지 못했습니다')
    expect(dateSaveErrorMessage(new Error('failed'))).toContain('여행 날짜를 변경하지 못했습니다')
    expect(
      metadataSaveErrorMessage({ response: { data: { code: 'PLAN_VERSION_CONFLICT' } } }),
    ).toContain('최신 플랜 정보를 불러왔습니다')
  })
})
