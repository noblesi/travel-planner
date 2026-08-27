<script setup>
import { computed, ref } from 'vue'

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
const searchResults = ref([])
const selectedSearchPlace = ref(null)

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
</script>

<template>
  <section class="map-panel" aria-label="여행 장소 지도 영역">
    <div class="map-stage">
      <KakaoMap
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
    <aside class="place-search-drawer" aria-label="장소 검색 서랍">
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
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-columns: minmax(0, 1fr) clamp(320px, 28vw, 380px);
  background: #dce5e1;
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
}
</style>
