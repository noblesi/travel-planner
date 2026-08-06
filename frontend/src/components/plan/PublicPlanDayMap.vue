<script setup>
import KakaoMap from '@/components/map/KakaoMap.vue'
import PublicPlanDaySummary from '@/components/plan/PublicPlanDaySummary.vue'

defineProps({
  currentDay: { type: Object, required: true },
  dayCount: { type: Number, required: true },
})
</script>

<template>
  <aside class="day-map" aria-label="선택 일차 지도와 요약">
    <header class="map-head">
      <span>DAY {{ currentDay.dayNumber }} 일정 위치</span>
      <span class="map-count">장소 {{ currentDay.places.length }}곳</span>
    </header>
    <div class="map-canvas">
      <KakaoMap :places="currentDay.places" />
    </div>
    <PublicPlanDaySummary
      :place-count="currentDay.places.length"
      :day-count="dayCount"
      :day-number="currentDay.dayNumber"
    />
  </aside>
</template>

<style scoped>
.day-map {
  display: flex;
  min-height: 0;
  flex-direction: column;
  padding: 20px;
  overflow-y: auto;
  border-radius: 14px;
  background: #f0f0f0;
  box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
}
.day-map::-webkit-scrollbar { width: 6px; }
.day-map::-webkit-scrollbar-thumb { border-radius: 3px; background: #ddd; }
.map-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  margin-bottom: 28px;
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 600;
}
.map-count { color: #888; font-size: 12px; font-weight: 400; }
.map-canvas {
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 10px;
  aspect-ratio: 200 / 220;
  box-shadow: 0 6px 18px rgb(0 0 0 / 15%);
}
@media (max-width: 760px) {
  .day-map { min-height: auto; padding: 16px; overflow: visible; }
  .map-canvas { aspect-ratio: 16 / 10; }
}
</style>
