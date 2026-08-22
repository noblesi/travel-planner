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
          <button type="button" @click="retryLoad">다시 시도</button>
        </div>

        <div v-if="hasSearched && !loading" class="result-meta">
          <div class="result-count">
            "{{ searchedKeyword }}" 검색 결과
            <em v-if="plans.length > 0">{{ totalCount }}개</em>
            <span v-else class="zero">0개</span>
          </div>
        </div>

        <div v-if="!hasSearched || plans.length > 0" class="divider"></div>

        <div v-if="!hasSearched || plans.length > 0" class="grid">
          <PublicPlanCard
            v-for="plan in plans"
            :key="plan.id"
            :plan="plan"
            @select="goToDetail"
          />
        </div>

        <div v-if="hasMore" class="more">
          <button class="more-btn" :disabled="loadingMore" @click="loadMore">
            {{ loadingMore ? '일정을 불러오는 중...' : '일정 더 보기' }}
          </button>
        </div>

        <div
          v-if="!loading && !errorMessage && hasSearched && plans.length === 0"
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
import PublicPlanCard from '@/components/plan/PublicPlanCard.vue'
import { usePlanSearch } from '@/composables/usePlanSearch'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const suggestedCities = ['서울', '제주', '부산', '경주', '전주']
const {
  plans,
  loading,
  loadingMore,
  errorMessage,
  totalCount,
  keyword,
  searchedKeyword,
  hasSearched,
  hasMore,
  openPlanDetail: goToDetail,
  retryLoad,
  search: handleSearch,
  searchSuggested,
  resetSearch,
  loadMore,
} = usePlanSearch()

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
