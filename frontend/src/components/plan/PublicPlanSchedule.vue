<script setup>
import { computed } from 'vue'

const props = defineProps({
  days: { type: Array, required: true },
  currentDay: { type: Object, required: true },
  selectedDay: { type: Number, required: true },
})

defineEmits(['select-day'])

const groupedPlaces = computed(() =>
  ['오전', '오후']
    .map((timeSlot) => ({
      timeSlot,
      places: props.currentDay.places.filter((place) => place.timeSlot === timeSlot),
    }))
    .filter((section) => section.places.length > 0),
)
</script>

<template>
  <div class="schedule-layout">
    <nav class="day-sidebar" aria-label="공개 여행 일차 선택">
      <button
        v-for="day in days"
        :key="day.dayNumber"
        class="day-item"
        :class="{ active: selectedDay === day.dayNumber }"
        :aria-pressed="selectedDay === day.dayNumber"
        @click="$emit('select-day', day.dayNumber)"
      >
        <span class="day-item-bar" />
        <span class="day-item-body">
          <strong class="day-num">DAY {{ day.dayNumber }}</strong>
          <small class="day-date">{{ day.dateLabel }}</small>
        </span>
      </button>
    </nav>

    <section class="day-content" aria-label="선택 일차 일정">
      <header class="day-content-head">
        <span class="day-content-num">DAY {{ currentDay.dayNumber }}</span>
        <span class="day-content-date">{{ currentDay.fullDateLabel }}</span>
      </header>
      <div class="day-content-body">
        <section
          v-for="section in groupedPlaces"
          :key="section.timeSlot"
          class="time-section"
          :class="section.timeSlot === '오전' ? 'time-section--morning' : 'time-section--afternoon'"
        >
          <header class="time-section-head">
            <span class="time-section-label">{{ section.timeSlot }}</span>
            <span class="time-section-count">{{ section.places.length }}곳</span>
          </header>
          <div class="place-list">
            <article v-for="place in section.places" :key="place.id" class="place-row">
              <span class="place-bar" />
              <div class="place-card">
                <strong class="place-name">{{ place.name }}</strong>
                <p class="place-desc">{{ place.address }}</p>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.schedule-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  min-width: 0;
  min-height: 0;
  gap: 20px;
}
.day-sidebar,
.day-content {
  min-height: 0;
  border-radius: 14px;
  background: #f0f0f0;
  box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
}
.day-sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 14px 10px 14px 14px;
  overflow-y: auto;
}
.day-sidebar::-webkit-scrollbar,
.day-content-body::-webkit-scrollbar { width: 6px; }
.day-sidebar::-webkit-scrollbar-thumb,
.day-content-body::-webkit-scrollbar-thumb { border-radius: 3px; background: #ddd; }
.day-item {
  display: flex;
  align-items: stretch;
  flex-shrink: 0;
  gap: 12px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
  cursor: pointer;
}
.day-item:not(.active):hover { background: #f7f7f7; }
.day-item.active { background: #fce3ce; }
.day-item-bar { width: 6px; flex-shrink: 0; background: #aaa; }
.day-item.active .day-item-bar { background: var(--color-brand-accent); }
.day-item-body {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  min-width: 0;
  flex: 1;
  gap: 8px;
  padding: 20px 16px 20px 0;
}
.day-num { color: #888; font-size: 15px; }
.day-date { color: #888; font-size: 12px; }
.day-item.active .day-num,
.day-item.active .day-date { color: var(--color-brand); }
.day-content { display: flex; flex-direction: column; padding: 24px; }
.day-content-head {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid #d8d8d8;
}
.day-content-body { min-height: 0; padding-top: 20px; padding-right: 4px; overflow-y: auto; }
.day-content-num { color: #1a1a1a; font-size: 20px; font-weight: 700; }
.day-content-date { color: #888; font-size: 14px; }
.time-section { margin-bottom: 24px; }
.time-section:last-child { margin-bottom: 0; }
.time-section--morning { --slot-color: #ef4444; --slot-color-dark: var(--color-danger); }
.time-section--afternoon { --slot-color: #3b82f6; --slot-color-dark: var(--color-secondary); }
.time-section-head { display: flex; align-items: baseline; gap: 8px; margin-bottom: 12px; }
.time-section-label { color: var(--slot-color-dark); font-size: 15px; font-weight: 700; }
.time-section-count { color: #888; font-size: 12px; }
.place-list { display: flex; flex-direction: column; gap: 10px; }
.place-row { display: flex; gap: 10px; }
.place-bar { width: 3px; flex-shrink: 0; background: var(--slot-color); }
.place-card {
  min-width: 0;
  flex: 1;
  margin-right: 16px;
  padding: 16px 18px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(0 0 0 / 8%);
}
.place-name { display: block; margin-bottom: 5px; color: #1a1a1a; font-size: 16px; }
.place-desc { margin: 0; color: #666; font-size: 14px; line-height: 1.5; }
@media (max-width: 760px) {
  .schedule-layout { display: flex; flex-direction: column; gap: 16px; }
  .day-sidebar {
    min-height: auto;
    flex-direction: row;
    padding: 0 0 8px;
    overflow-x: auto;
    overflow-y: hidden;
    scroll-snap-type: x proximity;
  }
  .day-sidebar::-webkit-scrollbar { width: auto; height: 6px; }
  .day-item { flex: 0 0 104px; padding: 12px 14px; scroll-snap-align: start; }
  .day-item-bar { display: none; }
  .day-item-body { display: grid; padding: 0; }
  .day-content { min-height: auto; padding: 18px 16px; overflow: visible; }
  .day-content-head { position: static; margin: 0 0 16px; padding: 0; }
  .day-content-num { font-size: 18px; }
  .place-card { padding: 14px; }
  .place-name,
  .place-desc { overflow-wrap: anywhere; }
}
</style>
