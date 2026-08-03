<script setup>
import { RouterLink } from 'vue-router'

const currentYear = new Date().getFullYear()
</script>

<template>
  <footer class="footer">
    <div class="app-container footer__inner">
      <div class="footer__brand">
        <RouterLink :to="{ name: 'home' }">WithTrip</RouterLink>
        <p>함께 계획하고 더 오래 기억하는 여행</p>
      </div>

      <nav class="footer__navigation" aria-label="푸터 메뉴">
        <RouterLink :to="{ name: 'home' }">홈</RouterLink>
        <RouterLink :to="{ name: 'plan-search' }">일정 탐색</RouterLink>
        <RouterLink :to="{ name: 'notice-list' }">공지사항</RouterLink>
      </nav>

      <div class="footer__meta">
        <small>© {{ currentYear }} WithTrip Team</small>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: auto;
  color: #cbd5e1;
  background: #0f172a;
}

.footer__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 32px;
  padding-block: 40px 28px;
}

.footer__brand > a {
  color: #f8fafc;
  font-size: 18px;
  font-weight: 800;
}

.footer__brand p {
  margin: 10px 0 0;
  color: #94a3b8;
}

.footer__navigation {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  font-size: 14px;
  font-weight: 650;
}

.footer__navigation a {
  transition: color 150ms ease;
}

.footer__navigation a:hover,
.footer__navigation a.router-link-active {
  color: #ffffff;
}

.footer__meta {
  grid-column: 1 / -1;
  padding-top: 20px;
  border-top: 1px solid rgb(148 163 184 / 20%);
  color: #94a3b8;
}

@media (max-width: 640px) {
  .footer__inner {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .footer__navigation {
    flex-wrap: wrap;
    gap: 12px 22px;
  }

  .footer__meta {
    grid-column: auto;
  }
}
</style>
