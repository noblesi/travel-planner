<script setup>
import { computed, ref, watch } from 'vue'

import { searchPlaces } from '@/api/places'
import { readLocalStorage, writeLocalStorage } from '@/utils/browserStorage'

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
  scheduleItems: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['results-change', 'select'])

const PAGE_SIZE = 10
const keyword = ref('')
const searchedKeyword = ref('')
const status = ref('idle')
const errorMessage = ref('')
const places = ref([])
const page = ref(1)
const totalCount = ref(0)
const hasNext = ref(false)
const categoryFilter = ref('ALL')
const categories = ref([])
const recentKeywords = ref(loadRecentKeywords())
let requestSequence = 0

const isLoading = computed(() => status.value === 'loading')
const categoryOptions = computed(() => ['ALL', ...categories.value])

function loadRecentKeywords() {
  try {
    const values = JSON.parse(readLocalStorage('planEditorRecentKeywords', '[]'))
    return Array.isArray(values)
      ? values.filter((value) => typeof value === 'string').slice(0, 5)
      : []
  } catch {
    return []
  }
}

function rememberKeyword(value) {
  recentKeywords.value = [
    value,
    ...recentKeywords.value.filter((keyword) => keyword !== value),
  ].slice(0, 5)
  writeLocalStorage('planEditorRecentKeywords', JSON.stringify(recentKeywords.value))
}

function searchRecent(value) {
  keyword.value = value
  startSearch()
}

function registeredTimeSlots(place) {
  return props.scheduleItems
    .filter(
      (item) =>
        item.placeProvider === place.placeProvider &&
        String(item.externalPlaceId) === String(place.externalPlaceId),
    )
    .map((item) => item.timeSlot)
}

function placeId(place) {
  return String(`${place.placeProvider}:${place.externalPlaceId}`)
}

function searchErrorMessage(error) {
  const code = error?.response?.data?.code
  if (code === 'TOUR_API_NOT_CONFIGURED' || code === 'KAKAO_LOCAL_NOT_CONFIGURED') {
    return '장소 검색 설정을 확인해 주세요.'
  }
  if (
    code === 'TOUR_API_TIMEOUT' ||
    code === 'TOUR_API_UNAVAILABLE' ||
    code === 'KAKAO_LOCAL_TIMEOUT' ||
    code === 'KAKAO_LOCAL_UNAVAILABLE'
  ) {
    return '장소 검색 서비스 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.'
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
  categories.value = []
  emit('results-change', [])
}

function startSearch() {
  categoryFilter.value = 'ALL'
  executeSearch(1)
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
    const searchParams = {
      keyword: normalizedKeyword,
      regionCode: props.regionCode,
      page: targetPage,
      size: PAGE_SIZE,
    }
    if (categoryFilter.value !== 'ALL') {
      searchParams.category = categoryFilter.value
    }
    const data = await searchPlaces(searchParams)
    if (currentRequest !== requestSequence) return

    const nextPlaces = Array.isArray(data.places) ? data.places : []
    searchedKeyword.value = normalizedKeyword
    rememberKeyword(normalizedKeyword)
    places.value = nextPlaces
    categories.value = Array.isArray(data.categories) ? data.categories : []
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

    <form class="place-search-panel__form" role="search" @submit.prevent="startSearch">
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

    <div v-if="recentKeywords.length" class="recent-searches" aria-label="최근 검색어">
      <span>최근</span>
      <button
        v-for="recent in recentKeywords"
        :key="recent"
        type="button"
        @click="searchRecent(recent)"
      >
        {{ recent }}
      </button>
    </div>

    <p v-if="status === 'loading'" class="place-search-panel__state" role="status">
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
        <label>
          <span class="sr-only">카테고리 필터</span>
          <select
            v-model="categoryFilter"
            :disabled="isLoading"
            @change="executeSearch(1)"
          >
            <option value="ALL">전체 카테고리</option>
            <option v-for="category in categoryOptions.slice(1)" :key="category" :value="category">
              {{ category }}
            </option>
          </select>
        </label>
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
              <em v-if="registeredTimeSlots(place).length">
                {{
                  registeredTimeSlots(place)
                    .map((slot) => (slot === 'MORNING' ? '오전' : '오후'))
                    .join('·')
                }}
                등록됨
              </em>
            </span>
          </button>
        </li>
      </ul>

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
  height: 100%;
  min-height: 0;
  flex-direction: column;
  padding: 18px;
  overflow: hidden;
  background: #fff;
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
  color: var(--color-brand);
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
.recent-searches {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 9px;
  overflow: hidden;
}
.recent-searches > span {
  color: #94a3b8;
  font-size: 9px;
}
.recent-searches button {
  max-width: 84px;
  padding: 4px 7px;
  overflow: hidden;
  color: #64748b;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #fff;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
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
  border-color: var(--color-brand);
  box-shadow: 0 0 0 3px var(--color-brand-focus);
}

.place-search-panel__form button,
.place-search-panel__pagination button {
  min-height: 42px;
  padding: 0 14px;
  border: 0;
  border-radius: 11px;
  background: var(--color-brand);
  color: #fff;
  font-weight: 750;
  cursor: pointer;
}

.place-search-panel__form button:disabled,
.place-search-panel__pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

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
.place-search-panel__summary select {
  max-width: 150px;
  min-height: 30px;
  padding: 0 8px;
  color: #475569;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
  font-size: 10px;
}

.place-search-panel__results {
  display: grid;
  flex: 1;
  align-content: start;
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
  border-color: var(--color-brand-border);
  background: var(--color-brand-soft);
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
.place-result__body > em {
  display: block;
  margin-top: 3px;
  color: #16a34a;
  font-size: 9px;
  font-style: normal;
  font-weight: 800;
}

.place-result__body > small {
  color: var(--color-brand);
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
  margin-top: 4px;
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
</style>
