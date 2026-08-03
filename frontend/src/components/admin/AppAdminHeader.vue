<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineProps({
  // AdminLayout이 현재 라우트의 meta.title을 전달합니다.
  pageTitle: {
    type: String,
    required: true,
  },
  adminName: {
    // 추후 로그인한 관리자 정보로 교체할 기본 표시값입니다.
    type: String,
    default: '홍길동',
  },
})
defineEmits(['toggle-sidebar'])

const route = useRoute()
const breadcrumbs = computed(() => {
  const labels = route.matched.map((record) => record.meta?.title).filter(Boolean)
  return ['관리자', ...new Set(labels)]
})

</script>

<template>
  <header class="admin-header">
    <button class="menu-button" type="button" aria-label="관리자 메뉴 열기" @click="$emit('toggle-sidebar')">☰</button>
    <div class="breadcrumb">
      <template v-for="(label, index) in breadcrumbs" :key="label">
        <span v-if="index" class="separator">&gt;</span>
        <strong v-if="index === breadcrumbs.length - 1">{{ label }}</strong>
        <span v-else>{{ label }}</span>
      </template>
    </div>

    <div class="header-actions">
      <span class="admin-name">{{ adminName }} 관리자</span>
    </div>
  </header>
</template>

<style scoped>
.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 30px;
  border-bottom: 1px solid var(--admin-border);
  background: var(--admin-surface);
  color: var(--admin-muted);
  font-size: 12px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
}

.menu-button { display: none; border: 0; background: transparent; color: var(--admin-text); font-size: 20px; cursor: pointer; }
.header-actions { display: flex; align-items: center; gap: 15px; }

.separator {
  color: var(--admin-muted);
}

.breadcrumb strong {
  color: var(--admin-text);
}

.admin-name {
  color: var(--admin-text);
}

@media (max-width: 800px) {
  .admin-header {
    padding: 0 18px;
  }
  .menu-button { display: block; }
  .breadcrumb > span:first-child, .breadcrumb > .separator:first-of-type { display: none; }
  .admin-name { display: none; }
}
</style>
