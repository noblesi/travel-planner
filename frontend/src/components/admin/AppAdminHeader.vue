<script setup>
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
</script>

<template>
  <header class="admin-header">
    <div class="breadcrumb">
      <span>관리자</span>
      <span class="separator">&gt;</span>
      <strong>{{ pageTitle }}</strong>
    </div>

    <span class="admin-name">{{ adminName }} 관리자</span>
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

.separator {
  color: var(--admin-muted);
}

.breadcrumb strong {
  color: var(--admin-text);
}

.admin-name {
  color: var(--admin-text);
}

@media (max-width: 650px) {
  .admin-header {
    padding: 0 18px;
  }
}
</style>
