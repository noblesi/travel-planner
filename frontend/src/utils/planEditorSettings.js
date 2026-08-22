import { inclusiveDayCount } from '@/utils/travelDate'

const PLAN_VISIBILITIES = new Set(['PUBLIC', 'PRIVATE'])

export function validatePlanMetadata({ title, visibility }) {
  const normalizedTitle = title.trim()

  if (!normalizedTitle) {
    return { field: 'title', message: '플랜 제목을 입력해 주세요.' }
  }
  if (normalizedTitle.length > 200) {
    return { field: 'title', message: '플랜 제목은 200자 이하로 입력해 주세요.' }
  }
  if (!PLAN_VISIBILITIES.has(visibility)) {
    return { field: 'visibility', message: '공개 범위를 다시 선택해 주세요.' }
  }

  return null
}

export function metadataSaveErrorMessage(error) {
  if (error?.response?.data?.code === 'PLAN_VERSION_CONFLICT') {
    return '다른 변경이 먼저 저장되어 최신 플랜 정보를 불러왔습니다. 입력 내용을 확인한 뒤 다시 저장해 주세요.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '플랜 정보를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export function getPlanDateStatus(plan, today) {
  const isCompleted = Boolean(plan?.endDate && plan.endDate < today)
  const isOngoing = Boolean(
    plan?.startDate &&
      plan?.endDate &&
      plan.startDate <= today &&
      plan.endDate >= today,
  )

  return { isCompleted, isOngoing }
}

export function validatePlanDates({ startDate, endDate, plan, today }) {
  if (!startDate || !endDate) return '시작일과 종료일을 모두 선택해 주세요.'

  const { isCompleted, isOngoing } = getPlanDateStatus(plan, today)
  if (isCompleted) return '종료된 여행 플랜의 날짜는 변경할 수 없습니다.'
  if (isOngoing && startDate !== plan.startDate) {
    return '진행 중인 여행의 시작일은 변경할 수 없습니다.'
  }
  if (!isOngoing && startDate < today) {
    return '여행 시작일은 오늘보다 빠를 수 없습니다.'
  }
  if (isOngoing && endDate < today) {
    return '진행 중인 여행의 종료일은 오늘보다 빠를 수 없습니다.'
  }
  if (startDate > endDate) return '종료일은 시작일보다 빠를 수 없습니다.'
  if (inclusiveDayCount(startDate, endDate) > 14) {
    return '여행 기간은 최대 14일까지 설정할 수 있습니다.'
  }

  return ''
}

export function dateSaveErrorMessage(error) {
  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '여행 날짜를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}
