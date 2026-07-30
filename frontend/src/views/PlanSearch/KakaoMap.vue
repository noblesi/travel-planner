<template>
  <div ref="mapContainer" class="kakao-map"></div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'

const props = defineProps({
  // { id, name, lat, lng }[] 형태의 마커 목록
  places: { type: Array, required: true, default: () => [] },
})

const mapContainer = ref(null)
let mapInstance = null
let markers = []
let infowindow = null

// 카카오맵 SDK는 index.html에 아래 형태로 한 번만 로드해두고,
// autoload=false로 두면 kakao.maps.load(callback)으로 필요한 시점에 초기화할 수 있다.
//   예: <script src="//dapi.kakao.com/v2/maps/sdk.js?appkey=YOUR_APP_KEY&libraries=services&autoload=false">(스크립트 닫는 태그)
//
// appkey는 .env 파일에 VITE_KAKAO_MAP_KEY로 넣고, index.html에서
// %VITE_KAKAO_MAP_KEY% 같은 치환 없이 직접 쓸 수 없으므로,
// Vite라면 index.html의 스크립트 태그 대신 아래처럼 런타임에 스크립트를 동적 주입하는 방식을 쓴다.
function loadKakaoMapScript() {
  return new Promise((resolve, reject) => {
    // 이미 로드되어 있으면 바로 resolve
    if (window.kakao && window.kakao.maps) {
      resolve()
      return
    }

    const existingScript = document.getElementById('kakao-map-sdk')
    if (existingScript) {
      existingScript.addEventListener('load', () => window.kakao.maps.load(resolve))
      return
    }

    const appKey = import.meta.env.VITE_KAKAO_MAP_KEY
    if (!appKey) {
      reject(new Error('VITE_KAKAO_MAP_KEY가 설정되지 않았습니다. .env 파일을 확인하세요.'))
      return
    }

    const script = document.createElement('script')
    script.id = 'kakao-map-sdk'
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${appKey}&libraries=services&autoload=false`
    script.onload = () => window.kakao.maps.load(resolve)
    script.onerror = () => reject(new Error('카카오맵 SDK 로드에 실패했습니다.'))
    document.head.appendChild(script)
  })
}

async function initMap() {
  if (!props.places.length) return

  try {
    await loadKakaoMapScript()
  } catch (err) {
    console.error(err)
    return
  }

  await nextTick()
  if (!mapContainer.value) return

  const { kakao } = window
  const center = new kakao.maps.LatLng(props.places[0].lat, props.places[0].lng)

  mapInstance = new kakao.maps.Map(mapContainer.value, {
    center,
    level: 6,
  })

  infowindow = new kakao.maps.InfoWindow({ removable: true })

  renderMarkers()
}

function clearMarkers() {
  markers.forEach((marker) => marker.setMap(null))
  markers = []
}

function renderMarkers() {
  if (!mapInstance) return
  const { kakao } = window

  clearMarkers()

  const bounds = new kakao.maps.LatLngBounds()

  props.places.forEach((place) => {
    const position = new kakao.maps.LatLng(place.lat, place.lng)
    bounds.extend(position)

    // 첫 번째 장소는 검정, 나머지는 브랜드 컬러로 구분해 SVG 목업 때의 시각 규칙을 유지한다.
    const marker = new kakao.maps.Marker({
      map: mapInstance,
      position,
      title: place.name,
    })

    kakao.maps.event.addListener(marker, 'click', () => {
      infowindow.setContent(
        `<div style="padding:6px 10px;font-size:12px;white-space:nowrap;">${place.name}</div>`
      )
      infowindow.open(mapInstance, marker)
    })

    markers.push(marker)
  })

  mapInstance.setBounds(bounds)
}

onMounted(initMap)

// DAY를 전환하면 부모가 다른 places 배열을 넘겨주므로, 지도를 다시 그린다.
watch(
  () => props.places,
  async () => {
    if (!mapInstance) {
      await initMap()
      return
    }
    renderMarkers()
  }
)
</script>

<style scoped>
.kakao-map {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  overflow: hidden;
}
</style>
