<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { getEmailFind } from '@/api/find'

const router = useRouter()
const name = ref('')
const birth = ref('')
const phone = ref('')

// 모달 상태 및 찾은 이메일 저장 변수
const isModalOpen = ref(false)
const foundEmail = ref('')

const userFindInfo = ref({
  memberName: '',
  birthDate: '',
  phoneNumber: ''
})

const findEmail = () => {
  if(name.value != '' && name.value != null){
    userFindInfo.value.memberName = name.value
  } else {
    alert("이름을 입력해주세요")
    return
  }

  if(birth.value != '' && birth.value != null){
    userFindInfo.value.birthDate = birth.value
  } else {
    alert("생년월일을 입력해주세요")
    return
  }
  
  if(phone.value != '' && phone.value != null){
    userFindInfo.value.phoneNumber = phone.value
  } else {
    alert("핸드폰 번호를 입력해주세요")
    return
  }

  getEmailFind(userFindInfo.value).then((response) => {
    if (response) {
      foundEmail.value = response
      isModalOpen.value = true // 모달 열기
    } else {
      alert("일치하는 이메일 정보를 찾을 수 없습니다.")
    }
  }).catch((error) => {
    alert("이메일을 찾는데 오류가 발생했습니다: " + error)
  })

}

const goToLogin = () => {
  isModalOpen.value = false
  router.push({ name: 'login' })
}

</script>

<template>
  <div class="login-container">
    <div class="login-box">
      
      <!-- 메인 타이틀 -->
      <div>
        <h1 class="main-title" style="margin-top: 30px;">
            나만의 플랜을 계획해보세요<br />
            <span>내 이메일 찿기</span>
        </h1>
      </div>
      <!-- 이메일 입력 및 계속하기 폼 -->
      <form class="login-form">
        <div class="input-container">
          <label class="input-label">이름</label>
          <input 
            type="text" 
            v-model="name" 
            placeholder="이름을 입력해주세요" 
            required
          />
          <label class="input-label">생년월일</label>
          <input 
            type="date" 
            v-model="birth" 
            placeholder="생년월일을 입력해주세요. 예)20010528" 
            required
          />
          <label class="input-label">전화번호</label>
          <input 
            type="text" 
            v-model="phone" 
            placeholder="전화번호를 입력해주세요. 예)010-1234-5689" 
            required
          />
        </div>
        
        <button type="button" @click="findEmail" class="btn-submit" >이메일 찾기</button>
      </form>
      <div class="footer-links">
        <p class="signup-prompt">신규 사용자이신가요? <RouterLink :to="{ name: 'join' }">가입하기</RouterLink></p>
      </div>

    </div>
    <!-- 이메일 찾기 완료 모달 -->
    <Teleport to="body">
      <div v-if="isModalOpen" class="modal-backdrop">
        <div class="modal-box">
          <h2 class="modal-title">이메일 찾기 완료</h2>
          <p class="modal-content">
            입력하신 정보와 일치하는 이메일입니다.<br />
            <strong>{{ foundEmail }}</strong>
          </p>
          <button type="button" class="btn-modal-confirm" @click="goToLogin">
            확인
          </button>
        </div>
      </div>
    </Teleport>
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

// 버튼
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
// 하단 가이드 문구
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
}

// ====================================================
// 이메일 찾기 모달 스타일
// ====================================================
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.modal-box {
  width: 320px;
  padding: 28px 24px;
  background-color: #ffffff;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
  animation: modalPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  .modal-title {
    font-size: 18px;
    font-weight: 700;
    color: #111827;
    margin: 0 0 12px 0;
  }

  .modal-content {
    font-size: 14px;
    color: #4b5563;
    line-height: 1.5;
    margin: 0 0 24px 0;

    strong {
      display: block;
      margin-top: 10px;
      font-size: 17px;
      color: #2383e2;
      font-weight: 700;
      word-break: break-all;
    }
  }

  .btn-modal-confirm {
    width: 100%;
    height: 44px;
    background-color: #2383e2;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.15s ease;

    &:hover {
      background-color: #1a6cb9;
    }
  }
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>