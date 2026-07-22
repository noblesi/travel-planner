<script setup>
defineProps({
  pageTitle: {
    type: String,
    required: true,
  },
  adminName: {
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
  border-bottom: 1px solid #e7e9ed;
  background: #ffffff;
  color: #555b64;
  font-size: 12px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
}

.separator {
  color: #a4a8af;
}

.breadcrumb strong {
  color: #30343a;
}

.admin-name {
  color: #555b64;
}

@media (max-width: 650px) {
  .admin-header {
    padding: 0 18px;
  }
}
</style>