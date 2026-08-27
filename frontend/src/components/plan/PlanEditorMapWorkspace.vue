<script setup>
import { computed, nextTick, ref } from 'vue'

import KakaoMap from '@/components/map/KakaoMap.vue'
import PlaceSearchPanel from '@/components/plan/PlaceSearchPanel.vue'

const props = defineProps({
  plan: { type: Object, required: true },
  selectedDay: { type: Object, default: null },
  scheduleItems: { type: Array, required: true },
  settingsBusy: { type: Boolean, required: true },
  selectedScheduleItemId: { type: [String, Number], default: null },
})

const emit = defineEmits(['add', 'select-schedule'])
const mapRef = ref(null)
const searchResults = ref([])
const selectedSearchPlace = ref(null)
const isSearchDrawerOpen = ref(true)

const selectedSearchPlaceId = computed(() =>
  selectedSearchPlace.value
    ? `${selectedSearchPlace.value.placeProvider}:${selectedSearchPlace.value.externalPlaceId}`
    : null,
)
const selectedMapPlaceId = computed(() =>
  selectedSearchPlaceId.value
    ? `search:${selectedSearchPlaceId.value}`
    : props.selectedScheduleItemId != null
      ? `schedule:${props.selectedScheduleItemId}`
      : null,
)
const mapPlaces = computed(() => [
  ...props.scheduleItems.map((item) => ({
    ...item,
    mapPlaceId: `schedule:${item.scheduleItemId}`,
    markerSource: 'SCHEDULE',
  })),
  ...searchResults.value.map((place) => ({
    ...place,
    mapPlaceId: `search:${place.placeProvider}:${place.externalPlaceId}`,
    markerSource: 'SEARCH',
  })),
])
const selectedSearchPlaceTimeSlots = computed(() => {
  if (!selectedSearchPlace.value) return []
  return props.scheduleItems
    .filter(
      (item) =>
        item.placeProvider === selectedSearchPlace.value.placeProvider &&
        String(item.externalPlaceId) === String(selectedSearchPlace.value.externalPlaceId),
    )
    .map((item) => item.timeSlot)
})

function updateSearchResults(places) {
  searchResults.value = places
  if (
    selectedSearchPlace.value &&
    !places.some(
      (place) =>
        place.placeProvider === selectedSearchPlace.value.placeProvider &&
        place.externalPlaceId === selectedSearchPlace.value.externalPlaceId,
    )
  ) {
    selectedSearchPlace.value = null
  }
}

function selectMapPlace(place) {
  if (place.markerSource === 'SEARCH') selectSearchPlace(place)
  if (place.markerSource === 'SCHEDULE') {
    selectedSearchPlace.value = null
    emit('select-schedule', place)
  }
}

function selectSearchPlace(place) {
  selectedSearchPlace.value = place
  if (place) emit('select-schedule', null)
}

function clearMapSelection() {
  selectedSearchPlace.value = null
  emit('select-schedule', null)
}

function relayoutMap() {
  mapRef.value?.relayout?.()
}

function toggleSearchDrawer() {
  isSearchDrawerOpen.value = !isSearchDrawerOpen.value
  nextTick(relayoutMap)
}
</script>

<template>
  <section
    class="map-panel"
    :class="{ 'map-panel--drawer-closed': !isSearchDrawerOpen }"
    aria-label="여행 장소 지도 영역"
    @transitionend.self="relayoutMap"
  >
    <div class="map-stage">
      <KakaoMap
        ref="mapRef"
        class="editor-map"
        :places="mapPlaces"
        :selected-place-id="selectedMapPlaceId"
        :selected-place-detail="selectedSearchPlace"
        :selected-place-existing-time-slots="selectedSearchPlaceTimeSlots"
        :selected-place-add-disabled="settingsBusy || !selectedDay"
        :empty-message="`${plan.regionName}의 장소를 검색하면 지도에 표시됩니다.`"
        @select="selectMapPlace"
        @deselect="clearMapSelection"
        @add="$emit('add', $event)"
      />
    </div>
    <button
      class="place-search-drawer__toggle"
      type="button"
      aria-controls="place-search-drawer"
      :aria-expanded="isSearchDrawerOpen"
      :aria-label="isSearchDrawerOpen ? '장소 검색 서랍 닫기' : '장소 검색 서랍 열기'"
      @click="toggleSearchDrawer"
    >
      <svg viewBox="0 0 20 20" aria-hidden="true">
        <path
          :d="isSearchDrawerOpen ? 'm7.5 4 6 6-6 6' : 'm12.5 4-6 6 6 6'"
          fill="none"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.8"
        />
      </svg>
    </button>
    <aside
      id="place-search-drawer"
      class="place-search-drawer"
      aria-label="장소 검색 서랍"
      :aria-hidden="!isSearchDrawerOpen"
      :inert="!isSearchDrawerOpen"
    >
      <PlaceSearchPanel
        :region-code="plan.regionCode"
        :region-name="plan.regionName"
        :selected-place-id="selectedSearchPlaceId"
        :schedule-items="scheduleItems"
        @results-change="updateSearchResults"
        @select="selectSearchPlace"
      />
    </aside>
  </section>
</template>

<style scoped>
.map-panel {
  --place-search-drawer-width: clamp(320px, 28vw, 380px);

  position: relative;
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(0, 1fr) var(--place-search-drawer-width);
  background: #dce5e1;
  transition: grid-template-columns 220ms ease;
}
.map-panel--drawer-closed {
  grid-template-columns: minmax(0, 1fr) 0;
}
.map-stage {
  position: relative;
  min-width: 0;
  min-height: 0;
}
.editor-map {
  width: 100%;
  height: 100%;
  min-height: 0;
}
.place-search-drawer {
  position: relative;
  z-index: 5;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border-left: 1px solid #dbe2ea;
  background: #fff;
  box-shadow: -10px 0 28px rgb(15 23 42 / 8%);
  opacity: 1;
  transition:
    opacity 140ms ease,
    visibility 0s;
}
.map-panel--drawer-closed .place-search-drawer {
  visibility: hidden;
  border-left-color: transparent;
  opacity: 0;
  pointer-events: none;
  transition:
    opacity 100ms ease,
    visibility 0s 220ms;
}
.place-search-drawer__toggle {
  position: absolute;
  z-index: 7;
  top: 24px;
  right: var(--place-search-drawer-width);
  display: grid;
  width: 38px;
  height: 46px;
  padding: 0;
  place-items: center;
  border: 1px solid #d6dee8;
  border-radius: 12px;
  background: #fff;
  color: #475569;
  box-shadow: 0 7px 20px rgb(15 23 42 / 14%);
  cursor: pointer;
  transform: translateX(50%);
  transition:
    right 220ms ease,
    transform 220ms ease,
    color 140ms ease,
    border-color 140ms ease;
}
.place-search-drawer__toggle:hover {
  border-color: var(--color-brand-border);
  color: var(--color-brand);
}
.place-search-drawer__toggle:focus-visible {
  outline: 3px solid rgb(255 90 78 / 24%);
  outline-offset: 2px;
}
.place-search-drawer__toggle svg {
  width: 20px;
  height: 20px;
}
.map-panel--drawer-closed .place-search-drawer__toggle {
  right: 16px;
  transform: none;
}
@media (prefers-reduced-motion: reduce) {
  .map-panel,
  .place-search-drawer,
  .place-search-drawer__toggle {
    transition: none;
  }
}
</style>
