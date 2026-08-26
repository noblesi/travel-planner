<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router' 
import { getPasswordFindReword } from '@/api/find'
import { getPasswordReword } from '@/api/find' 

const inputEmail = ref('')
const birth = ref('')
const phone = ref('')
const router = useRouter()

const userFindPassword = ref({
  email: '',
  birthDate: '',
  phoneNumber: ''
})

const isResetModalOpen = ref(false)
const newPassword = ref('')
const confirmPassword = ref('')

const findPassword = () => {

  if(inputEmail.value != '' && inputEmail.value != null){
    userFindPassword.value.email = inputEmail.value
  } else {
    alert("이메일을 입력해주세요")
    return
  }

  if(birth.value != '' && birth.value != null){
    userFindPassword.value.birthDate = birth.value
  } else {
    alert("생년월일을 입력해주세요")
    return
  }

  if(phone.value != '' && phone.value != null){
    userFindPassword.value.phoneNumber = phone.value
  } else {
    alert("핸드폰 번호를 입력해주세요")
    return
  }

  // API 호출 후 성공 시 모달 오픈
  getPasswordFindReword(userFindPassword.value).then(()=>{
    isResetModalOpen.value = true // 🟢 유저 확인 성공 시 모달창 열기
  }).catch(()=>{
    alert("일치하는 회원 정보를 찾을 수 없습니다.") // 오류 메시지 다듬기
  })
}

// 🟢 모달창 닫기 함수
const closeResetModal = () => {
  isResetModalOpen.value = false
  newPassword.value = ''
  confirmPassword.value = ''
}

// 🟢 새로운 비밀번호 제출 함수
const submitNewPassword = () => {

  const rewordPass = ref({
    email: inputEmail.value,
    newPassword: newPassword.value
  })

  if (!newPassword.value) {
    alert("새 비밀번호를 입력해주세요.")
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    alert("비밀번호가 일치하지 않습니다. 다시 확인해주세요.")
    return
  }

  // 💡 TODO: 백엔드로 진짜 비밀번호를 변경하는 API를 호출하세요!
  // 예: postResetPassword({ email: inputEmail.value, newPassword: newPassword.value })
  //     .then(() => { ... })
  getPasswordReword(rewordPass.value).then(()=>{
    alert("비밀번호가 성공적으로 변경되었습니다.")
    closeResetModal()
    router.push({ name: 'login' }) // 성공 후 로그인 화면으로 이동
  }).catch(()=>{
    alert("비밀번호 변경에 실패했습니다.")
  })
  
}

</script>

<template>
  <div class="login-container">
    <div class="login-box">
      
      <!-- 메인 타이틀 -->
      <div>
        <h1 class="main-title" style="margin-top: 30px;">
            나만의 플랜을 계획해보세요<br />
            <span>비밀번호 찾기</span>
        </h1>
      </div>
      
      <!-- 이메일 입력 및 계속하기 폼 -->
      <form class="login-form">
        <div class="input-container">
          <label class="input-label">이메일</label>
          <input 
            type="email" 
            v-model="inputEmail" 
            placeholder="이메일 주소를 입력하세요." 
            required
          />
          <label class="input-label">생년월일</label>
          <input 
            type="date" 
            v-model="birth"
            required
          />
          <label class="input-label">전화번호</label>
          <input 
            type="text" 
            v-model="phone" 
            placeholder="예) 010-1234-5689" 
            required
          />
        </div>
        
        <button type="button" @click="findPassword" class="btn-submit">비밀번호 찾기</button>
      </form>
      <div class="footer-links">
        <p class="signup-prompt">신규 사용자이신가요? <RouterLink :to="{ name: 'join' }">가입하기</RouterLink></p>
      </div>

    </div>

    <!-- 🟢 비밀번호 재설정 모달 창 -->
    <div v-if="isResetModalOpen" class="modal-overlay" @click="closeResetModal">
      <div class="modal-content" @click.stop>
        <h3 class="modal-title">비밀번호 재설정</h3>
        <p class="modal-subtitle">새롭게 사용할 비밀번호를 입력해 주세요.</p>
        
        <div class="modal-input-group">
          <label>새 비밀번호</label>
          <input type="password" v-model="newPassword" placeholder="새 비밀번호 입력" />
        </div>
        
        <div class="modal-input-group">
          <label>새 비밀번호 확인</label>
          <input type="password" v-model="confirmPassword" placeholder="새 비밀번호 다시 입력" @keyup.enter="submitNewPassword" />
        </div>

        <div class="modal-actions">
          <button type="button" class="btn-cancel" @click="closeResetModal">취소</button>
          <button type="button" class="btn-submit-modal" @click="submitNewPassword">변경하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
/* =========== 기존 로그인 컨테이너 스타일 유지 =========== */
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

.login-box {
  width: 400px;
  height: 550px;
  background-color: #ec8f6b;
  text-align: center;
  box-shadow: 0 10px 30px 5px rgba(0, 0, 0, 0.1), 
              0 4px 12px 2px rgba(0, 0, 0, 0.1);
  border-radius: 30px;
}

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

/* =========== 🟢 새롭게 추가된 모달창 스타일 =========== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background-color: #ffffff;
  padding: 35px 30px;
  border-radius: 16px;
  width: 380px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
  color: #333;
  animation: fadeIn 0.2s ease-out;
}

.modal-title {
  margin: 0;
  font-size: 1.4rem;
  color: #2b2b2b;
  text-align: center;
  font-weight: 700;
}

.modal-subtitle {
  text-align: center;
  font-size: 0.9rem;
  color: #666;
  margin-bottom: 25px;
}

.modal-input-group {
  margin-bottom: 15px;
  text-align: left;
}

.modal-input-group label {
  display: block;
  font-size: 0.85rem;
  margin-bottom: 6px;
  color: #555;
  font-weight: 600;
}

.modal-input-group input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  outline: none;
  background-color: #f9f9f9;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.modal-input-group input:focus {
  border-color: #2383e2;
  background-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(35, 131, 226, 0.15);
}

.modal-actions {
  display: flex;
  gap: 10px;
  margin-top: 25px;
}

.modal-actions button {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-cancel {
  background-color: #f1f1f1;
  color: #555;
}
.btn-cancel:hover {
  background-color: #e4e4e4;
}

.btn-submit-modal {
  background-color: #2383e2;
  color: #fff;
}
.btn-submit-modal:hover {
  background-color: #1a6cb9;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
