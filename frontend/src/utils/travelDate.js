export const KOREA_TIME_ZONE = 'Asia/Seoul'

function dateParts(value) {
  if (typeof value !== 'string') return null
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value)
  if (!match) return null
  return {
    year: Number(match[1]),
    month: Number(match[2]),
    day: Number(match[3]),
  }
}

function dateAtStartOfDayInKorea(value) {
  return new Date(`${value}T00:00:00+09:00`)
}

export function todayInKorea(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: KOREA_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date)
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]))
  return `${values.year}-${values.month}-${values.day}`
}

export function addDaysToDate(value, days) {
  const parts = dateParts(value)
  if (!parts || !Number.isInteger(days)) return ''

  const date = new Date(Date.UTC(parts.year, parts.month - 1, parts.day + days))
  return date.toISOString().slice(0, 10)
}

export function inclusiveDayCount(startDate, endDate) {
  const start = dateParts(startDate)
  const end = dateParts(endDate)
  if (!start || !end) return 0

  const startTime = Date.UTC(start.year, start.month - 1, start.day)
  const endTime = Date.UTC(end.year, end.month - 1, end.day)
  return Math.floor((endTime - startTime) / 86_400_000) + 1
}

export function formatKoreanTravelDate(value) {
  if (!dateParts(value)) return ''
  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: KOREA_TIME_ZONE,
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(dateAtStartOfDayInKorea(value))
}

export function formatPeriodDate(value) {
  const parts = dateParts(value)
  if (!parts) return ''
  return `${String(parts.month).padStart(2, '0')}.${String(parts.day).padStart(2, '0')}`
}

export function formatShortTravelDate(value) {
  const parts = dateParts(value)
  if (!parts) return ''
  const weekday = new Intl.DateTimeFormat('ko-KR', {
    timeZone: KOREA_TIME_ZONE,
    weekday: 'short',
  }).format(dateAtStartOfDayInKorea(value))
  return `${parts.month}/${parts.day}(${weekday.replace('요일', '')})`
}
