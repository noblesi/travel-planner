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
  <div class="login-container">
    <div class="login-box">
      
      <!-- 메인 타이틀 -->
      <div>
        <h1 class="main-title" style="margin-top: 30px;">
            거의다 왔어요 조금만 더 하면되요.<br />
            <span>프로필 등록하기</span>
        </h1>
      </div>
      <!-- 이메일 입력 및 계속하기 폼 -->
      <form @submit.prevent="handleContinue" class="login-form">
        <div class="input-container">
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
        <div class="input-container">
          <label class="input-label" for="join-birth">생년월일</label>
          <input 
            id="join-birth"
            type="text" 
            v-model="birth" 
            placeholder="생년월일을 입력해주세요. 예) 20001031" 
            autocomplete="bday"
            inputmode="numeric"
            maxlength="8"
            pattern="[0-9]{8}"
            required
          />
        </div>
        <div class="input-container">
          <label class="input-label" for="join-phone">전화번호</label>
          <input 
            id="join-phone"
            type="text" 
            v-model="phone" 
            placeholder="전화번호를 입력해주세요 예) 010-1234-5678" 
            autocomplete="tel"
            inputmode="tel"
            maxlength="20"
            required
          />
        </div>
        <fieldset class="input-container gender-options">
          <legend class="input-label">성별</legend>
          <label><input type="radio" name="gender" v-model="gender" value="M" /> 남성</label>
          <label><input type="radio" name="gender" v-model="gender" value="F" /> 여성</label>
          <label><input type="radio" name="gender" v-model="gender" value="N" /> 선택안함</label>
        </fieldset>
        <div class="input-container privacy-consent">
          <input id="join-privacy" type="checkbox" class="input-checkbox" v-model="privacy" />
          <label for="join-privacy">개인정보 저장에 동의합니다.</label>
        </div>
        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
        <button
          type="submit"
          class="btn-submit"
          :disabled="isSubmitting || isRegistrationCompleted"
        >
          {{ isRegistrationCompleted ? '가입 완료' : isSubmitting ? '가입 처리 중...' : '가입하기' }}
        </button>
      </form>
      
    </div>
  </div>
</template>

<style lang="scss" scoped>
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

.input-checkbox {
  margin-right: 10px;
  transform: scale(1.2);
  cursor: pointer;
}

.gender-options {
  display: flex;
  margin-inline: 20px;
  padding: 0;
  border: 0;
  gap: 24px;
}

.gender-options .input-label {
  width: 100%;
}

.gender-options label,
.privacy-consent label {
  cursor: pointer;
  font-size: 14px;
}

.privacy-consent {
  display: flex;
  align-items: center;
  margin-inline: 20px;
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

    input[type='text'],
    input[type='button'] {
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
    opacity: 0.65;
  }
}

.btn-submit:focus-visible,
input:focus-visible {
  outline: 3px solid rgb(35 131 226 / 35%);
  outline-offset: 3px;
}

.form-error {
  min-height: 20px;
  margin: 0 20px 8px;
  color: #991b1b;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
}

// 공통 사각형 박스 버튼 스타일
.card-btn {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 68px;
  border: 1px solid #e3e3e3;
  border-radius: 6px;
  background-color: #ffffff;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s, border-color 0.15s;

  &:hover {
    background-color: #f7f7f7;
    border-color: #cccccc;
  }

  img {
    width: 20px;
    height: 20px;
    margin-bottom: 6px;
  }

  .emoji-icon {
    font-size: 18px;
    margin-bottom: 4px;
  }

  span {
    font-size: 12px;
    color: #1a1a1a;
    font-weight: 500;
  }
}

// 하단 가이드 문구 스타일 영역
.footer-links {
  margin-top: 40px;

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

  .terms-text {
    font-size: 11px;
    color: #9b9b9b;
    line-height: 1.6;
    
    a {
      color: #9b9b9b;
    }
  }
}

</style>
