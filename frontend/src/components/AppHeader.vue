<script setup>
import { RouterLink, useRouter } from 'vue-router'

import headerLogoUrl from '@/assets/branding/travel-planner-logo-symbol.png'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const moveLoginPage = () => {
  router.push('/loginView')
}

const moveJoinPage = () => {
  router.push('/joinView')
}

const handleLogout = async () => {
  try {
    await authStore.logout()
    await router.push('/')
  } catch {
    // Store에 사용자에게 표시할 오류를 유지하고 현재 화면에 머뭅니다.
  }
}
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <RouterLink class="brand" to="/" aria-label="WithTrip 홈">
        <img class="brand__mark" :src="headerLogoUrl" alt="" width="40" height="40" />
        <span>WithTrip</span>
      </RouterLink>

      <nav class="navigation" aria-label="주요 메뉴">
        <RouterLink to="/">홈</RouterLink>
        <a href="/plans">일정 탐색</a>
        <a href="/announcements">공지사항</a>
      </nav>

      <div class="header__actions">
        <div v-if="!authStore.isAuthenticated">
          <button class="text-button" type="button" id="loginBtn" v-on:click="moveLoginPage">로그인</button>
          <button class="primary-button" type="button" v-on:click="moveJoinPage">회원가입</button>
        </div>
        <div v-else class="authenticated-actions">
          <span class="member-name">{{ authStore.currentUser.displayName }}</span>
          <button
            class="text-button"
            type="button"
            :disabled="authStore.pending"
            v-on:click="handleLogout"
          >
            로그아웃
          </button>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 11;
  border-bottom: 1px solid #e5e7eb;
  background: rgb(255 255 255 / 92%);
  backdrop-filter: blur(12px);
}

.header__inner {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  width: min(1180px, calc(100% - 40px));
  min-height: 72px;
  margin: 0 auto;
}

.brand {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  width: fit-content;
  color: #0f766e;
  font-size: 21px;
  font-weight: 800;
}

.brand__mark {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  object-fit: contain;
}

.navigation {
  display: flex;
  gap: 36px;
  font-size: 15px;
  font-weight: 650;
}

.navigation a:hover {
  color: #0f766e;
}

.header__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.authenticated-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-name {
  color: #374151;
  font-size: 14px;
  font-weight: 650;
}

.text-button,
.primary-button {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 10px;
  cursor: pointer;
}

.text-button {
  border: 0;
  background: transparent;
}

.primary-button {
  color: white;
  border: 1px solid #0f766e;
  background: #0f766e;
}

@media (max-width: 760px) {
  .header__inner {
    grid-template-columns: 1fr auto;
  }

  .navigation {
    display: none;
  }

  .text-button {
    display: none;
  }
}
</style>
