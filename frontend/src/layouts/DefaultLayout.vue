<script setup>
import AppFooter from '@/components/AppFooter.vue'
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="layout">
    <a class="skip-link" href="#main-content">본문 바로가기</a>
    <AppHeader />
    <main id="main-content" class="layout__main" tabindex="-1">
      <slot />
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
}

.layout__main {
  flex: 1;
  min-width: 0;
  scroll-margin-top: var(--layout-header-height);
}

.skip-link {
  position: fixed;
  z-index: 100;
  top: 12px;
  left: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--color-brand);
  color: var(--color-brand-on);
  font-weight: 700;
  transform: translateY(-160%);
  transition: transform 150ms ease;
}

.skip-link:focus {
  transform: translateY(0);
}
</style>
