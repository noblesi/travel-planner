<script setup>
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

const menuItems = [
  {
    name: '대시보드',
    path: '/admin/dashboard',
  },
  {
    name: '회원 관리',
    path: '/admin/users',
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
  // 실제 로그인 기능이 생기면 토큰을 여기서 삭제합니다.
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
  background: #2b201c;
  color: #ffffff;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 20px 18px;
  border-bottom: 1px solid rgb(255 255 255 / 25%);
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
  background: #513224;
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
  color: #d0c3bc;
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
  color: #ffffff;
  font-size: 14px;
  font-weight: 750;
  text-decoration: none;
  transition:
    background-color 0.2s,
    transform 0.1s;
}

.navigation-item:hover {
  background: rgb(255 255 255 / 9%);
}

.navigation-item:active {
  transform: translateY(1px);
}

.navigation-item.router-link-active {
  background: #ff6815;
}

.sidebar-footer {
  margin-top: auto;
  padding: 18px 14px;
  border-top: 1px solid rgb(255 255 255 / 25%);
}

.logout-button {
  width: 100%;
  height: 42px;
  border: 1px solid #ebe1dc;
  border-radius: 5px;
  background: #ffffff;
  color: #ec6b43;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.logout-button:hover {
  background: #fff5ef;
}

@media (max-width: 800px) {
  .admin-sidebar {
    width: 200px;
    min-width: 200px;
  }
}
</style>
