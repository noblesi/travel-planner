<script setup>
defineProps({ page: { type: Number, required: true }, totalPages: { type: Number, required: true } })
defineEmits(['update:page'])
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="목록 페이지">
    <button type="button" :disabled="page <= 1" @click="$emit('update:page', page - 1)">이전</button>
    <button
      v-for="number in totalPages"
      :key="number"
      type="button"
      :class="{ active: number === page }"
      :aria-current="number === page ? 'page' : undefined"
      @click="$emit('update:page', number)"
    >{{ number }}</button>
    <button type="button" :disabled="page >= totalPages" @click="$emit('update:page', page + 1)">다음</button>
  </nav>
</template>

<style scoped>
.pagination { display: flex; justify-content: center; gap: 6px; margin-top: 22px; }
.pagination button { min-width: 34px; height: 34px; padding: 0 10px; border: 1px solid var(--admin-border); border-radius: 6px; background: var(--admin-surface); color: var(--admin-text); cursor: pointer; }
.pagination button:hover:not(:disabled), .pagination button.active { border-color: var(--admin-orange); background: var(--admin-orange); color: #fff; }
.pagination button:focus-visible { outline: 3px solid rgb(243 136 59 / 25%); outline-offset: 2px; }
.pagination button:disabled { cursor: not-allowed; opacity: .45; }
</style>
