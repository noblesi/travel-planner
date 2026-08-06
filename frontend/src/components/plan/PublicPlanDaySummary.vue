<script setup>
defineProps({
  placeCount: { type: Number, required: true },
  dayCount: { type: Number, required: true },
  dayNumber: { type: Number, required: true },
})
</script>

<template>
  <dl class="day-summary">
    <div class="summary-row">
      <dt>오늘 방문 장소</dt>
      <dd>{{ placeCount }}곳</dd>
    </div>
    <div class="summary-row">
      <dt>여행 진행</dt>
      <dd>{{ dayCount }}일 중 {{ dayNumber }}일차</dd>
    </div>
  </dl>
</template>

<style scoped>
.day-summary {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
  gap: 10px;
  margin: 28px 0 0;
  padding-top: 14px;
  border-top: 1px solid #d8d8d8;
}
.summary-row { display: flex; align-items: center; justify-content: space-between; }
dt { color: #888; font-size: 12px; }
dd { margin: 0; color: #1a1a1a; font-size: 12px; font-weight: 600; }
</style>
