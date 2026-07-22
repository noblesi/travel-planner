<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppAdminHeader from '@/components/admin/AppAdminHeader.vue'
import AppAdminSidebar from '@/components/admin/AppAdminSidebar.vue'

const route = useRoute()

const pageTitle = computed(() => route.meta.title || '관리자')
</script>

<template>
  <div class="admin-layout">
    <AppAdminSidebar />

    <div class="admin-main">
      <AppAdminHeader :page-title="pageTitle" />

      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background: #f5f7fa;
}

.admin-main {
  flex: 1;
  min-width: 0;
  height: 100vh;
  overflow: hidden;
}

.admin-content {
  height: calc(100vh - 48px);
  min-width: 0;
  padding: 28px 30px 40px;
  overflow-x: hidden;
  overflow-y: auto;
}
</style>
