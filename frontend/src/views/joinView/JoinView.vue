<script setup>
import { computed, ref } from 'vue'
import { isNavigationFailure, RouterLink, useRouter } from 'vue-router'

import { getMemberEmailCheck } from '@/api/users'
import { useJoinDraftStore } from '@/stores/joinDraft'
import { normalizeJoinEmail, validateJoinCredentials } from '@/utils/joinValidation'

const joinDraftStore = useJoinDraftStore()
const router = useRouter()
const email = ref('')
const password = ref('')
const passwordConfirmation = ref('')
const errorMessage = ref('')
const isCheckingEmail = ref(false)

const isPasswordMatched = computed(() => {
  if (!passwordConfirmation.value) return true
  return password.value === passwordConfirmation.value
})

function goBack() {
  router.back()
}

async function navigateToProfile() {
  try {
    const failure = await router.push({ name: 'joinProfile' })
    if (!isNavigationFailure(failure)) return true
  } catch {
    errorMessage.value =
      '이메일 확인은 완료됐지만 다음 화면으로 이동하지 못했습니다. 다시 시도해 주세요.'
    return false
  }

  errorMessage.value =
    '이메일 확인은 완료됐지만 다음 화면으로 이동하지 못했습니다. 다시 시도해 주세요.'
  return false
}

async function handleContinue() {
  if (isCheckingEmail.value) return

  errorMessage.value = validateJoinCredentials({
    email: email.value,
    password: password.value,
    passwordConfirmation: passwordConfirmation.value,
  })
  if (errorMessage.value) return

  const normalizedEmail = normalizeJoinEmail(email.value)
  isCheckingEmail.value = true
  try {
    let isRegistered
    try {
      isRegistered = await getMemberEmailCheck(normalizedEmail)
    } catch (error) {
      errorMessage.value =
        error?.response?.data?.message ||
        '이메일 사용 가능 여부를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      return
    }

    if (isRegistered) {
      errorMessage.value = '이미 사용 중인 이메일입니다. 다른 이메일을 입력해 주세요.'
      return
    }

    joinDraftStore.beginRegistration({
      email: normalizedEmail,
      password: password.value,
    })
    await navigateToProfile()
  } finally {
    isCheckingEmail.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-box">
      <!-- 헤더 및 뒤로가기 버튼 -->
      <header class="auth-header">
        <div class="header-top">
          <button
            type="button"
            class="back-button"
            aria-label="이전 화면으로 이동"
            @click="goBack"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m15 18-6-6 6-6"/>
            </svg>
          </button>
        </div>
        <h1 class="main-title">
          나만의 플랜을 계획해보세요<br />
          <span>회원가입</span>
        </h1>
      </header>

      <!-- 회원가입 폼 -->
      <form class="auth-form" @submit.prevent="handleContinue">
        <div class="input-group">
          <label class="input-label" for="join-email">이메일</label>
          <input 
            id="join-email"
            type="email" 
            v-model="email" 
            placeholder="이메일 주소를 입력하세요." 
            autocomplete="email"
            maxlength="255"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="join-password">비밀번호</label>
          <input 
            id="join-password"
            type="password"
            v-model="password" 
            placeholder="비밀번호를 입력해주세요." 
            autocomplete="new-password"
            minlength="10"
            maxlength="72"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="join-password-confirmation">비밀번호 확인</label>
          <input 
            id="join-password-confirmation"
            type="password" 
            v-model="passwordConfirmation"
            placeholder="비밀번호를 한번 더 입력해주세요."
            autocomplete="new-password"
            minlength="10"
            maxlength="72"
            required
          />
          <!-- 비밀번호 확인 상태 문구 -->
          <div
            v-if="passwordConfirmation"
            class="password-match-status"
            aria-live="polite"
          >
            <span v-if="!isPasswordMatched" class="status-mismatch">
              ❌ 비밀번호가 일치하지 않습니다.
            </span>
            <span v-else class="status-match">
              ✅ 비밀번호가 일치합니다.
            </span>
          </div>
        </div>

        <div class="error-container" aria-live="polite">
          <span v-if="errorMessage" class="error-message" role="alert">
            {{ errorMessage }}
          </span>
        </div>

        <button
          type="submit"
          class="btn-primary"
          :disabled="isCheckingEmail"
          :aria-busy="isCheckingEmail"
        >
          {{ isCheckingEmail ? '확인 중...' : '다음으로' }}
        </button>
      </form>

      <!-- 하단 네비게이션 가이드 -->
      <div class="footer-links">
        <p class="signup-prompt">
          이미 계정이 있으신가요? <RouterLink :to="{ name: 'login' }">로그인</RouterLink>
        </p>
      </div>
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
  position: relative;
  text-align: center;
  margin-bottom: 32px;
}

.header-top {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-muted, #6b7280);
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.back-button:hover {
  background-color: var(--color-page, #fafaf9);
  color: var(--color-text, #111827);
}

.back-button:focus-visible {
  outline: 2px solid var(--color-brand, #f97316);
  outline-offset: 2px;
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

.input-label {
  font-size: 13px;
  color: var(--color-text, #374151);
  font-weight: 600;
}

input[type="email"],
input[type="password"] {
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

input[type="email"]:focus,
input[type="password"]:focus {
  border-color: var(--color-brand, #f97316);
  background: var(--color-surface, #ffffff);
}

.password-match-status {
  margin-top: 4px;
  font-size: 13px;
}

.status-mismatch {
  color: var(--color-danger, #ef4444);
  font-weight: 500;
}

.status-match {
  color: #10b981;
  font-weight: 500;
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

.footer-links {
  margin-top: 28px;
  text-align: center;
}

.signup-prompt {
  font-size: 14px;
  color: var(--color-text-muted, #6b7280);
  margin: 0;
}

.signup-prompt a {
  color: var(--color-brand, #f97316);
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}

.signup-prompt a:hover {
  text-decoration: underline;
}
</style>
