<script setup>
import { ref } from 'vue'
import { isNavigationFailure, RouterLink, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { getSafeAuthenticationRedirect } from '@/utils/authRedirect'

const email = ref('')
const password = ref('')
const navigationError = ref('')
const loginCompleted = ref(false)
const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const handleContinue = async () => {
  if (authStore.pending || loginCompleted.value) return

  if (!email.value) {
    return
  }

  if (!password.value) {
    return
  }

  navigationError.value = ''
  try {
    await authStore.login({ email: email.value.trim(), password: password.value })
  } catch {
    password.value = ''
    return
  }

  password.value = ''
  loginCompleted.value = true
  try {
    const failure = await router.replace(getSafeAuthenticationRedirect(route.query.redirect))
    if (!isNavigationFailure(failure)) return
  } catch {
    navigationError.value =
      '로그인은 완료되었지만 다음 화면으로 이동하지 못했습니다. 새로고침해 주세요.'
    return
  }

  navigationError.value =
    '로그인은 완료되었지만 다음 화면으로 이동하지 못했습니다. 새로고침해 주세요.'
}
</script>
<template>
  <div class="auth-container">
    <div class="auth-box">
      <!-- 메인 타이틀 -->
      <header class="auth-header">
        <h1 class="main-title">
          나만의 플랜을 계획해보세요<br />
          <span>로그인</span>
        </h1>
      </header>

      <!-- 로그인 폼 -->
      <form class="auth-form" :aria-busy="authStore.pending" @submit.prevent="handleContinue">
        <div class="input-group">
          <label class="input-label" for="login-email">이메일</label>
          <input 
            id="login-email"
            type="email" 
            v-model="email" 
            placeholder="이메일 주소를 입력하세요" 
            autocomplete="email"
            maxlength="255"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="login-password">비밀번호</label>
          <input 
            id="login-password"
            type="password" 
            v-model="password" 
            placeholder="비밀번호를 입력하세요" 
            autocomplete="current-password"
            maxlength="72"
            required
          />
        </div>

        <div class="error-container" aria-live="polite">
          <span v-if="navigationError || authStore.errorMessage" class="error-message" role="alert">
            {{ navigationError || authStore.errorMessage }}
          </span>
        </div>

        <button type="submit" class="btn-primary" :disabled="authStore.pending || loginCompleted">
          {{ loginCompleted ? '로그인 완료' : authStore.pending ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <!-- 하단 네비게이션 가이드 -->
      <div class="footer-links">
        <div class="recovery-links">
          <RouterLink :to="{ name: 'emailFind' }">이메일 찾기</RouterLink>
          <span class="divider" aria-hidden="true">·</span>
          <RouterLink :to="{ name: 'passwordFind' }">비밀번호 재설정</RouterLink>
        </div>
        <p class="signup-prompt">
          신규 사용자이신가요? <RouterLink :to="{ name: 'join' }">가입하기</RouterLink>
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
  /* 마이페이지와 동일한 배경 색감 적용 */
  background: radial-gradient(circle at 90% 5%, rgb(249 115 22 / 10%), transparent 28rem), var(--color-page, #fafaf9);
  padding: 40px 20px;
  box-sizing: border-box;
}

.auth-box {
  width: 100%;
  max-width: 420px;
  background: var(--color-surface, #ffffff);
  border: 1px solid var(--color-border, #e5e7eb);
  padding: 48px 40px;
  border-radius: 20px;
  box-shadow: 0 16px 45px rgb(15 23 42 / 6%);
  box-sizing: border-box;
}

.auth-header {
  text-align: center;
  margin-bottom: 36px;
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
  gap: 20px;
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

input {
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

input:focus {
  border-color: var(--color-brand, #f97316);
  background: var(--color-surface, #ffffff);
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
  margin-top: 8px;
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
  margin-top: 32px;
  text-align: center;
}

.recovery-links {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 13px;
}

.recovery-links a {
  color: var(--color-text-muted, #6b7280);
  text-decoration: none;
  transition: color 0.2s;
}

.recovery-links a:hover {
  color: var(--color-text, #111827);
}

.divider {
  color: var(--color-border, #d1d5db);
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