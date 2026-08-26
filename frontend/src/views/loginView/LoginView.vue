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
  <div class="login-container">
    <div class="login-box">
      
      <!-- 메인 타이틀 -->
      <div>
        <h1 class="main-title" style="margin-top: 30px;">
            나만의 플랜을 계획해보세요<br />
            <span>로그인</span>
        </h1>
      </div>
      <!-- 이메일 입력 및 계속하기 폼 -->
      <form class="login-form" :aria-busy="authStore.pending" @submit.prevent="handleContinue">
        <div class="input-container">
          <label class="input-label" for="login-email">이메일</label>
          <input 
            id="login-email"
            type="email" 
            v-model="email" 
            placeholder="이메일 주소를 입력하세요." 
            autocomplete="email"
            maxlength="255"
            required
          />
          <label class="input-label" for="login-password">비밀번호</label>
          <input 
            id="login-password"
            type="password" 
            v-model="password" 
            placeholder="비밀번호를 입력해주세요." 
            autocomplete="current-password"
            maxlength="72"
            required
          />
          <br/>
          <div class="passwordMatchedDiv" aria-live="polite">
            <span
              v-if="navigationError || authStore.errorMessage"
              class="error-message"
              role="alert"
            >
              {{ navigationError || authStore.errorMessage }}
            </span>
          </div>
        </div>


        <button
          type="submit"
          class="btn-submit"
          :disabled="authStore.pending || loginCompleted"
          :aria-busy="authStore.pending"
        >
          {{ loginCompleted ? '로그인 완료' : authStore.pending ? '로그인 중...' : '로그인' }}
        </button>
      </form>

      <!-- 하단 네비게이션 가이드 -->
      <div class="footer-links">
        <p class="recovery-links">
          <RouterLink :to="{ name: 'emailFind' }">이메일 찾기</RouterLink>
          <span aria-hidden="true">·</span>
          <RouterLink :to="{ name: 'passwordFind' }">비밀번호 재설정</RouterLink>
        </p>
        <p class="signup-prompt">신규 사용자이신가요? <RouterLink :to="{ name: 'join' }">가입하기</RouterLink></p>
      </div>
     
      
    </div>
  </div>
</template>

<style lang="scss" scoped>
.passwordMatchedDiv{
  min-height: 20px;
  text-align: center;
}

.error-message {
  color: #b42318;
  font-size: 13px;
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
.login-form {
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
    opacity: 0.7;
  }
}

// 하단 가이드 문구 스타일 영역
.footer-links {
  margin-top: 10px;

  .recovery-links {
    display: flex;
    justify-content: center;
    gap: 8px;
    margin: 0 0 8px;
    font-size: 13px;
  }

  a {
    color: #1a1a1a;
    text-decoration: underline;
    &:hover { color: #2383e2; }
  }

  .signup-prompt {
    font-size: 14px;
    color: #6b6b6b;
    margin-bottom: 10px;
    
    a {
      font-weight: 500;
    }
  }
}

</style>
