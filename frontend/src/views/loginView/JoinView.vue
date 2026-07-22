<script setup>
import { ref } from 'vue'

const email = ref('')
const password = ref('')

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
            v-model="password" 
            placeholder="비밀번호를 입력해주세요." 
            required
          />
        </div>
        
        <button type="submit" class="btn-submit">가입하기</button>
      </form>
      <div class="divider">
        <span class="divider-text">또는 다음으로 계속하기</span>
      </div>


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
  margin-top: 40px;
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
  margin: 32px 0 20px 0;

  &::before, &::after {
    content: '';
    flex: 1;
    border-bottom: 1px solid #e8e8e8;
  }

  .divider-text {
    padding: 0 16px;
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

</style>