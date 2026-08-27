<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { findEmail as requestEmailRecovery } from '@/api/find'

const router = useRouter()
const name = ref('')
const birth = ref('')
const phone = ref('')
const isModalOpen = ref(false)
const foundEmail = ref('')
const pending = ref(false)
const errorMessage = ref('')

async function findEmail() {
  if (pending.value) return

  pending.value = true
  errorMessage.value = ''
  try {
    foundEmail.value = await requestEmailRecovery({
      memberName: name.value.trim(),
      birthDate: birth.value,
      phoneNumber: phone.value.trim(),
    })
    isModalOpen.value = true
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message || '이메일을 찾지 못했습니다. 입력한 정보를 확인해 주세요.'
  } finally {
    pending.value = false
  }
}

async function goToLogin() {
  isModalOpen.value = false
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-box">
      <!-- 메인 타이틀 -->
      <header class="auth-header">
        <h1 class="main-title">
            나만의 플랜을 계획해보세요<br />
            <span>내 이메일 찾기</span>
        </h1>
      </header>

      <!-- 폼 영역 -->
      <form class="auth-form" :aria-busy="pending" @submit.prevent="findEmail">
        <div class="input-group">
          <label class="input-label" for="recovery-name">이름</label>
          <input 
            id="recovery-name"
            v-model="name"
            type="text" 
            placeholder="이름을 입력해주세요" 
            maxlength="10"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="recovery-birth">생년월일</label>
          <input 
            id="recovery-birth"
            v-model="birth"
            type="date" 
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="recovery-phone">전화번호</label>
          <input 
            id="recovery-phone"
            v-model="phone"
            type="text" 
            placeholder="예) 010-1234-5689" 
            maxlength="20"
            required
          />
        </div>

        <div class="error-container">
          <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
        </div>

        <button type="submit" class="btn-primary" :disabled="pending">
          {{ pending ? '확인 중...' : '이메일 찾기' }}
        </button>
      </form>

      <div class="footer-links">
        <p class="signup-prompt">
          신규 사용자이신가요? <RouterLink :to="{ name: 'join' }">가입하기</RouterLink>
        </p>
      </div>
    </div>

    <!-- 이메일 찾기 완료 모달 -->
    <Teleport to="body">
      <div v-if="isModalOpen" class="modal-overlay">
        <div class="modal-card">
          <h2 class="modal-title">이메일 찾기 완료</h2>
          <p class="modal-content">
            개인정보 보호를 위해 일부를 가린 이메일입니다.<br />
            <strong>{{ foundEmail }}</strong>
          </p>
          <button type="button" class="btn-primary" @click="goToLogin">
            확인 후 로그인하기
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* 로그인 폼과 동일한 컨테이너 스타일 */
.auth-container {
  display: flex; justify-content: center; align-items: center; min-height: 100vh;
  background: radial-gradient(circle at 90% 5%, rgb(249 115 22 / 10%), transparent 28rem), var(--color-page, #fafaf9);
  padding: 40px 20px; box-sizing: border-box;
}

.auth-box {
  width: 100%; max-width: 420px; background: var(--color-surface, #ffffff);
  border: 1px solid var(--color-border, #e5e7eb); padding: 48px 40px;
  border-radius: 20px; box-shadow: 0 16px 45px rgb(15 23 42 / 6%); box-sizing: border-box;
}

.auth-header { text-align: center; margin-bottom: 36px; }
.main-title { margin: 0; color: var(--color-text, #111827); font-size: clamp(22px, 3vw, 26px); font-weight: 700; letter-spacing: -0.04em; line-height: 1.4; }
.main-title span { display: block; margin-top: 8px; color: var(--color-text-muted, #6b7280); font-size: 15px; font-weight: 500; }

.auth-form { display: flex; flex-direction: column; gap: 20px; }
.input-group { display: flex; flex-direction: column; gap: 8px; }
.input-label { font-size: 13px; color: var(--color-text, #374151); font-weight: 600; }
input {
  width: 100%; height: 48px; padding: 0 16px; border: 1px solid var(--color-border, #e5e7eb); border-radius: 12px;
  font-size: 15px; color: var(--color-text, #111827); background: var(--color-page, #fafaf9); box-sizing: border-box; outline: none; transition: border-color 0.2s, background-color 0.2s;
}
input:focus { border-color: var(--color-brand, #f97316); background: var(--color-surface, #ffffff); }

.error-container { min-height: 16px; text-align: center; margin-top: -8px; }
.error-message { color: var(--color-danger, #ef4444); font-size: 13px; font-weight: 500; margin: 0; }

.btn-primary {
  display: inline-flex; width: 100%; height: 52px; align-items: center; justify-content: center;
  background: var(--color-brand, #f97316); color: #ffffff; border: none; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer; transition: opacity 0.2s;
}
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-primary:disabled { background: var(--color-text-muted, #9ca3af); cursor: wait; opacity: 0.7; }

.footer-links { margin-top: 32px; text-align: center; }
.signup-prompt { font-size: 14px; color: var(--color-text-muted, #6b7280); margin: 0; }
.signup-prompt a { color: var(--color-brand, #f97316); font-weight: 600; text-decoration: none; margin-left: 4px; }
.signup-prompt a:hover { text-decoration: underline; }

/* 모달 스타일 */
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  background-color: rgba(17, 24, 39, 0.4); display: flex; justify-content: center; align-items: center; z-index: 9999; backdrop-filter: blur(4px);
}
.modal-card {
  width: 90%; max-width: 360px; padding: 36px 30px;
  background: var(--color-surface, #ffffff); border-radius: 20px; text-align: center; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  animation: modalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.modal-title { font-size: 20px; font-weight: 700; color: var(--color-text, #111827); margin: 0 0 12px 0; letter-spacing: -0.04em; }
.modal-content { font-size: 14px; color: var(--color-text-muted, #6b7280); line-height: 1.6; margin: 0 0 28px 0; }
.modal-content strong { display: block; margin-top: 12px; font-size: 20px; color: var(--color-brand, #f97316); font-weight: 700; letter-spacing: 0; }

@keyframes modalPop {
  from { opacity: 0; transform: scale(0.9) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>