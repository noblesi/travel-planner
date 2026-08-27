<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { resetRecoveredPassword, verifyPasswordRecovery } from '@/api/find'

const inputEmail = ref('')
const birth = ref('')
const phone = ref('')
const router = useRouter()
const isResetModalOpen = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')
const pending = ref(false)
const errorMessage = ref('')
const resetErrorMessage = ref('')

async function findPassword() {
  if (pending.value) return

  pending.value = true
  errorMessage.value = ''
  try {
    await verifyPasswordRecovery({
      email: inputEmail.value.trim(),
      birthDate: birth.value,
      phoneNumber: phone.value.trim(),
    })
    isResetModalOpen.value = true
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message || '회원정보를 확인하지 못했습니다. 입력값을 확인해 주세요.'
  } finally {
    pending.value = false
  }
}

const closeResetModal = () => {
  isResetModalOpen.value = false
  newPassword.value = ''
  confirmPassword.value = ''
  resetErrorMessage.value = ''
}

async function submitNewPassword() {
  if (pending.value) return

  resetErrorMessage.value = ''
  if (newPassword.value.length < 10 || newPassword.value.length > 72) {
    resetErrorMessage.value = '새 비밀번호는 10자 이상 72자 이하로 입력해 주세요.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    resetErrorMessage.value = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  pending.value = true
  try {
    await resetRecoveredPassword(newPassword.value)
    closeResetModal()
    await router.push({ name: 'login', query: { recovered: 'true' } })
  } catch (error) {
    resetErrorMessage.value =
      error?.response?.data?.message || '비밀번호를 변경하지 못했습니다. 다시 시도해 주세요.'
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-box">
      <!-- 메인 타이틀 -->
      <header class="auth-header">
        <h1 class="main-title">
            나만의 플랜을 계획해보세요<br />
            <span>비밀번호 재설정</span>
        </h1>
      </header>
      
      <!-- 폼 영역 -->
      <form class="auth-form" :aria-busy="pending" @submit.prevent="findPassword">
        <div class="input-group">
          <label class="input-label" for="password-recovery-email">이메일</label>
          <input 
            id="password-recovery-email"
            v-model="inputEmail"
            type="email" 
            placeholder="가입하신 이메일 주소" 
            autocomplete="email"
            maxlength="255"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="password-recovery-birth">생년월일</label>
          <input 
            id="password-recovery-birth"
            v-model="birth"
            type="date"
            required
          />
        </div>

        <div class="input-group">
          <label class="input-label" for="password-recovery-phone">전화번호</label>
          <input 
            id="password-recovery-phone"
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
          {{ pending ? '확인 중...' : '회원정보 확인' }}
        </button>
      </form>

      <div class="footer-links">
        <p class="signup-prompt">신규 사용자이신가요? <RouterLink :to="{ name: 'join' }">가입하기</RouterLink></p>
      </div>
    </div>

    <!-- 비밀번호 재설정 모달 창 -->
    <Teleport to="body">
      <div v-if="isResetModalOpen" class="modal-overlay" @click="closeResetModal">
        <form class="modal-card" :aria-busy="pending" @submit.prevent="submitNewPassword" @click.stop>
          
          <div class="modal-header">
            <h3 class="modal-title">비밀번호 재설정</h3>
            <p class="modal-subtitle">새롭게 사용할 비밀번호를 입력해 주세요.</p>
          </div>
          
          <div class="modal-form">
            <div class="input-group">
              <label>새 비밀번호</label>
              <input v-model="newPassword" type="password" placeholder="영문, 숫자, 특수문자 조합" autocomplete="new-password" minlength="10" maxlength="72" required />
            </div>
            
            <div class="input-group">
              <label>비밀번호 확인</label>
              <input v-model="confirmPassword" type="password" placeholder="새 비밀번호 다시 입력" autocomplete="new-password" minlength="10" maxlength="72" required />
            </div>
          </div>

          <div class="error-container modal-error">
            <p v-if="resetErrorMessage" class="error-message" role="alert">{{ resetErrorMessage }}</p>
          </div>

          <div class="modal-actions">
            <!-- 취소 버튼에 var(--color-brand-soft) 적용 -->
            <button type="button" class="btn-secondary" :disabled="pending" @click="closeResetModal">취소</button>
            <button type="submit" class="btn-primary" :disabled="pending">
              {{ pending ? '변경 중...' : '변경하기' }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* 공통 로그인 컨테이너 영역 */
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

/* 비밀번호 재설정 폼 모달 */
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  background-color: rgba(17, 24, 39, 0.5); display: flex; justify-content: center; align-items: center; z-index: 10000; backdrop-filter: blur(5px);
}

.modal-card {
  width: 90%; max-width: 400px; background: var(--color-surface, #ffffff);
  padding: 36px 32px; border-radius: 20px; box-shadow: 0 24px 48px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box;
}

.modal-header { text-align: center; margin-bottom: 24px; }
.modal-title { margin: 0 0 8px; font-size: 20px; color: var(--color-text, #111827); font-weight: 700; letter-spacing: -0.04em; }
.modal-subtitle { margin: 0; font-size: 14px; color: var(--color-text-muted, #6b7280); }

.modal-form { display: flex; flex-direction: column; gap: 16px; margin-bottom: 24px; }
.modal-form .input-group { display: flex; flex-direction: column; gap: 6px; text-align: left; }
.modal-form label { font-size: 13px; font-weight: 600; color: var(--color-text, #374151); }
.modal-form input {
  width: 100%; height: 46px; padding: 0 14px; border: 1px solid var(--color-border, #e5e7eb); border-radius: 10px;
  font-size: 14px; background: var(--color-page, #fafaf9); outline: none; transition: border-color 0.2s; box-sizing: border-box;
}
.modal-form input:focus { border-color: var(--color-brand, #f97316); background: var(--color-surface, #ffffff); }

.modal-error { margin-top: 0; margin-bottom: 16px; }

.modal-actions {
  display: flex; gap: 12px;
}
.modal-actions button {
  flex: 1; height: 48px; border-radius: 12px; font-size: 15px; font-weight: 600; cursor: pointer; transition: opacity 0.2s;
}

/* 마이페이지 상태 컨테이너 버튼 색감 참조 */
.btn-secondary {
  background: var(--color-brand-soft, #fff7ed);
  border: 1px solid var(--color-brand-border, #fed7aa);
  color: var(--color-brand, #ea580c);
}
.btn-secondary:hover { opacity: 0.8; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

@keyframes slideUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>