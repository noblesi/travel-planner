<script setup>
import { ref, computed } from 'vue'

const email = ref('')
const password = ref('')
const passwordCheck = ref('')



const isPasswordMatched = computed(() => {
  if (!passwordCheck.value) return true; 
  return password.value === passwordCheck.value;
});

const handleContinue = () => {
  if (!email.value) {
    alert('이메일 주소를 입력해주세요.')
    return
  }

  if (!password.value){
    alert('비밀번호를 입력해주세요.')
    return
  }

  if (password.value.length < 10){
    alert('비밀번호는 10자 이상 입력해주셔야 합니다.')
    return
  }
  
  if (!isPasswordMatched.value) {
    alert('비밀번호가 일치하지 않습니다.');
    return;
  }

  alert("로그인 유효성 검사")

}



</script>

<template>
  <div class="login-container">
    <div class="login-box">
      
      <!-- 메인 타이틀 -->
      <div style="margin-top: 30px;">
        <h1 class="main-title">
            나만의 플랜을 계획해보세요<br />
            <span>회원가입</span>
        </h1>
      </div>
      <!-- 이메일 입력 및 계속하기 폼 -->
      <form @submit.prevent="handleContinue" class="join-form">
        <div class="input-container">
          <label class="input-label">이메일</label>
          <input 
            type="email" 
            v-model="email" 
            placeholder="이메일 주소를 입력하세요." 
            required
          />
          <label class="input-label">비밀번호</label>
          <input 
            type="password"
            v-model="password" 
            placeholder="비밀번호를 입력해주세요." 
            required
          />
          <label class="input-label">비밀번호 확인</label>
          <input 
            type="password" 
            v-model="passwordCheck"
            placeholder="비밀번호를 입력해주세요." 
            required
          />
          <br/>
           <!-- 비밀번호 확인란에 입력이 시작되었을 때만 메시지 노출 -->
          <div v-if="passwordCheck" style="margin-top: 5px;" class="passwordMatchedDiv">
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
        
        <button type="submit" class="btn-submit">가입하기</button>
      </form>
      <div class="divider">
        <span class="divider-text">또는 다음으로 계속하기</span>
      </div>
      <div class="social-grid-triple">
        <div class="gsi-material-button">
            <div class="gsi-material-button-state"></div>
            <div class="gsi-material-button-content-wrapper">
                <div class="gsi-material-button-icon">
                <svg version="1.1" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" xmlns:xlink="http://www.w3.org/1999/xlink" style="display: block;">
                    <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"></path>
                    <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"></path>
                    <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"></path>
                    <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"></path>
                    <path fill="none" d="M0 0h48v48H0z"></path>
                </svg>
                </div>
                <span class="gsi-material-button-contents">Sign in with Google</span>
                <span style="display: none;">Sign in with Google</span>
            </div>
        </div>
        
      </div>

    </div>
  </div>
</template>

<style lang="scss" scoped>
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
  background-color: #f5c150;
  padding: 60px 20px;
  //font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, "Apple Color Emoji", Arial, sans-serif;
  box-sizing: border-box;
  backdrop-filter: blur(20px);
}

// 노션 특유의 슬림하고 중앙 집중된 박스 레이아웃
.login-box {
  width: 400px;
  height: 550px;
  background-color: #eed8a8;
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

.social-grid-double {
  display: grid;
  grid-template-columns: repeat(2, 1fr); // 2열 배치
  gap: 10px;
  padding: 0 24px; // 양옆 마진을 주어 중간 크기로 조절
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
    margin-bottom: 32px;
    
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
//구글 버튼 디자인

//google 버튼 디자인
.gsi-material-button {
  -moz-user-select: none;
  -webkit-user-select: none;
  -ms-user-select: none;
  -webkit-appearance: none;
  background-color: #f2f2f2;
  background-image: none;
  border: none;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  -webkit-box-sizing: border-box;
  box-sizing: border-box;
  color: #1f1f1f;
  cursor: pointer;
  font-family: 'Roboto', arial, sans-serif;
  font-size: 14px;
  height: 40px;
  letter-spacing: 0.25px;
  outline: none;
  overflow: hidden;
  padding: 0 12px;
  position: relative;
  text-align: center;
  -webkit-transition: background-color .218s, border-color .218s, box-shadow .218s;
  transition: background-color .218s, border-color .218s, box-shadow .218s;
  vertical-align: middle;
  white-space: nowrap;
  width: 360px;
  margin-left: 20px;
}

.gsi-material-button .gsi-material-button-icon {
  height: 20px;
  margin-right: 10px;
  min-width: 20px;
  width: 20px;
}

.gsi-material-button .gsi-material-button-content-wrapper {
  -webkit-align-items: center;
  align-items: center;
  display: flex;
  -webkit-flex-direction: row;
  flex-direction: row;
  -webkit-flex-wrap: nowrap;
  flex-wrap: nowrap;
  height: 100%;
  justify-content: space-between;
  position: relative;
  width: 100%;
}

.gsi-material-button .gsi-material-button-contents {
  -webkit-flex-grow: 1;
  flex-grow: 1;
  font-family: 'Roboto', arial, sans-serif;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: top;
}

.gsi-material-button .gsi-material-button-state {
  -webkit-transition: opacity .218s;
  transition: opacity .218s;
  bottom: 0;
  left: 0;
  opacity: 0;
  position: absolute;
  right: 0;
  top: 0;
}

.gsi-material-button:disabled {
  cursor: default;
  background-color: #ffffff61;
}

.gsi-material-button:disabled .gsi-material-button-state {
  background-color: #1f1f1f1f;
}

.gsi-material-button:disabled .gsi-material-button-contents {
  opacity: 38%;
}

.gsi-material-button:disabled .gsi-material-button-icon {
  opacity: 38%;
}

.gsi-material-button:not(:disabled):active .gsi-material-button-state, 
.gsi-material-button:not(:disabled):focus .gsi-material-button-state {
  background-color: #001d35;
  opacity: 12%;
}

.gsi-material-button:not(:disabled):hover {
  -webkit-box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .30), 0 1px 3px 1px rgba(60, 64, 67, .15);
  box-shadow: 0 1px 2px 0 rgba(60, 64, 67, .30), 0 1px 3px 1px rgba(60, 64, 67, .15);
}

.gsi-material-button:not(:disabled):hover .gsi-material-button-state {
  background-color: #001d35;
  opacity: 8%;
}

</style>