<template>
  <main class="admin-login-page">
    <!-- 왼쪽 소개 영역 -->
    <section class="brand-section">
      <div class="brand-content">
        <h1 class="brand-name">WithTrip</h1>

        <h2 class="brand-title">
          여행 콘텐츠와<br />
          서비스 운영을 한 곳에서
        </h2>

        <p class="brand-description">
          회원, 공개 여행 플랜, 추천 노출 규칙과 공지사항을
          효율적으로 관리합니다.
        </p>
      </div>
    </section>

    <!-- 오른쪽 로그인 영역 -->
    <section class="login-section">
      <div class="service-logo">
        <!-- 로고 이미지가 없어도 테스트 가능 -->
        <!-- <img src="/images/withtrip-logo.png" alt="WithTrip 로고" /> -->

        <p>친구들과 함께 여행을 계획하다</p>
      </div>

      <form class="login-card" @submit.prevent="handleLogin">
        <h2>관리자 로그인</h2>

        <p class="login-description">
          인가된 관리자 계정으로 로그인해 주세요.
        </p>

        <div class="form-field">
          <label for="adminId">관리자 아이디</label>

          <input
            id="adminId"
            v-model="loginForm.adminId"
            type="text"
            placeholder="아이디 입력"
            autocomplete="username"
          />
        </div>

        <div class="form-field">
          <label for="password">비밀번호</label>

          <input
            id="password"
            v-model="loginForm.password"
            type="password"
            placeholder="비밀번호 입력"
            autocomplete="current-password"
          />
        </div>

        <p v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </p>

        <button type="submit" class="login-button">
          로그인
        </button>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

interface AdminLoginForm {
  adminId: string
  password: string
}

const loginForm = reactive<AdminLoginForm>({
  adminId: '',
  password: '',
})

const errorMessage = ref('')

const handleLogin = () => {
  errorMessage.value = ''

  if (!loginForm.adminId.trim()) {
    errorMessage.value = '관리자 아이디를 입력해 주세요.'
    return
  }

  if (!loginForm.password.trim()) {
    errorMessage.value = '비밀번호를 입력해 주세요.'
    return
  }

  // 백엔드 연결 전 테스트
  console.log('관리자 로그인 요청', {
    adminId: loginForm.adminId,
    password: loginForm.password,
  })

  alert('로그인 테스트 성공')
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.admin-login-page {
  width: 100%;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #ffffff;
  color: #222222;
}

/* 왼쪽 소개 영역 */
.brand-section {
  display: flex;
  align-items: center;
  padding: 70px 54px;
  background: linear-gradient(
    180deg,
    #ee986e 0%,
    #efad6e 48%,
    #efe171 100%
  );
}

.brand-content {
  max-width: 520px;
}

.brand-name {
  margin: 0 0 18px;
  color: #ffffff;
  font-size: 34px;
  font-weight: 900;
  letter-spacing: -1px;
}

.brand-title {
  margin: 0;
  color: #ffffff;
  font-size: 41px;
  font-weight: 900;
  line-height: 1.35;
  letter-spacing: -2px;
}

.brand-description {
  margin: 20px 0 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.7;
}

/* 오른쪽 로그인 영역 */
.login-section {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120px 40px 50px;
  background: #ffffff;
}

.service-logo {
  position: absolute;
  top: 66px;
  left: 50%;
  transform: translateX(-50%);
  text-align: center;
}

.service-logo img {
  display: block;
  width: 86px;
  height: 32px;
  margin: 0 auto 7px;
  object-fit: contain;
}

.service-logo p {
  margin: 0;
  color: #8a8a8a;
  font-size: 10px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.login-card {
  width: 100%;
  max-width: 470px;
  padding: 48px 54px;
  border: 3px solid #c8ced8;
  border-radius: 26px;
  background: #ffffff;
}

.login-card h2 {
  margin: 0;
  color: #222222;
  font-size: 31px;
  font-weight: 900;
  letter-spacing: -1.5px;
}

.login-description {
  margin: 12px 0 22px;
  color: #8b9099;
  font-size: 14px;
}

.form-field {
  display: grid;
  gap: 8px;
  margin-bottom: 17px;
}

.form-field label {
  color: #363636;
  font-size: 14px;
  font-weight: 800;
}

.form-field input {
  width: 100%;
  height: 46px;
  padding: 0 16px;
  border: 2px solid #abb2bd;
  border-radius: 10px;
  outline: none;
  color: #252525;
  font-size: 14px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.form-field input::placeholder {
  color: #c5c7cb;
}

.form-field input:focus {
  border-color: #ff7012;
  box-shadow: 0 0 0 3px rgba(255, 112, 18, 0.13);
}

.error-message {
  margin: -2px 0 12px;
  color: #dc2626;
  font-size: 13px;
}

.login-button {
  width: 100%;
  height: 49px;
  margin-top: 13px;
  border: 0;
  border-radius: 5px;
  background: #ff7012;
  color: #ffffff;
  font-size: 14px;
  font-weight: 900;
  transition:
    background 0.2s,
    transform 0.1s;
  cursor: pointer;
}

.login-button:hover {
  background: #ed5f00;
}

.login-button:active {
  transform: translateY(1px);
}

@media (max-width: 900px) {
  .admin-login-page {
    grid-template-columns: 1fr;
  }

  .brand-section {
    min-height: 320px;
    padding: 50px 30px;
  }

  .brand-title {
    font-size: 34px;
  }

  .login-section {
    min-height: 620px;
    padding: 140px 24px 60px;
  }
}

@media (max-width: 520px) {
  .brand-section {
    min-height: 270px;
  }

  .brand-name {
    font-size: 27px;
  }

  .brand-title {
    font-size: 29px;
  }

  .login-card {
    padding: 38px 28px;
    border-width: 2px;
    border-radius: 20px;
  }

  .login-card h2 {
    font-size: 27px;
  }
}
</style>