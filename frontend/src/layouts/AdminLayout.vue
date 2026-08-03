<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AppAdminHeader from '@/components/admin/AppAdminHeader.vue'
import AppAdminSidebar from '@/components/admin/AppAdminSidebar.vue'

const route = useRoute()
const isSidebarOpen = ref(false)

// 자식 라우트의 meta.title이 바뀌면 공통 헤더 제목도 자동으로 갱신됩니다.
const pageTitle = computed(() => route.meta.title || '관리자')
watch(() => route.fullPath, () => { isSidebarOpen.value = false })
</script>

<template>
  <div class="admin-layout">
    <AppAdminSidebar :open="isSidebarOpen" @close="isSidebarOpen = false" />
    <button v-if="isSidebarOpen" class="sidebar-backdrop" type="button" aria-label="관리자 메뉴 닫기" @click="isSidebarOpen = false" />

    <div class="admin-main">
      <AppAdminHeader :page-title="pageTitle" @toggle-sidebar="isSidebarOpen = !isSidebarOpen" />

      <main class="admin-content">
        <!-- 현재 URL과 일치하는 관리자 자식 View가 이 위치에 렌더링됩니다. -->
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  --admin-page-bg: var(--color-page);
  --admin-surface: #ffffff;
  --admin-orange: var(--color-brand);
  --admin-orange-hover: var(--color-brand-hover);
  --admin-orange-soft: var(--color-brand-soft);
  --admin-text: #252a31;
  --admin-muted: #858c96;
  --admin-border: #eee3db;

  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: var(--admin-page-bg);
  color: var(--admin-text);
}

.admin-main {
  flex: 1;
  min-width: 0;
  height: 100vh;
  overflow: hidden;
  background: var(--admin-page-bg);
}

.admin-content {
  height: calc(100vh - 48px);
  min-width: 0;
  padding: 28px 30px 40px;
  overflow-x: hidden;
  overflow-y: auto;
}

.sidebar-backdrop { display: none; }

@media (max-width: 800px) {
  .sidebar-backdrop { position: fixed; z-index: 29; inset: 0; display: block; border: 0; background: rgb(37 42 49 / 42%); }
  .admin-content { padding: 22px 18px 32px; }
}
</style>
