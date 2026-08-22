<script setup>
import { computed, ref } from 'vue'
import { isNavigationFailure, useRouter } from 'vue-router'

import { getMemberEmailCheck } from '@/api/users'
import GoogleAuthPlaceholder from '@/components/auth/GoogleAuthPlaceholder.vue'
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
  <div class="login-container">
    <div class="login-box">
      <div style="height: 30px; text-align: left;">
        <button
          type="button"
          class="back-button"
          aria-label="이전 화면으로 이동"
          @click="goBack"
        >
          &lt;
        </button>
      </div>
      <!-- 메인 타이틀 -->
      <div>
        <h1 class="main-title">
            나만의 플랜을 계획해보세요<br />
            <span>회원가입</span>
        </h1>
      </div>
      <!-- 이메일 입력 및 계속하기 폼 -->
      <form @submit.prevent="handleContinue" class="join-form">
        <div class="input-container">
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
          <label class="input-label" for="join-password-confirmation">비밀번호 확인</label>
          <input 
            id="join-password-confirmation"
            type="password" 
            v-model="passwordConfirmation"
            placeholder="비밀번호를 입력해주세요." 
            autocomplete="new-password"
            minlength="10"
            maxlength="72"
            required
          />
          <br/>
           <!-- 비밀번호 확인란에 입력이 시작되었을 때만 메시지 노출 -->
          <div
            v-if="passwordConfirmation"
            class="passwordMatchedDiv"
            style="margin-top: 5px;"
            aria-live="polite"
          >
            <!-- 일치하지 않을 때 -->
            <span v-show="!isPasswordMatched" style="color: #ff4d4d; font-size: 14px;">
              ❌ 비밀번호가 맞지 않습니다.
            </span>
            <!-- 일치할 때 -->
            <span v-show="isPasswordMatched" style="color: #222573; font-size: 14px;">
              ✅ 비밀번호가 일치합니다.
            </span>
          </div>
        </div>

        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
        <button
          type="submit"
          class="btn-submit"
          :disabled="isCheckingEmail"
          :aria-busy="isCheckingEmail"
        >
          {{ isCheckingEmail ? '확인 중...' : '다음으로' }}
        </button>
      </form>
      <div class="divider">
        <span class="divider-text">또는 다음으로 계속하기</span>
      </div>
      <div class="social-grid-triple">
        <GoogleAuthPlaceholder />
      </div>

    </div>
  </div>
</template>

<style lang="scss" scoped>
.back-button{
  margin-left: 20px;
  width: 32px;
  height: 32px;
  border: 0;
  background: transparent;
  color: #1a1a1a;
  font-size: 30px;
  line-height: 1;
  text-align: center;
}
.back-button:hover{
  cursor: pointer;
  color: #2383e2;
}

.back-button:focus-visible,
.btn-submit:focus-visible {
  outline: 3px solid rgb(35 131 226 / 35%);
  outline-offset: 3px;
}

.passwordMatchedDiv{
  height: 14px;
  text-align: center;

}
// 전체 배경 배치
.login-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  background-color: #c2410c;
  padding: 60px 20px;
  //font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, "Apple Color Emoji", Arial, sans-serif;
  box-sizing: border-box;
  backdrop-filter: blur(20px);
}

// 노션 특유의 슬림하고 중앙 집중된 박스 레이아웃
.login-box {
  width: 400px;
  height: 550px;
  background-color: #ec8f6b;
  text-align: center;
  box-shadow: 0 10px 30px 5px rgba(0, 0, 0, 0.1), 
              0 4px 12px 2px rgba(0, 0, 0, 0.1);
  border-radius: 30px;
}


// 타이틀
.main-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.3;
  margin-top: 0;
  margin-bottom: 32px;

  span {
    color: #6b6b6b;
    font-size: 20px;
    font-weight: 500;
  }
}

// 이메일 폼 세팅
.join-form {
  text-align: left;

  .input-container {
    margin-bottom: 10px;

    .input-label {
      display: block;
      font-size: 12px;
      color: #6b6b6b;
      margin-bottom: 6px;
      margin-left: 20px;
    }

    input {
      width: 90%;
      height: 44px;
      padding: 0 14px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      font-size: 14px;
      background-color: #fafafa;
      box-sizing: border-box;
      outline: none;
      transition: border-color 0.15s;
      text-align: center;
      margin-left: 20px;
      &::placeholder {
        color: #cccccc;
      }

      &:focus {
        border-color: #2383e2;
        background-color: #ffffff;
      }
    }

  }
}

// 메인 '계속' 파란색 버튼
.btn-submit {
  margin-top: 20px;
  margin-left: 20px;
  width: 90%;
  height: 44px;
  background-color: #2383e2;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s;

  &:hover {
    background-color: #1a6cb9;
  }

  &:disabled {
    cursor: wait;
    opacity: 0.65;
  }
}

.form-error {
  min-height: 20px;
  margin: 0 20px 8px;
  color: #991b1b;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

// 또는 다음으로 계속하기 구분선
.divider {
  display: flex;
  align-items: center;
  margin: 15px 0 20px 0;

  &::before, &::after {
    content: '';
    flex: 1;
    border-bottom: 1px solid #e8e8e8;
  }

  .divider-text {
    padding: 0 10px;
    font-size: 12px;
    color: #9b9b9b;
  }
}

// 소셜 카드 그리드 시스템 (핵심 뼈대 구조)
.social-grid-triple {
  display: grid;
  grid-template-columns: repeat(1, 1fr); // 3열 배치
  gap: 10px;
  margin-bottom: 10px;
}

</style>
