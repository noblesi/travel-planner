<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppAdminHeader from '@/components/admin/AppAdminHeader.vue'
import AppAdminSidebar from '@/components/admin/AppAdminSidebar.vue'

const route = useRoute()

// 자식 라우트의 meta.title이 바뀌면 공통 헤더 제목도 자동으로 갱신됩니다.
const pageTitle = computed(() => route.meta.title || '관리자')
</script>

<template>
  <div class="admin-layout">
    <AppAdminSidebar />

    <div class="admin-main">
      <AppAdminHeader :page-title="pageTitle" />

      <main class="admin-content">
        <!-- 현재 URL과 일치하는 관리자 자식 View가 이 위치에 렌더링됩니다. -->
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  --admin-page-bg: #fff5ed;
  --admin-surface: #ffffff;
  --admin-orange: #f3883b;
  --admin-orange-hover: #e8792f;
  --admin-orange-soft: #fff0e5;
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
</style>
