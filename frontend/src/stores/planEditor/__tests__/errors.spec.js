import { describe, expect, it } from 'vitest'

import {
  createLocalScheduleError,
  isScheduleConflict,
  planEditorErrorMessage,
  scheduleSaveErrorMessage,
  shouldRefreshScheduleAfterError,
} from '@/stores/planEditor/errors'

describe('planEditor errors', () => {
  it('인증 및 기본 조회 오류를 사용자 메시지로 변환한다', () => {
    expect(planEditorErrorMessage({ response: { status: 401 } })).toContain('로그인 후')
    expect(planEditorErrorMessage(new Error('failed'))).toContain('여행 계획을 불러오지 못했습니다')
  })

  it('schedule conflict와 refresh 대상 오류를 분류한다', () => {
    expect(isScheduleConflict('SCHEDULE_VERSION_CONFLICT')).toBe(true)
    expect(isScheduleConflict('INVALID_SCHEDULE_ORDER')).toBe(false)
    expect(shouldRefreshScheduleAfterError('INVALID_SCHEDULE_ORDER')).toBe(true)
    expect(shouldRefreshScheduleAfterError('SCHEDULE_ITEM_ALREADY_EXISTS')).toBe(false)
  })

  it('충돌 복구 여부에 따라 schedule 오류 메시지를 구분한다', () => {
    const error = { response: { data: { code: 'ITEM_VERSION_CONFLICT' } } }

    expect(scheduleSaveErrorMessage(error, true)).toContain('최신 일정을 다시 불러왔습니다')
    expect(scheduleSaveErrorMessage(error, false)).toContain('최신 일정을 불러온 뒤')
  })

  it('local schedule 오류에 사용자 메시지를 보존한다', () => {
    const error = createLocalScheduleError('일차를 찾을 수 없습니다.')

    expect(error.message).toBe('일차를 찾을 수 없습니다.')
    expect(error.userMessage).toBe('일차를 찾을 수 없습니다.')
  })
})
