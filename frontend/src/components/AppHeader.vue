<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'

import headerLogoUrl from '@/assets/branding/travel-planner-logo-symbol.webp'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isMenuOpen = ref(false)

const closeMenu = () => {
  isMenuOpen.value = false
}

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

watch(() => route.fullPath, closeMenu)

const handlePlanSearchClick = () => {
  closeMenu()
  if (route.name === 'plan-search') {
    window.dispatchEvent(new Event('plan-search:reset'))
  }
}

const handleLogout = async () => {
  try {
    await authStore.logout()
    closeMenu()
    await router.push({ name: 'home' })
  } catch {
    // Store에 사용자에게 표시할 오류를 유지하고 현재 화면에 머뭅니다.
  }
}
</script>

<template>
  <header class="header" @keydown.esc="closeMenu">
    <div class="app-container header__inner">
      <RouterLink class="brand" :to="{ name: 'home' }" aria-label="WithTrip 홈" @click="closeMenu">
        <img class="brand__mark" :src="headerLogoUrl" alt="" width="40" height="40" />
        <span>WithTrip</span>
      </RouterLink>

      <nav
        id="primary-navigation"
        :class="['navigation', { 'navigation--open': isMenuOpen }]"
        aria-label="주요 메뉴"
      >
        <RouterLink class="navigation__link" :to="{ name: 'home' }" @click="closeMenu">홈</RouterLink>
        <RouterLink class="navigation__link" :to="{ name: 'plan-search' }" @click="handlePlanSearchClick">
          일정 탐색
        </RouterLink>
        <RouterLink
          v-if="authStore.isAuthenticated"
          class="navigation__link"
          :to="{ name: 'my-plans' }"
          @click="closeMenu"
        >
          내 플랜
        </RouterLink>
        <RouterLink
          class="navigation__link"
          :to="{ name: 'notice-list' }"
          @click="closeMenu"
        >
          공지사항
        </RouterLink>

        <div class="navigation__account">
          <template v-if="!authStore.isAuthenticated">
            <RouterLink class="navigation__account-link" :to="{ name: 'login' }" @click="closeMenu">
              로그인
            </RouterLink>
            <RouterLink
              class="navigation__account-link navigation__account-link--primary"
              :to="{ name: 'join' }"
              @click="closeMenu"
            >
              회원가입
            </RouterLink>
          </template>
          <template v-else>
            <span class="member-name">{{ authStore.currentUser.displayName }}</span>
            <button type="button" :disabled="authStore.pending" @click="handleLogout">로그아웃</button>
          </template>
        </div>
      </nav>

      <div class="header__actions">
        <div v-if="!authStore.isAuthenticated" class="desktop-actions">
          <RouterLink id="loginBtn" class="text-button" :to="{ name: 'login' }">로그인</RouterLink>
          <RouterLink class="primary-button" :to="{ name: 'join' }">회원가입</RouterLink>
        </div>
        <div v-else class="desktop-actions authenticated-actions">
          <span class="member-name">{{ authStore.currentUser.displayName }}</span>
          <button
            class="text-button"
            type="button"
            :disabled="authStore.pending"
            @click="handleLogout"
          >
            로그아웃
          </button>
        </div>

        <button
          :class="['menu-button', { 'menu-button--open': isMenuOpen }]"
          type="button"
          aria-controls="primary-navigation"
          :aria-expanded="isMenuOpen"
          :aria-label="isMenuOpen ? '주요 메뉴 닫기' : '주요 메뉴 열기'"
          @click="toggleMenu"
        >
          <span aria-hidden="true" />
          <span aria-hidden="true" />
          <span aria-hidden="true" />
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 11;
  border-bottom: 1px solid rgb(226 232 240 / 90%);
  background: rgb(255 255 255 / 92%);
  backdrop-filter: blur(12px);
}

.header__inner {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  min-height: var(--layout-header-height);
}

.brand {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  width: fit-content;
  color: var(--color-brand);
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

.navigation__link {
  position: relative;
  display: inline-flex;
  min-height: var(--layout-header-height);
  align-items: center;
  transition: color 150ms ease;
}

.navigation__link::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--color-brand-accent);
  content: '';
  opacity: 0;
  transform: scaleX(0.6);
  transition: opacity 150ms ease, transform 150ms ease;
}

.navigation__link:hover,
.navigation__link.router-link-active {
  color: var(--color-brand);
}

.navigation__link.router-link-active::after {
  opacity: 1;
  transform: scaleX(1);
}

.navigation__account {
  display: none;
}

.header__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.desktop-actions,
.authenticated-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.authenticated-actions {
  justify-content: flex-end;
}

.member-name {
  color: #374151;
  font-size: 14px;
  font-weight: 650;
}

.text-button,
.primary-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-radius: 10px;
  cursor: pointer;
}

.text-button {
  border: 0;
  background: transparent;
}

.primary-button {
  color: var(--color-brand-on);
  border: 1px solid var(--color-brand);
  background: var(--color-brand);
  transition: background-color 150ms ease, border-color 150ms ease;
}

.primary-button:hover {
  border-color: var(--color-brand-hover);
  background: var(--color-brand-hover);
}

.menu-button {
  display: none;
  width: 42px;
  height: 42px;
  padding: 9px;
  border: 1px solid var(--color-brand-border);
  border-radius: 10px;
  background: var(--color-surface);
  cursor: pointer;
}

.menu-button span {
  display: block;
  width: 100%;
  height: 2px;
  border-radius: 2px;
  background: var(--color-text);
  transition: transform 150ms ease, opacity 150ms ease;
}

.menu-button--open span:nth-child(1) {
  transform: translateY(6px) rotate(45deg);
}

.menu-button--open span:nth-child(2) {
  opacity: 0;
}

.menu-button--open span:nth-child(3) {
  transform: translateY(-6px) rotate(-45deg);
}

@media (max-width: 900px) {
  .navigation {
    gap: 24px;
  }
}

@media (max-width: 760px) {
  .header__inner {
    grid-template-columns: 1fr auto;
    min-height: 64px;
  }

  .navigation {
    position: absolute;
    top: 100%;
    right: 0;
    left: 0;
    flex-direction: column;
    gap: 4px;
    display: none;
    padding: 12px var(--layout-gutter) 18px;
    border: 1px solid var(--color-brand-border);
    border-top: 0;
    border-radius: 0 0 16px 16px;
    background: rgb(255 255 255 / 98%);
    box-shadow: 0 18px 40px rgb(15 23 42 / 12%);
    backdrop-filter: blur(12px);
  }

  .navigation--open {
    display: flex;
  }

  .navigation__link {
    min-height: 44px;
    justify-content: flex-start;
    padding: 0 12px;
    border-radius: 9px;
  }

  .navigation__link::after {
    display: none;
  }

  .navigation__link.router-link-active {
    background: var(--color-brand-soft);
  }

  .navigation__account {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    align-items: center;
    margin-top: 8px;
    padding-top: 14px;
    border-top: 1px solid #e2e8f0;
  }

  .navigation__account-link,
  .navigation__account button {
    display: inline-flex;
    min-height: 42px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--color-brand-border);
    border-radius: 10px;
    background: var(--color-surface);
    color: var(--color-brand);
    font-weight: 700;
  }

  .navigation__account-link--primary {
    border-color: var(--color-brand);
    background: var(--color-brand);
    color: var(--color-brand-on);
  }

  .navigation__account .member-name {
    padding-left: 12px;
  }

  .desktop-actions {
    display: none;
  }

  .menu-button {
    display: grid;
    align-content: center;
    gap: 4px;
  }
}
</style>
