const PHONE_PATTERN = /^\d{2,3}-?\d{3,4}-?\d{4}$/
const GENDER_CODES = new Set(['M', 'F', 'N'])

export function createProfileForm(profile = {}) {
  return {
    name: profile.name || '',
    nickname: profile.nickname || '',
    genderCode: profile.genderCode || 'N',
    birthDate: profile.birthDate || '',
    phoneNumber: profile.phoneNumber || '',
  }
}

export function validateProfileForm(form, today = formatDateInput(new Date())) {
  const errors = {}
  const name = form.name.trim()
  const nickname = form.nickname.trim()
  const phoneNumber = form.phoneNumber.trim()

  if (!name) errors.name = '이름을 입력해 주세요.'
  else if (name.length > 10) errors.name = '이름은 10자 이하로 입력해 주세요.'

  if (!nickname) errors.nickname = '닉네임을 입력해 주세요.'
  else if (nickname.length > 50) errors.nickname = '닉네임은 50자 이하로 입력해 주세요.'

  if (!GENDER_CODES.has(form.genderCode)) errors.genderCode = '성별을 선택해 주세요.'
  if (form.birthDate && form.birthDate >= today) {
    errors.birthDate = '생년월일은 오늘보다 이전이어야 합니다.'
  }
  if (phoneNumber && !PHONE_PATTERN.test(phoneNumber)) {
    errors.phoneNumber = '전화번호 형식이 올바르지 않습니다.'
  }

  return errors
}

export function toProfileUpdatePayload(form) {
  return {
    name: form.name.trim(),
    nickname: form.nickname.trim(),
    genderCode: form.genderCode,
    birthDate: form.birthDate || null,
    phoneNumber: form.phoneNumber.trim() || null,
  }
}

export function mapServerFieldErrors(errors) {
  if (!Array.isArray(errors)) return {}
  return Object.fromEntries(
    errors
      .filter((error) => error?.field && error?.message)
      .map((error) => [error.field, error.message]),
  )
}

export function displayProfileValue(value) {
  return value || '미입력'
}

export function getGenderLabel(genderCode) {
  return { M: '남성', F: '여성' }[genderCode] || '미입력'
}

export function formatBirthDate(birthDate) {
  if (!birthDate) return '미입력'

  const date = new Date(`${birthDate}T00:00:00`)
  if (Number.isNaN(date.getTime())) return birthDate
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date)
}

export function getLatestBirthDate(currentDate = new Date()) {
  return formatDateInput(
    new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate() - 1),
  )
}

export function validatePasswordChange({ currentPassword, newPassword, newPasswordConfirm }) {
  if (!currentPassword) return '현재 비밀번호를 입력해 주세요.'
  if (newPassword.length < 10) return '새 비밀번호는 10자 이상 입력해 주세요.'
  if (newPassword.length > 72) return '새 비밀번호는 72자 이하로 입력해 주세요.'
  if (currentPassword === newPassword) {
    return '새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.'
  }
  if (newPassword !== newPasswordConfirm) return '새 비밀번호 확인이 일치하지 않습니다.'
  return ''
}

export function validateWithdrawalPassword(currentPassword) {
  return currentPassword.length >= 10 ? '' : '현재 비밀번호를 정확히 입력해 주세요.'
}

function formatDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
