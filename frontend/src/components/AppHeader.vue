<script setup>
import { RouterLink } from 'vue-router'
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <RouterLink class="brand" to="/" aria-label="WithTrip 홈">
        <span class="brand__mark">W</span>
        <span>WithTrip</span>
      </RouterLink>

      <nav class="navigation" aria-label="주요 메뉴">
        <RouterLink to="/">홈</RouterLink>
        <a href="/plans">일정 탐색</a>
        <a href="/announcements">공지사항</a>
      </nav>

      <div class="header__actions">
        <button class="text-button" type="button">로그인</button>
        <button class="primary-button" type="button">회원가입</button>
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
  display: grid;
  width: 34px;
  height: 34px;
  color: white;
  border-radius: 11px;
  background: #0f766e;
  place-items: center;
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
