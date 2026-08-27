<script setup>
import { ref } from 'vue'
import { isNavigationFailure, useRouter } from 'vue-router'

import { postMemberJoin } from '@/api/users'
import { useJoinDraftStore } from '@/stores/joinDraft'
import { validateJoinProfile } from '@/utils/joinValidation'

const router = useRouter()
const joinDraftStore = useJoinDraftStore()

const birth = ref('')
const name = ref('')
const gender = ref('N')
const phone = ref('')
const privacy = ref(false)
const errorMessage = ref('')
const isSubmitting = ref(false)
const isRegistrationCompleted = ref(false)

async function replaceRoute(location, failureMessage) {
  try {
    const failure = await router.replace(location)
    if (!isNavigationFailure(failure)) return true
  } catch {
    errorMessage.value = failureMessage
    return false
  }

  errorMessage.value = failureMessage
  return false
}

async function handleContinue() {
  if (isSubmitting.value || isRegistrationCompleted.value) return

  errorMessage.value = validateJoinProfile({
    name: name.value,
    birth: birth.value,
    phone: phone.value,
    gender: gender.value,
    privacy: privacy.value,
  })
  if (errorMessage.value) return

  let payload
  try {
    const normalizedName = name.value.trim()
    payload = joinDraftStore.buildRegistrationPayload({
      name: normalizedName,
      nickname: normalizedName,
      gender: gender.value,
      birth: birth.value,
      privacy: 'Y',
      phone: phone.value.trim(),
    })
  } catch {
    await replaceRoute(
      { name: 'join', query: { reset: true } },
      '회원가입 정보를 다시 입력할 화면으로 이동하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    )
    return
  }

  isSubmitting.value = true
  try {
    try {
      await postMemberJoin(payload)
    } catch (error) {
      errorMessage.value =
        error?.response?.data?.message ||
        '회원가입을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      return
    }

    isRegistrationCompleted.value = true
    joinDraftStore.clearRegistration()
    await replaceRoute(
      { name: 'complete' },
      '회원가입은 완료되었지만 완료 화면으로 이동하지 못했습니다. 로그인 화면에서 로그인해 주세요.',
    )
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-box">
      <!-- 헤더 -->
      <header class="auth-header">
        <h1 class="main-title">
          거의 다 왔어요 조금만 더 하면 돼요<br />
          <span>프로필 등록하기</span>
        </h1>
      </header>

      <!-- 프로필 등록 폼 -->
      <form class="auth-form" @submit.prevent="handleContinue">
        <div class="input-group">
          <label class="input-label" for="join-name">이름</label>
          <input 
            id="join-name"
            type="text" 
            v-model="name" 
            placeholder="이름을 입력해주세요." 
            autocomplete="name"
            maxlength="10"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="join-birth">생년월일</label>
          <input 
            id="join-birth"
            type="text" 
            v-model="birth" 
            placeholder="생년월일 8자리 (예: 20001031)" 
            autocomplete="bday"
            inputmode="numeric"
            maxlength="8"
            pattern="[0-9]{8}"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="join-phone">전화번호</label>
          <input 
            id="join-phone"
            type="text" 
            v-model="phone" 
            placeholder="전화번호 (예: 010-1234-5678)" 
            autocomplete="tel"
            inputmode="tel"
            maxlength="20"
            required
          />
        </div>

        <!-- 성별 선택 세그먼트 -->
        <fieldset class="input-group fieldset-reset">
          <legend class="input-label">성별</legend>
          <div class="gender-options">
            <label class="gender-option" :class="{ active: gender === 'M' }">
              <input type="radio" name="gender" v-model="gender" value="M" />
              <span>남성</span>
            </label>
            <label class="gender-option" :class="{ active: gender === 'F' }">
              <input type="radio" name="gender" v-model="gender" value="F" />
              <span>여성</span>
            </label>
            <label class="gender-option" :class="{ active: gender === 'N' }">
              <input type="radio" name="gender" v-model="gender" value="N" />
              <span>선택안함</span>
            </label>
          </div>
        </fieldset>

        <!-- 개인정보 수집 동의 -->
        <div class="privacy-consent">
          <label class="checkbox-label" for="join-privacy">
            <input id="join-privacy" type="checkbox" class="input-checkbox" v-model="privacy" />
            <span>개인정보 저장 및 수집에 동의합니다.</span>
          </label>
        </div>

        <div class="error-container" aria-live="polite">
          <span v-if="errorMessage" class="error-message" role="alert">
            {{ errorMessage }}
          </span>
        </div>

        <button
          type="submit"
          class="btn-primary"
          :disabled="isSubmitting || isRegistrationCompleted"
        >
          {{ isRegistrationCompleted ? '가입 완료' : isSubmitting ? '가입 처리 중...' : '가입하기' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: radial-gradient(circle at 90% 5%, rgb(249 115 22 / 10%), transparent 28rem), var(--color-page, #fafaf9);
  padding: 40px 20px;
  box-sizing: border-box;
}

.auth-box {
  width: 100%;
  max-width: 420px;
  background: var(--color-surface, #ffffff);
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 40px 36px;
  border-radius: 20px;
  box-shadow: 0 16px 45px rgb(15 23 42 / 6%);
  box-sizing: border-box;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.main-title {
  margin: 0;
  color: var(--color-text, #111827);
  font-size: clamp(22px, 3vw, 26px);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1.4;
}

.main-title span {
  display: block;
  margin-top: 8px;
  color: var(--color-text-muted, #6b7280);
  font-size: 15px;
  font-weight: 500;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fieldset-reset {
  border: none;
  padding: 0;
  margin: 0;
}

.input-label {
  font-size: 13px;
  color: var(--color-text, #374151);
  font-weight: 600;
}

input[type="text"] {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 12px;
  font-size: 15px;
  color: var(--color-text, #111827);
  background: var(--color-page, #fafaf9);
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s, background-color 0.2s;
}

input[type="text"]:focus {
  border-color: var(--color-brand, #f97316);
  background: var(--color-surface, #ffffff);
}

/* 성별 선택 버튼 세그먼트 스타일 */
.gender-options {
  display: flex;
  gap: 8px;
  width: 100%;
}

.gender-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  background: var(--color-page, #fafaf9);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  transition: all 0.2s ease;

  input[type="radio"] {
    display: none;
  }
}

.gender-option.active {
  background: #fff7ed;
  border-color: var(--color-brand, #f97316);
  color: var(--color-brand, #f97316);
  font-weight: 600;
}

/* 개인정보 동의 체크박스 */
.privacy-consent {
  margin-top: 4px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13.5px;
  color: var(--color-text, #374151);
  cursor: pointer;
  user-select: none;
}

.input-checkbox {
  width: 18px;
  height: 18px;
  accent-color: var(--color-brand, #f97316);
  cursor: pointer;
  margin: 0;
}

.error-container {
  min-height: 20px;
  text-align: center;
}

.error-message {
  color: var(--color-danger, #ef4444);
  font-size: 13px;
  font-weight: 500;
}

.btn-primary {
  display: inline-flex;
  width: 100%;
  height: 52px;
  align-items: center;
  justify-content: center;
  background: var(--color-brand, #f97316);
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
  margin-top: 4px;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:active:not(:disabled) {
  transform: scale(0.98);
}

.btn-primary:disabled {
  background: var(--color-text-muted, #9ca3af);
  cursor: wait;
  opacity: 0.7;
}
</style>