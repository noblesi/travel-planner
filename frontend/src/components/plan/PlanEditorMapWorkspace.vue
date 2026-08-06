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
  if (place.markerSource === 'SEARCH') selectedSearchPlace.value = place
  if (place.markerSource === 'SCHEDULE') {
    selectedSearchPlace.value = null
    emit('select-schedule', place)
  }
}
</script>

<template>
  <section class="map-panel" aria-label="여행 장소 지도 영역">
    <KakaoMap
      class="editor-map"
      :places="mapPlaces"
      :selected-place-id="selectedMapPlaceId"
      :empty-message="`${plan.regionName}의 장소를 검색하면 지도에 표시됩니다.`"
      @select="selectMapPlace"
    />
    <div class="map-overlay">
      <PlaceSearchPanel
        :region-code="plan.regionCode"
        :region-name="plan.regionName"
        :selected-place-id="selectedSearchPlaceId"
        :schedule-disabled="settingsBusy || !selectedDay"
        :schedule-items="scheduleItems"
        @results-change="updateSearchResults"
        @select="selectedSearchPlace = $event"
        @add="$emit('add', $event)"
      />
    </div>
  </section>
</template>

<style scoped>
.map-panel { position: relative; min-width: 0; min-height: 0; background: #dce5e1; }
.editor-map { width: 100%; height: 100%; min-height: 520px; }
.map-overlay { position: absolute; z-index: 5; top: 20px; right: 20px; width: min(380px, calc(100% - 40px)); max-height: calc(100% - 40px); overflow-y: auto; }
</style>
