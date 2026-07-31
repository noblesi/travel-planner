<script setup>
import { computed, ref, watch } from 'vue'

import { searchPlaces } from '@/api/places'
import PlaceDetailCard from '@/components/plan/PlaceDetailCard.vue'

const props = defineProps({
  regionCode: {
    type: String,
    default: '',
  },
  regionName: {
    type: String,
    default: '',
  },
  selectedPlaceId: {
    type: [String, Number],
    default: null,
  },
  scheduleDisabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['results-change', 'select', 'add'])

const PAGE_SIZE = 10
const keyword = ref('')
const searchedKeyword = ref('')
const status = ref('idle')
const errorMessage = ref('')
const places = ref([])
const page = ref(1)
const totalCount = ref(0)
const hasNext = ref(false)
let requestSequence = 0

const isLoading = computed(() => status.value === 'loading')
const selectedPlace = computed(() => {
  if (props.selectedPlaceId == null) return null
  const id = String(props.selectedPlaceId)
  return places.value.find((place) => placeId(place) === id) ?? null
})

function placeId(place) {
  return String(`${place.placeProvider}:${place.externalPlaceId}`)
}

function searchErrorMessage(error) {
  const code = error?.response?.data?.code
  if (code === 'TOUR_API_NOT_CONFIGURED') {
    return '장소 검색 설정을 확인해 주세요.'
  }
  if (code === 'TOUR_API_TIMEOUT' || code === 'TOUR_API_UNAVAILABLE') {
    return '관광정보 서비스 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '장소를 검색하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

function validateKeyword(value) {
  if (!value) return '검색어를 입력해 주세요.'
  if (value.length > 100) return '검색어는 100자 이하로 입력해 주세요.'
  return ''
}

function resetResults(nextStatus = 'idle') {
  status.value = nextStatus
  places.value = []
  page.value = 1
  totalCount.value = 0
  hasNext.value = false
  emit('results-change', [])
}

async function executeSearch(targetPage = 1) {
  const normalizedKeyword = keyword.value.trim()
  const validationMessage = validateKeyword(normalizedKeyword)
  if (validationMessage) {
    requestSequence += 1
    errorMessage.value = validationMessage
    resetResults('validation-error')
    return
  }

  const currentRequest = ++requestSequence
  status.value = 'loading'
  errorMessage.value = ''

  try {
    const data = await searchPlaces({
      keyword: normalizedKeyword,
      regionCode: props.regionCode,
      page: targetPage,
      size: PAGE_SIZE,
    })
    if (currentRequest !== requestSequence) return

    const nextPlaces = Array.isArray(data.places) ? data.places : []
    searchedKeyword.value = normalizedKeyword
    places.value = nextPlaces
    page.value = data.page ?? targetPage
    totalCount.value = data.totalCount ?? nextPlaces.length
    hasNext.value = Boolean(data.hasNext)
    status.value = nextPlaces.length ? 'success' : 'empty'
    emit('results-change', nextPlaces)

    const selectedId = props.selectedPlaceId == null ? null : String(props.selectedPlaceId)
    if (selectedId && !nextPlaces.some((place) => placeId(place) === selectedId)) {
      emit('select', null)
    }
  } catch (error) {
    if (currentRequest !== requestSequence) return

    errorMessage.value = searchErrorMessage(error)
    resetResults('error')
  }
}

function selectPlace(place) {
  emit('select', place)
}

watch(
  () => props.regionCode,
  () => {
    requestSequence += 1
    searchedKeyword.value = ''
    errorMessage.value = ''
    resetResults()
    emit('select', null)
  },
)
</script>

<template>
  <section class="place-search-panel" aria-labelledby="place-search-heading">
    <header class="place-search-panel__header">
      <div>
        <span>PLACE SEARCH</span>
        <h2 id="place-search-heading">장소 찾기</h2>
      </div>
      <small>{{ regionName || '전국' }}</small>
    </header>

    <form class="place-search-panel__form" role="search" @submit.prevent="executeSearch(1)">
      <label class="sr-only" for="place-keyword">장소 검색어</label>
      <input
        id="place-keyword"
        v-model="keyword"
        name="placeKeyword"
        type="search"
        maxlength="100"
        placeholder="관광지, 명소를 검색하세요"
        autocomplete="off"
      />
      <button type="submit" :disabled="isLoading" :aria-busy="isLoading">
        {{ isLoading ? '검색 중' : '검색' }}
      </button>
    </form>

    <p v-if="status === 'idle'" class="place-search-panel__guide">
      {{ regionName || '전국' }}의 관광정보를 TourAPI에서 검색합니다.
    </p>
    <p v-else-if="status === 'loading'" class="place-search-panel__state" role="status">
      검색 결과를 불러오고 있습니다.
    </p>
    <p
      v-else-if="status === 'validation-error' || status === 'error'"
      class="place-search-panel__state place-search-panel__state--error"
      role="alert"
    >
      {{ errorMessage }}
    </p>
    <p v-else-if="status === 'empty'" class="place-search-panel__state" role="status">
      “{{ searchedKeyword }}” 검색 결과가 없습니다.
    </p>

    <template v-else-if="status === 'success'">
      <div class="place-search-panel__summary">
        <strong>검색 결과 {{ totalCount.toLocaleString('ko-KR') }}곳</strong>
        <span>{{ page }}페이지</span>
      </div>

      <ul class="place-search-panel__results">
        <li v-for="place in places" :key="placeId(place)">
          <button
            type="button"
            :class="{ 'place-result--selected': placeId(place) === String(selectedPlaceId) }"
            :aria-pressed="placeId(place) === String(selectedPlaceId)"
            @click="selectPlace(place)"
          >
            <img v-if="place.imageUrl" :src="place.imageUrl" alt="" />
            <span v-else class="place-result__image-empty" aria-hidden="true">⌖</span>
            <span class="place-result__body">
              <small>{{ place.categoryName || '관광지' }}</small>
              <strong>{{ place.placeName }}</strong>
              <span>{{ place.address || '주소 정보 없음' }}</span>
            </span>
          </button>
        </li>
      </ul>

      <PlaceDetailCard
        v-if="selectedPlace"
        :place="selectedPlace"
        :add-disabled="scheduleDisabled"
        @add="emit('add', { place: selectedPlace, timeSlot: $event })"
      />

      <nav class="place-search-panel__pagination" aria-label="장소 검색 결과 페이지">
        <button type="button" :disabled="page <= 1 || isLoading" @click="executeSearch(page - 1)">
          이전
        </button>
        <span>{{ page }}</span>
        <button type="button" :disabled="!hasNext || isLoading" @click="executeSearch(page + 1)">
          다음
        </button>
      </nav>
    </template>
  </section>
</template>

<style scoped>
.place-search-panel {
  display: flex;
  max-height: calc(100vh - 138px);
  flex-direction: column;
  padding: 18px;
  overflow: hidden;
  border: 1px solid rgb(148 163 184 / 48%);
  border-radius: 18px;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 18px 48px rgb(15 23 42 / 18%);
  backdrop-filter: blur(12px);
}

.place-search-panel__header,
.place-search-panel__summary,
.place-search-panel__pagination {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.place-search-panel__header span {
  color: #ff5a4e;
  font-size: 9px;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.place-search-panel__header h2 {
  margin: 2px 0 0;
  color: #1e293b;
  font-size: 20px;
}

.place-search-panel__header small {
  max-width: 130px;
  overflow: hidden;
  padding: 6px 9px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-search-panel__form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-top: 14px;
}

.place-search-panel__form input {
  min-width: 0;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid #dbe2ea;
  border-radius: 11px;
  outline: 0;
  color: #1e293b;
}

.place-search-panel__form input:focus {
  border-color: #ff8b82;
  box-shadow: 0 0 0 3px rgb(255 90 78 / 12%);
}

.place-search-panel__form button,
.place-search-panel__pagination button {
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 11px;
  background: #ff5a4e;
  color: #fff;
  font-weight: 750;
  cursor: pointer;
}

.place-search-panel__form button:disabled,
.place-search-panel__pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.place-search-panel__guide,
.place-search-panel__state {
  margin: 14px 0 0;
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-align: center;
}

.place-search-panel__state--error {
  background: #fff1f2;
  color: #b91c1c;
}

.place-search-panel__summary {
  margin-top: 14px;
  color: #64748b;
  font-size: 11px;
}

.place-search-panel__summary strong {
  color: #334155;
}

.place-search-panel__results {
  display: grid;
  gap: 7px;
  min-height: 0;
  margin: 10px -4px 10px 0;
  padding: 0 4px 0 0;
  overflow-y: auto;
  list-style: none;
}

.place-search-panel__results > li > button {
  display: grid;
  width: 100%;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 10px;
  padding: 8px;
  border: 1px solid #e5eaf1;
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.place-search-panel__results > li > button:hover,
.place-search-panel__results > li > .place-result--selected {
  border-color: #ff9f97;
  background: #fff8f7;
}

.place-search-panel__results img,
.place-result__image-empty {
  width: 54px;
  height: 54px;
  border-radius: 9px;
  object-fit: cover;
}

.place-result__image-empty {
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 18px;
}

.place-result__body {
  display: block;
  min-width: 0;
}

.place-result__body > small,
.place-result__body > strong,
.place-result__body > span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-result__body > small {
  color: #ff5a4e;
  font-size: 9px;
  font-weight: 800;
}

.place-result__body > strong {
  margin-top: 3px;
  color: #1e293b;
  font-size: 13px;
}

.place-result__body > span {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 10px;
}

.place-search-panel__pagination {
  margin-top: 12px;
}

.place-search-panel__pagination button {
  min-height: 34px;
  padding: 0 12px;
  background: #475569;
  font-size: 11px;
}

.place-search-panel__pagination span {
  color: #475569;
  font-size: 12px;
  font-weight: 800;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 520px) {
  .place-search-panel {
    max-height: 440px;
    padding: 14px;
  }
}
</style>
