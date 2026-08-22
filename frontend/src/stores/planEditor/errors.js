const SCHEDULE_CONFLICT_CODES = new Set([
  'SCHEDULE_VERSION_CONFLICT',
  'ITEM_VERSION_CONFLICT',
  'DUPLICATE_OPERATION',
])

export function planEditorErrorMessage(error) {
  if (error?.response?.status === 401) {
    return '로그인 후 여행 계획을 편집할 수 있습니다.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '여행 계획을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export function scheduleSaveErrorMessage(error, refreshed) {
  const code = error?.response?.data?.code
  if (code === 'SCHEDULE_VERSION_CONFLICT' || code === 'ITEM_VERSION_CONFLICT') {
    return refreshed
      ? '다른 변경이 먼저 저장되어 최신 일정을 다시 불러왔습니다. 작업을 다시 시도해 주세요.'
      : '다른 변경이 먼저 저장되었습니다. 최신 일정을 불러온 뒤 다시 시도해 주세요.'
  }
  if (code === 'DUPLICATE_OPERATION') {
    return '자동 저장 작업 식별자가 충돌했습니다. 최신 일정으로 복구했으니 다시 시도해 주세요.'
  }
  if (code === 'SCHEDULE_ITEM_ALREADY_EXISTS') {
    return '선택한 시간대에 같은 장소가 이미 있습니다.'
  }
  if (code === 'SCHEDULE_ITEM_LIMIT_EXCEEDED') {
    return '시간대별 일정은 최대 100개까지 추가할 수 있습니다.'
  }
  if (code === 'INVALID_SCHEDULE_ORDER') {
    return refreshed
      ? '일정 순서가 달라져 최신 일정을 다시 불러왔습니다.'
      : '일정 순서를 저장하지 못했습니다.'
  }

  const message = error?.response?.data?.message ?? error?.userMessage
  return typeof message === 'string' && message
    ? message
    : '일정을 자동 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export function isScheduleConflict(code) {
  return SCHEDULE_CONFLICT_CODES.has(code)
}

export function shouldRefreshScheduleAfterError(code) {
  return isScheduleConflict(code) || code === 'INVALID_SCHEDULE_ORDER'
}

export function createLocalScheduleError(message) {
  const error = new Error(message)
  error.userMessage = message
  return error
}
