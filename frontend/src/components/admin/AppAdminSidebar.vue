<script setup>
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

// 공통 사이드바 메뉴입니다. path는 adminRouter.js의 경로와 일치해야 합니다.
const menuItems = [
  {
    name: '대시보드',
    path: '/admin/dashboard',
  },
  {
    name: '회원 관리',
    path: '/admin/members',
  },
  {
    name: '여행 플랜 관리',
    path: '/admin/trips',
  },
  {
    name: '공지사항 관리',
    path: '/admin/notices',
  },
  {
    name: '관광데이터 관리',
    path: '/admin/tour-data',
  },
]

const logout = () => {
  // 백엔드 로그아웃 API 및 Pinia 인증 상태 초기화와 연결합니다.
  localStorage.removeItem('adminAccessToken')
  router.push('/admin/login')
}
</script>

<template>
  <aside class="admin-sidebar">
    <header class="sidebar-header">
      <strong class="logo">WithTrip</strong>
      <span class="console-name">ADMIN CONSOLE</span>
    </header>

    <section class="admin-profile">
      <div class="profile-image" aria-hidden="true" />

      <div class="profile-information">
        <strong>홍길동</strong>
        <span>최고 관리자</span>
      </div>
    </section>

    <nav class="sidebar-navigation" aria-label="관리자 메뉴">
      <p class="navigation-title">MANAGEMENT</p>

      <RouterLink
        v-for="item in menuItems"
        :key="item.path"
        :to="item.path"
        class="navigation-item"
      >
        {{ item.name }}
      </RouterLink>
    </nav>

    <div class="sidebar-footer">
      <button class="logout-button" type="button" @click="logout">
        로그아웃
      </button>
    </div>
  </aside>
</template>

<style scoped>
.admin-sidebar {
  display: flex;
  flex-direction: column;
  width: 240px;
  min-width: 240px;
  height: 100%;
  overflow-y: auto;
  border-right: 1px solid var(--admin-border);
  background: var(--admin-surface);
  color: var(--admin-text);
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 20px 18px;
  border-bottom: 1px solid var(--admin-border);
}

.logo {
  font-size: 19px;
  font-weight: 900;
  letter-spacing: -0.5px;
}

.console-name {
  font-size: 11px;
  font-weight: 800;
}

.admin-profile {
  display: flex;
  gap: 13px;
  align-items: center;
  margin: 14px;
  padding: 14px;
  border-radius: 6px;
  background: var(--admin-orange-soft);
}

.profile-image {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #ffddca;
}

.profile-information {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.profile-information strong {
  font-size: 13px;
}

.profile-information span {
  color: var(--admin-muted);
  font-size: 11px;
}

.sidebar-navigation {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 0 14px;
}

.navigation-title {
  margin: 10px 0 5px;
  font-size: 12px;
  font-weight: 900;
}

.navigation-item {
  display: flex;
  align-items: center;
  min-height: 45px;
  padding: 0 17px;
  border-radius: 5px;
  color: var(--admin-text);
  font-size: 14px;
  font-weight: 750;
  text-decoration: none;
  transition:
    background-color 0.2s,
    transform 0.1s;
}

.navigation-item:hover {
  background: var(--admin-orange-soft);
}

.navigation-item:active {
  transform: translateY(1px);
}

.navigation-item.router-link-active {
  background: var(--admin-orange);
  color: #ffffff;
}

.sidebar-footer {
  margin-top: auto;
  padding: 18px 14px;
  border-top: 1px solid var(--admin-border);
}

.logout-button {
  width: 100%;
  height: 42px;
  border: 1px solid var(--admin-orange);
  border-radius: 5px;
  background: #ffffff;
  color: var(--admin-orange);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.logout-button:hover {
  background: var(--admin-orange-soft);
}

@media (max-width: 800px) {
  .admin-sidebar {
    width: 200px;
    min-width: 200px;
  }
}
</style>
