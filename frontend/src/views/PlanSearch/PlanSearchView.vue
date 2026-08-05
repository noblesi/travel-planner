<template>
  <DefaultLayout>
    <div class="search-page">
      <div class="app-container search-page__inner">
        <div class="pg-head">
          <div class="eyebrow">DISCOVER ITINERARIES</div>
          <div class="pg-title">다른 사람들은 어떻게 떠날까요?</div>
          <div class="pg-subtitle">
            먼저 다녀온 여행자들의 일정을 그대로 가져와 나만의 여행을 시작해보세요.
          </div>
        </div>

        <div class="search-wrap">
          <i class="ti ti-search" aria-hidden="true" @click="handleSearch"></i>
          <input
            class="search-input"
            type="text"
            v-model="keyword"
            placeholder="목적지 검색 (예: 서울, 부산, 제주)"
            @keyup.enter="handleSearch"
          />
        </div>

        <div v-if="!hasSearched" class="suggested-tags">
          <span class="suggested-label">이런 여행지는 어때요?</span>
          <div class="suggested-tag-list">
            <button
              v-for="city in suggestedCities"
              :key="city"
              class="suggested-tag"
              @click="searchSuggested(city)"
            >
              {{ city }}
            </button>
          </div>
        </div>

        <div v-if="loading" class="status-wrap" role="status">공개 일정을 불러오는 중이에요.</div>
        <div v-else-if="errorMessage" class="status-wrap status-wrap--error" role="alert">
          <span>{{ errorMessage }}</span>
          <button type="button" @click="loadPlans(searchedKeyword, currentPage)">다시 시도</button>
        </div>

        <div v-if="hasSearched && !loading" class="result-meta">
          <div class="result-count">
            "{{ searchedKeyword }}" 검색 결과
            <em v-if="filteredPlans.length > 0">{{ totalCount }}개</em>
            <span v-else class="zero">0개</span>
          </div>
        </div>

        <div v-if="!hasSearched || filteredPlans.length > 0" class="divider"></div>

        <div v-if="!hasSearched || filteredPlans.length > 0" class="grid">
          <button
            v-for="plan in filteredPlans"
            :key="plan.id"
            type="button"
            class="card"
            @click="goToDetail(plan.id)"
          >
            <div class="card-img-wrap">
              <img
                class="card-img"
                :src="plan.thumbImage"
                alt=""
                @error="useDefaultThumbnail"
              />
              <div class="badge-days">{{ plan.days }}일</div>
            </div>
            <div class="card-body">
              <div class="card-top">
                <span class="region-badge" :class="'region-' + regionColorKey(plan.region)">{{
                  plan.region
                }}</span>
              </div>
              <div class="card-title">{{ plan.title }}</div>
              <div class="card-foot">
                <div class="author">
                  <div
                    class="avatar"
                    :style="
                      plan.authorAvatar ? { backgroundImage: `url(${plan.authorAvatar})` } : {}
                    "
                  >
                    <span v-if="!plan.authorAvatar">{{ plan.authorInitials }}</span>
                  </div>
                  <span class="author-name">{{ plan.authorName }}</span>
                </div>
                <div class="stats">
                  <span class="stat stat-like"
                    ><i class="ti ti-heart" aria-hidden="true"></i>{{ plan.likeCount }}</span
                  >
                  <span class="stat"
                    ><i class="ti ti-eye" aria-hidden="true"></i
                    >{{ formatCount(plan.viewCount) }}</span
                  >
                </div>
              </div>
            </div>
          </button>
        </div>

        <div v-if="hasMore" class="more">
          <button class="more-btn" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? '일정을 불러오는 중...' : '일정 더 보기' }}
          </button>
        </div>

        <div
          v-if="!loading && !errorMessage && hasSearched && filteredPlans.length === 0"
          class="empty-wrap"
        >
          <div class="divider"></div>
          <div class="empty-illus" aria-hidden="true">
            <svg width="200" height="200" viewBox="0 0 148 148" xmlns="http://www.w3.org/2000/svg">
              <circle
                cx="74"
                cy="74"
                r="62"
                fill="var(--color-brand-soft)"
                stroke="var(--color-brand-border)"
                stroke-width="1"
              />
              <circle cx="74" cy="74" r="40" fill="none" stroke="#e8d5d2" stroke-width="1.5" />
              <line
                x1="74"
                y1="34"
                x2="74"
                y2="114"
                stroke="#e0d0ce"
                stroke-width="1"
                stroke-dasharray="3 3"
              />
              <line
                x1="34"
                y1="74"
                x2="114"
                y2="74"
                stroke="#e0d0ce"
                stroke-width="1"
                stroke-dasharray="3 3"
              />
              <text x="74" y="28" text-anchor="middle" font-size="9" fill="#bbb">N</text>
              <text x="74" y="124" text-anchor="middle" font-size="9" fill="#bbb">S</text>
              <text x="122" y="78" text-anchor="middle" font-size="9" fill="#bbb">E</text>
              <text x="26" y="78" text-anchor="middle" font-size="9" fill="#bbb">W</text>
              <polygon points="74,48 78,74 74,70 70,74" fill="var(--color-brand-accent)" />
              <polygon points="74,100 78,74 74,78 70,74" fill="#ccc" />
              <circle cx="74" cy="74" r="4" fill="#fff" stroke="#ddd" stroke-width="1.5" />
              <circle cx="104" cy="44" r="13" fill="#fff" stroke="#f0e0de" stroke-width="1" />
              <line
                x1="98.5"
                y1="38.5"
                x2="109.5"
                y2="49.5"
                stroke="var(--color-brand-accent)"
                stroke-width="2.2"
                stroke-linecap="round"
              />
              <line
                x1="109.5"
                y1="38.5"
                x2="98.5"
                y2="49.5"
                stroke="var(--color-brand-accent)"
                stroke-width="2.2"
                stroke-linecap="round"
              />
            </svg>
          </div>

          <div class="empty-head">일정을 찾을 수 없어요</div>
          <div class="empty-sub">
            <em>"{{ searchedKeyword }}"</em>에 대한 여행 일정이 아직 없어요.<br />
            다른 국내 도시로 검색하거나 아래 추천 여행지를 둘러보세요.
          </div>

          <div class="suggest-label">이런 일정은 어떠세요?</div>
          <div class="suggest-chips">
            <button
              v-for="city in suggestedCities"
              :key="city"
              class="suggest-chip"
              @click="searchSuggested(city)"
            >
              {{ city }}
            </button>
          </div>

          <button class="browse-btn" @click="resetSearch">모든 일정 보기</button>
        </div>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { searchPublicPlans } from '@/api/plans'
import defaultPlanThumbnail from '@/assets/plan/default-plan-thumbnail.svg'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { usePlanSearchStore } from '@/stores/planSearch'

const router = useRouter()
const route = useRoute()
const planSearchStore = usePlanSearchStore()

const suggestedCities = ['서울', '제주', '부산', '경주', '전주']
const pageSize = 8
const initialKeyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
const legacyPage = Math.ceil((Number(route.query.count) || pageSize) / pageSize)
const initialPage = Math.max(Number(route.query.page) || legacyPage, 1)
const cachedSearch = planSearchStore.restoreSearch({
  searchedKeyword: initialKeyword,
  currentPage: initialPage,
})

const plans = ref(cachedSearch?.plans ?? [])
const loading = ref(false)
const loadingMore = ref(false)
const errorMessage = ref('')
const totalCount = ref(cachedSearch?.totalCount ?? 0)
const hasNextPage = ref(cachedSearch?.hasNextPage ?? false)
const keyword = ref(cachedSearch?.keyword ?? initialKeyword)
const searchedKeyword = ref(cachedSearch?.searchedKeyword ?? initialKeyword)
const hasSearched = ref(cachedSearch?.hasSearched ?? Boolean(initialKeyword))
const currentPage = ref(cachedSearch?.currentPage ?? initialPage)
let requestSequence = 0

// 카드를 클릭하면 상세 페이지로 이동
function goToDetail(planId) {
  cacheCurrentSearch()
  router.push({ name: 'plan-detail', params: { id: planId } })
}

const filteredPlans = computed(() => plans.value)

const hasMore = computed(() => hasNextPage.value)

function cacheCurrentSearch() {
  planSearchStore.cacheSearch({
    keyword: keyword.value,
    searchedKeyword: searchedKeyword.value,
    hasSearched: hasSearched.value,
    currentPage: currentPage.value,
    plans: plans.value,
    totalCount: totalCount.value,
    hasNextPage: hasNextPage.value,
  })
}

// 검색 상태가 바뀔 때마다 URL 쿼리에 반영한다.
// push가 아니라 replace를 쓰는 이유: 검색어를 입력하거나 "더 보기"를 누를 때마다
// 히스토리가 쌓이면, 상세페이지에서 뒤로가기를 여러 번 눌러야 탐색 페이지로 돌아오게 된다.
// replace는 현재 히스토리 항목을 덮어써서, 뒤로가기 한 번으로 바로 직전 탐색 상태로 돌아오게 한다.
function syncUrl() {
  const query = {}
  if (searchedKeyword.value) query.keyword = searchedKeyword.value
  if (currentPage.value > 1) query.page = currentPage.value
  router.replace({ query })
}

function mapPlan(plan) {
  return {
    id: plan.planId,
    title: plan.title,
    region: plan.regionName,
    days: plan.dayCount,
    likeCount: plan.likeCount,
    viewCount: plan.viewCount,
    authorInitials: Array.from(plan.authorName || '여행자').slice(0, 2).join(''),
    authorName: plan.authorName,
    authorAvatar: plan.authorProfileImageUrl,
    thumbImage: plan.thumbnailImageUrl || defaultPlanThumbnail,
  }
}

function useDefaultThumbnail(event) {
  const image = event.currentTarget
  if (image.dataset.fallbackApplied === 'true') return

  image.dataset.fallbackApplied = 'true'
  image.src = defaultPlanThumbnail
}

async function loadPlans(searchKeyword = '', targetPage = 1) {
  const sequence = ++requestSequence
  loading.value = true
  loadingMore.value = false
  errorMessage.value = ''
  plans.value = []
  totalCount.value = 0
  hasNextPage.value = false

  try {
    const restoredPlans = []
    const requestedTarget = Math.min(Math.max(Number(targetPage) || 1, 1), 25)
    let loadedPage = 0
    let result

    for (let page = 1; page <= requestedTarget; page += 1) {
      result = await searchPublicPlans({ keyword: searchKeyword, page, size: pageSize })
      if (sequence !== requestSequence) return
      restoredPlans.push(...result.plans.map(mapPlan))
      loadedPage = page
      if (!result.hasNext) break
    }

    plans.value = restoredPlans
    currentPage.value = loadedPage || 1
    totalCount.value = result?.totalCount || 0
    hasNextPage.value = Boolean(result?.hasNext)
    cacheCurrentSearch()
  } catch {
    if (sequence !== requestSequence) return
    plans.value = []
    totalCount.value = 0
    hasNextPage.value = false
    errorMessage.value = '공개 일정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

async function handleSearch() {
  const trimmed = keyword.value.trim()
  if (!trimmed) return
  searchedKeyword.value = trimmed
  hasSearched.value = true
  currentPage.value = 1
  syncUrl()
  await loadPlans(searchedKeyword.value, 1)
}

function searchSuggested(city) {
  keyword.value = city
  handleSearch()
}

async function resetSearch() {
  keyword.value = ''
  searchedKeyword.value = ''
  hasSearched.value = false
  currentPage.value = 1
  syncUrl()
  await loadPlans('', 1)
}

async function loadMore() {
  if (loadingMore.value || !hasNextPage.value) return

  const sequence = ++requestSequence
  const requestKeyword = searchedKeyword.value
  loadingMore.value = true
  errorMessage.value = ''
  try {
    const nextPage = currentPage.value + 1
    const result = await searchPublicPlans({
      keyword: requestKeyword,
      page: nextPage,
      size: pageSize,
    })
    if (sequence !== requestSequence) return

    plans.value.push(...result.plans.map(mapPlan))
    currentPage.value = result.page
    totalCount.value = result.totalCount
    hasNextPage.value = result.hasNext
    syncUrl()
    cacheCurrentSearch()
  } catch {
    if (sequence !== requestSequence) return
    errorMessage.value = '다음 일정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    if (sequence === requestSequence) loadingMore.value = false
  }
}

// 헤더의 "일정 탐색"을 이미 이 페이지에 있는 상태에서 다시 클릭하면
// 라우트가 바뀌지 않아 컴포넌트가 재마운트되지 않으므로, AppHeader가 쏘는
// 커스텀 이벤트를 받아 검색 상태를 새로고침한 것처럼 초기화한다.
onMounted(() => {
  window.addEventListener('plan-search:reset', resetSearch)
  if (!cachedSearch) loadPlans(searchedKeyword.value, currentPage.value)
})
onUnmounted(() => {
  requestSequence += 1
  window.removeEventListener('plan-search:reset', resetSearch)
})

function formatCount(n) {
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(n)
}

// region(목적지) 배지 색을 정하는 로직.
// DB에 카테고리 같은 별도 필드가 없고 region(지역명) 문자열 하나만 있으므로,
// 하드코딩된 매핑 테이블 대신 문자열을 해시해서 4가지 색상 중 하나를 결정론적으로 뽑는다.
// 이렇게 하면 새로운 지역이 추가돼도 코드를 손댈 필요가 없고, 같은 지역명은 항상 같은 색이 나온다.
const REGION_COLOR_KEYS = ['sunset', 'berry', 'ocean', 'forest']

function regionColorKey(region) {
  let hash = 0
  for (let i = 0; i < region.length; i++) {
    hash = (hash * 31 + region.charCodeAt(i)) % REGION_COLOR_KEYS.length
  }
  return REGION_COLOR_KEYS[hash]
}

</script>

<style scoped>
* {
  box-sizing: border-box;
}

/* 배경은 이 바깥 래퍼가 뷰포트 전체 폭으로 칠하고,
   폭 제한은 안쪽 .search-page__inner(app-container)가 맡는다.
   AppHeader/AppFooter와 같은 패턴 — 이렇게 분리하지 않으면
   app-container의 max-width 바깥으로 body의 --color-page(크림색)가 그대로 보인다. */
/* 홈 화면과 같은 브랜드 글로우를 제목 뒤 한 군데가 아니라, 좌우 여백 곳곳에 비정형적으로 흩뿌린다.
   검색 결과 카드 등 불투명한 콘텐츠에 자연히 가려지고, 카드가 없는 여백에서만 은은하게 드러난다. */
.search-page {
  background:
    radial-gradient(circle at 4% 8%, rgb(249 115 22 / 8%) 0%, rgb(249 115 22 / 0%) 38%),
    radial-gradient(circle at 97% 22%, rgb(249 115 22 / 6.5%) 0%, rgb(249 115 22 / 0%) 32%),
    radial-gradient(circle at 2% 55%, rgb(249 115 22 / 6%) 0%, rgb(249 115 22 / 0%) 35%),
    radial-gradient(circle at 96% 68%, rgb(249 115 22 / 7%) 0%, rgb(249 115 22 / 0%) 34%),
    radial-gradient(circle at 6% 90%, rgb(249 115 22 / 5%) 0%, rgb(249 115 22 / 0%) 28%),
    var(--color-page);
  color: #1a1a1a;
}

.search-page__inner {
  /* app-container 기본 padding-inline(--layout-gutter, 20px)은 헤더/푸터 내비게이션 기준으로
     좁게 잡힌 값이라, 카드 그리드가 있는 본문에는 좀 더 넉넉하게 덮어쓴다. */
  padding-inline: clamp(20px, 5vw, 64px);
  padding-block: 0 5rem;
}

.pg-head {
  text-align: center;
  padding: 3.5rem 0 2rem;
}

.eyebrow {
  font-size: 13px;
  letter-spacing: 0.14em;
  color: var(--color-brand);
  margin-bottom: 0.6rem;
  text-transform: uppercase;
}

.pg-title {
  font-size: 34px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 0.75rem;
}

.pg-subtitle {
  font-size: 15px;
  color: #999;
}

.search-wrap {
  position: relative;
  max-width: 600px;
  margin: 0 auto 2.25rem;
}

.search-wrap i {
  position: absolute;
  left: 20px;
  top: 50%;
  transform: translateY(-50%);
  color: #aaa;
  font-size: 20px;
  cursor: pointer;
}

.search-input {
  width: 100%;
  padding: 16px 22px 16px 52px;
  border: 1px solid #c4c4c4;
  border-radius: 30px;
  font-size: 16px;
  color: #1a1a1a;
  background: #fafafa;
  outline: none;
}

.search-input::placeholder {
  color: #999;
}

.search-input:focus {
  border-color: var(--color-brand-accent);
}

.suggested-tags {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 2.5rem;
}

.suggested-tag-list {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

.suggested-label {
  font-size: 13px;
  color: #888;
}

.suggested-tag {
  padding: 7px 18px;
  border-radius: 20px;
  border: 1px solid #c4c4c4;
  background: #fafafa;
  color: #666;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.15s;
}

.suggested-tag:hover {
  border-color: var(--color-brand-accent);
  background: var(--color-brand-soft);
  color: var(--color-brand);
}

.result-meta {
  margin-bottom: 1.25rem;
}

.status-wrap {
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #777;
  text-align: center;
}

.status-wrap--error {
  flex-direction: column;
  color: #8a4c45;
}

.status-wrap button {
  border: 1px solid var(--color-brand-border);
  border-radius: 999px;
  padding: 8px 16px;
  background: #fff;
  color: var(--color-brand);
  cursor: pointer;
}

.result-count {
  font-size: 15px;
  color: #666;
}

.result-count em {
  color: var(--color-brand);
  font-style: normal;
  font-weight: 600;
}

.result-count .zero {
  color: #aaa;
  font-weight: 500;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  padding: 0;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.1);
}

/* 이미지를 감싸는 별도 wrap을 둬서, 카드 자체는 overflow: hidden으로 모서리를 유지하면서
   내부 이미지만 hover 시 확대(zoom)되도록 한다. */
.card-img-wrap {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.card-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background-color: #f0f0f0;
  /* 이미지 로딩 전/실패 시 대체 배경 */
  transition: transform 0.35s ease;
}

.card:hover .card-img {
  transform: scale(1.08);
}

.badge-days {
  position: absolute;
  top: 14px;
  left: 14px;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 14px;
  z-index: 1;
}

.card-body {
  padding: 20px 20px 22px;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

/* 지역 배지: 카테고리 그룹별로 색을 다르게 줘서 카드 그리드에 컬러 리듬을 만든다. */
.region-badge {
  font-size: 12px;
  font-weight: 700;
  padding: 4px 11px;
  border-radius: 20px;
}

.region-sunset {
  background: #fde8e5;
  color: #b23a24;
}

.region-berry {
  background: #f3e6f8;
  color: #8e3aa8;
}

.region-ocean {
  background: #e2eefc;
  color: #1f5fae;
}

.region-forest {
  background: #e3f2e7;
  color: #24815a;
}

.region-neutral {
  background: #f0f0f0;
  color: #777;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 16px;
  line-height: 1.4;
  letter-spacing: -0.2px;
  /* 제목이 1줄이든 2줄이든 카드 높이가 달라지지 않도록 min-height로 2줄분 공간을 항상 확보한다.
     18px * 1.4(line-height) * 2줄 = 50.4px */
  min-height: 50.4px;
  /* 2줄을 초과하면 자동으로 말줄임(...) 처리해서 3줄 이상 늘어나는 것을 막는다. */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-foot {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: var(--color-brand-soft);
  background-size: cover;
  background-position: center;
  font-size: 11px;
  color: var(--color-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.author-name {
  font-size: 13px;
  color: #666;
  font-weight: 500;
  max-width: 74px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stats {
  display: flex;
  gap: 14px;
  margin-left: auto;
  flex-shrink: 0;
}

.stat {
  font-size: 13px;
  color: #bbb;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.15s;
}

.more {
  display: flex;
  justify-content: center;
  margin-top: 2.5rem;
}

.more-btn {
  padding: 13px 40px;
  border-radius: 26px;
  border: 1px solid #c4c4c4;
  background: #fff;
  color: #666;
  font-size: 15px;
  cursor: pointer;
}

.more-btn:hover {
  border-color: #999;
}

.empty-wrap {
  text-align: center;
  padding: 0 0 1.5rem;
}

.divider {
  height: 1px;
  background: #c4c4c4;
  margin: 0 0 3rem;
}

.empty-illus {
  margin: 0 auto 2.25rem;
}

.empty-illus svg {
  width: 200px;
  height: 200px;
}

.empty-head {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 0.75rem;
}

.empty-sub {
  font-size: 16px;
  color: #888;
  line-height: 1.75;
  margin-bottom: 2.5rem;
}

.empty-sub em {
  color: var(--color-brand);
  font-style: normal;
}

.suggest-label {
  font-size: 14px;
  color: #aaa;
  margin-bottom: 1rem;
}

.suggest-chips {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 2.25rem;
}

.suggest-chip {
  padding: 10px 24px;
  border-radius: 24px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #555;
  font-size: 15px;
  cursor: pointer;
}

.suggest-chip:hover {
  border-color: var(--color-brand-accent);
  color: var(--color-brand);
}

.browse-btn {
  padding: 14px 36px;
  background: var(--color-brand);
  color: var(--color-brand-on);
  border-radius: 26px;
  font-size: 16px;
  border: none;
  cursor: pointer;
}

.browse-btn:hover {
  background: var(--color-brand-hover);
}
</style>
