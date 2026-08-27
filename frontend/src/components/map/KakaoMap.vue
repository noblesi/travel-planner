<script setup>
import { computed, h, nextTick, onBeforeUnmount, onMounted, ref, render, watch } from 'vue'

import PlaceDetailCard from '@/components/plan/PlaceDetailCard.vue'
import { loadKakaoMapSdk } from '@/utils/kakaoMapSdk'

const props = defineProps({
  places: {
    type: Array,
    default: () => [],
  },
  selectedPlaceId: {
    type: [String, Number],
    default: null,
  },
  selectedPlaceDetail: {
    type: Object,
    default: null,
  },
  selectedPlaceExistingTimeSlots: {
    type: Array,
    default: () => [],
  },
  selectedPlaceAddDisabled: {
    type: Boolean,
    default: false,
  },
  emptyMessage: {
    type: String,
    default: '표시할 장소가 없습니다.',
  },
  defaultCenter: {
    type: Object,
    default: () => ({ latitude: 37.5665, longitude: 126.978 }),
  },
})

const emit = defineEmits(['select', 'deselect', 'add'])

const mapContainer = ref(null)
const status = ref('loading')
const errorMessage = ref('')
let mapInstance = null
let markers = []
let infoWindow = null
let infoWindowRoot = null
let routeLine = null

function finiteCoordinate(value) {
  const coordinate = Number(value)
  return Number.isFinite(coordinate) ? coordinate : null
}

function placeId(place, index) {
  return String(
    place.mapPlaceId ??
      place.scheduleItemId ??
      place.externalPlaceId ??
      place.id ??
      `place-${index}`,
  )
}

const normalizedPlaces = computed(() =>
  props.places
    .map((place, index) => ({
      id: placeId(place, index),
      name: place.placeName ?? place.name ?? '이름 없는 장소',
      latitude: finiteCoordinate(place.latitude ?? place.lat),
      longitude: finiteCoordinate(place.longitude ?? place.lng),
      markerSource: place.markerSource ?? 'DEFAULT',
      positionNo: Number(place.positionNo) || null,
      timeSlot: place.timeSlot ?? null,
      original: place,
    }))
    .filter((place) => place.latitude !== null && place.longitude !== null),
)

function closeInfoWindow() {
  infoWindow?.close()
  if (infoWindowRoot) {
    render(null, infoWindowRoot)
    infoWindowRoot = null
  }
}

function clearMarkers() {
  markers.forEach(({ marker }) => marker.setMap(null))
  markers = []
  routeLine?.setMap?.(null)
  routeLine = null
}

function markerImage(kakao, place) {
  if (typeof kakao.maps.MarkerImage !== 'function' || typeof kakao.maps.Size !== 'function') {
    return undefined
  }
  const scheduled = place.markerSource === 'SCHEDULE'
  const color = scheduled ? (place.timeSlot === 'MORNING' ? '#ff5a4e' : '#475569') : '#2563eb'
  const label = scheduled && place.positionNo ? String(place.positionNo) : '•'
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="42" viewBox="0 0 34 42"><path fill="${color}" stroke="white" stroke-width="2" d="M17 1C8.2 1 1 8.2 1 17c0 12 16 24 16 24s16-12 16-24C33 8.2 25.8 1 17 1z"/><circle cx="17" cy="17" r="10" fill="white"/><text x="17" y="21" text-anchor="middle" font-family="Arial" font-size="11" font-weight="700" fill="${color}">${label}</text></svg>`
  return new kakao.maps.MarkerImage(
    `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
    new kakao.maps.Size(34, 42),
  )
}

function renderScheduleRoute(kakao) {
  if (typeof kakao.maps.Polyline !== 'function') return
  const routePlaces = markers
    .filter(({ place }) => place.markerSource === 'SCHEDULE')
    .sort((left, right) => {
      const slotOrder = { MORNING: 0, AFTERNOON: 1 }
      return (
        (slotOrder[left.place.timeSlot] ?? 2) - (slotOrder[right.place.timeSlot] ?? 2) ||
        (left.place.positionNo ?? 0) - (right.place.positionNo ?? 0)
      )
    })
  if (routePlaces.length < 2) return
  routeLine = new kakao.maps.Polyline({
    path: routePlaces.map(({ position }) => position),
    strokeWeight: 4,
    strokeColor: '#ff5a4e',
    strokeOpacity: 0.72,
    strokeStyle: 'shortdash',
  })
  routeLine.setMap(mapInstance)
}

function simpleInfoWindowContent(place) {
  const content = document.createElement('div')
  content.className = 'kakao-map__info-window'
  content.textContent = place.name
  return content
}

function isSelectedDetailPlace(place) {
  return (
    props.selectedPlaceDetail &&
    place.markerSource === 'SEARCH' &&
    place.id === String(props.selectedPlaceId)
  )
}

function infoWindowContent(place) {
  if (!isSelectedDetailPlace(place)) return simpleInfoWindowContent(place)

  const content = document.createElement('div')
  content.className = 'kakao-map__detail-window'
  infoWindowRoot = content
  render(
    h(PlaceDetailCard, {
      place: props.selectedPlaceDetail,
      addDisabled: props.selectedPlaceAddDisabled,
      existingTimeSlots: props.selectedPlaceExistingTimeSlots,
      closable: true,
      onAdd: (timeSlot) => emit('add', { place: props.selectedPlaceDetail, timeSlot }),
      onClose: () => {
        closeInfoWindow()
        emit('deselect', place.original)
      },
    }),
    content,
  )
  return content
}

function showSelectedPlace() {
  if (!mapInstance || !infoWindow) return

  const selectedId = props.selectedPlaceId == null ? null : String(props.selectedPlaceId)
  const selectedMarker = markers.find(({ place }) => place.id === selectedId)

  markers.forEach(({ marker }) => marker.setZIndex?.(0))
  if (!selectedMarker) {
    closeInfoWindow()
    return
  }

  selectedMarker.marker.setZIndex?.(10)
  openPlaceInfo(selectedMarker)
}

function openPlaceInfo({ marker, place, position }) {
  closeInfoWindow()
  infoWindow.setContent(infoWindowContent(place))
  infoWindow.open(mapInstance, marker)
  mapInstance.panTo?.(position)
}

function fitMapToPlaces(kakao) {
  if (markers.length === 0) {
    const center = new kakao.maps.LatLng(
      finiteCoordinate(props.defaultCenter.latitude) ?? 37.5665,
      finiteCoordinate(props.defaultCenter.longitude) ?? 126.978,
    )
    mapInstance.setCenter(center)
    return
  }

  if (markers.length === 1) {
    mapInstance.setCenter(markers[0].position)
    return
  }

  const bounds = new kakao.maps.LatLngBounds()
  markers.forEach(({ position }) => bounds.extend(position))
  mapInstance.setBounds(bounds)
}

function renderMarkers() {
  if (!mapInstance || !window.kakao?.maps) return

  const { kakao } = window
  clearMarkers()
  closeInfoWindow()

  normalizedPlaces.value.forEach((place) => {
    const position = new kakao.maps.LatLng(place.latitude, place.longitude)
    const options = {
      map: mapInstance,
      position,
      title: place.name,
    }
    const image = markerImage(kakao, place)
    if (image) options.image = image
    const marker = new kakao.maps.Marker(options)

    const markerEntry = { marker, place, position }
    kakao.maps.event.addListener(marker, 'click', () => {
      markers.forEach(({ marker: currentMarker }) => currentMarker.setZIndex?.(0))
      marker.setZIndex?.(10)
      openPlaceInfo(markerEntry)
      emit('select', place.original)
    })
    markers.push(markerEntry)
  })

  renderScheduleRoute(kakao)
  fitMapToPlaces(kakao)
  showSelectedPlace()
}

async function initializeMap() {
  clearMarkers()
  closeInfoWindow()
  mapInstance = null
  infoWindow = null
  status.value = 'loading'
  errorMessage.value = ''

  try {
    await loadKakaoMapSdk()
    await nextTick()
    if (!mapContainer.value) return

    const { kakao } = window
    const center = new kakao.maps.LatLng(
      finiteCoordinate(props.defaultCenter.latitude) ?? 37.5665,
      finiteCoordinate(props.defaultCenter.longitude) ?? 126.978,
    )
    mapInstance = new kakao.maps.Map(mapContainer.value, { center, level: 7 })
    infoWindow = new kakao.maps.InfoWindow({ removable: false })
    renderMarkers()
    kakao.maps.event.addListener(mapInstance, 'click', () => {
      closeInfoWindow()
      emit('deselect')
    })
    status.value = 'ready'
  } catch (error) {
    status.value = 'error'
    errorMessage.value = error instanceof Error ? error.message : '지도를 불러오지 못했습니다.'
  }
}

watch(normalizedPlaces, renderMarkers)
watch(() => props.selectedPlaceId, showSelectedPlace)
watch(
  () => [
    props.selectedPlaceDetail,
    props.selectedPlaceAddDisabled,
    props.selectedPlaceExistingTimeSlots,
  ],
  showSelectedPlace,
  { deep: true },
)
watch(
  () => props.defaultCenter,
  () => {
    if (normalizedPlaces.value.length === 0) renderMarkers()
  },
  { deep: true },
)

onMounted(initializeMap)
onBeforeUnmount(() => {
  clearMarkers()
  closeInfoWindow()
  mapInstance = null
  infoWindow = null
})
</script>

<template>
  <div class="kakao-map">
    <div ref="mapContainer" class="kakao-map__canvas" aria-label="카카오 지도" />

    <div v-if="status === 'loading'" class="kakao-map__state" role="status">
      <span class="kakao-map__spinner" aria-hidden="true" />
      <strong>지도를 불러오고 있어요.</strong>
    </div>

    <div
      v-else-if="status === 'error'"
      class="kakao-map__state kakao-map__state--error"
      role="alert"
    >
      <strong>지도를 표시할 수 없습니다.</strong>
      <p>{{ errorMessage }}</p>
      <button type="button" @click="initializeMap">다시 시도</button>
    </div>

    <div v-else-if="normalizedPlaces.length === 0" class="kakao-map__empty" role="status">
      <span aria-hidden="true">⌖</span>
      <p>{{ emptyMessage }}</p>
    </div>
  </div>
</template>

<style scoped>
.kakao-map {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 260px;
  overflow: hidden;
  border-radius: inherit;
  background: #e7ecea;
}

.kakao-map__canvas {
  width: 100%;
  height: 100%;
  min-height: inherit;
}

.kakao-map__state,
.kakao-map__empty {
  position: absolute;
  z-index: 2;
  inset: 0;
  display: grid;
  padding: 24px;
  place-content: center;
  justify-items: center;
  background: rgb(245 248 247 / 88%);
  color: #475569;
  text-align: center;
  backdrop-filter: blur(3px);
}

.kakao-map__state p,
.kakao-map__empty p {
  max-width: 300px;
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.kakao-map__state button {
  min-height: 38px;
  margin-top: 14px;
  padding: 0 16px;
  border: 0;
  border-radius: 10px;
  background: #ff5a4e;
  color: #fff;
  font-weight: 750;
  cursor: pointer;
}

.kakao-map__state--error strong {
  color: #b91c1c;
}

.kakao-map__spinner {
  width: 32px;
  height: 32px;
  margin-bottom: 12px;
  border: 3px solid #cbd5e1;
  border-top-color: #ff5a4e;
  border-radius: 50%;
  animation: map-spin 800ms linear infinite;
}

.kakao-map__empty span {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 14px;
  background: #fff;
  color: #ff5a4e;
  box-shadow: 0 8px 24px rgb(15 23 42 / 10%);
  font-size: 24px;
}

:global(.kakao-map__info-window) {
  max-width: 220px;
  padding: 8px 12px;
  overflow: hidden;
  color: #1e293b;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.kakao-map__detail-window) {
  width: min(360px, calc(100vw - 48px));
  padding: 2px;
}

:global(.kakao-map__detail-window .place-detail-card) {
  border-color: #cbd5e1;
  background: #fff;
  box-shadow: 0 14px 36px rgb(15 23 42 / 18%);
}

@keyframes map-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .kakao-map__spinner {
    animation: none;
  }
}
</style>
