const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_PATTERN = /^\d{2,3}-?\d{3,4}-?\d{4}$/
const GENDER_CODES = new Set(['M', 'F', 'N'])

export function normalizeJoinEmail(email) {
  return email.trim().toLowerCase()
}

export function validateJoinCredentials({ email, password, passwordConfirmation }) {
  const normalizedEmail = normalizeJoinEmail(email)
  if (!normalizedEmail) return '이메일 주소를 입력해 주세요.'
  if (normalizedEmail.length > 255 || !EMAIL_PATTERN.test(normalizedEmail)) {
    return '올바른 이메일 주소를 입력해 주세요.'
  }
  if (!password) return '비밀번호를 입력해 주세요.'
  if (password.length < 10 || password.length > 72) {
    return '비밀번호는 10자 이상 72자 이하로 입력해 주세요.'
  }
  if (password !== passwordConfirmation) return '비밀번호 확인이 일치하지 않습니다.'
  return ''
}

export function validateJoinProfile({ name, birth, phone, gender, privacy }, today = new Date()) {
  const normalizedName = name.trim()
  const normalizedPhone = phone.trim()

  if (!normalizedName) return '이름을 입력해 주세요.'
  if (normalizedName.length > 10) return '이름은 10자 이하로 입력해 주세요.'
  if (!isPastCalendarDate(birth, today)) {
    return '생년월일을 8자리로 정확하게 입력해 주세요. (예: 20001031)'
  }
  if (!normalizedPhone) return '전화번호를 입력해 주세요.'
  if (normalizedPhone.length > 20 || !PHONE_PATTERN.test(normalizedPhone)) {
    return '전화번호 형식이 올바르지 않습니다.'
  }
  if (!GENDER_CODES.has(gender)) return '성별을 선택해 주세요.'
  if (!privacy) return '개인정보 저장에 동의해 주세요.'
  return ''
}

function isPastCalendarDate(value, today) {
  if (!/^\d{8}$/.test(value)) return false

  const year = Number(value.slice(0, 4))
  const month = Number(value.slice(4, 6))
  const day = Number(value.slice(6, 8))
  const date = new Date(year, month - 1, day)
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate())

  return (
    date.getFullYear() === year &&
    date.getMonth() === month - 1 &&
    date.getDate() === day &&
    date < startOfToday
  )
}
