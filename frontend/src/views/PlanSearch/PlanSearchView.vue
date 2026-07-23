<template>
  <DefaultLayout>
    <div class="search-page">

      <div class="pg-head">
        <div class="eyebrow">DISCOVER ITINERARIES</div>
        <div class="pg-title">다른 사람들은 어떻게 떠날까요?</div>
        <div class="pg-subtitle">먼저 다녀온 여행자들의 일정을 그대로 가져와 나만의 여행을 시작해보세요.</div>
      </div>

      <div class="search-wrap">
        <i class="ti ti-search" aria-hidden="true"></i>
        <input class="search-input" type="text" v-model="keyword" placeholder="목적지 검색 (예: 서울, 부산, 제주)"
          @keyup.enter="handleSearch" />
      </div>

      <div v-if="!hasSearched" class="suggested-tags">
        <span class="suggested-label">이런 여행지는 어때요?</span>
        <button v-for="city in suggestedCities" :key="city" class="suggested-tag" @click="searchSuggested(city)">{{ city
        }}</button>
      </div>

      <div v-if="hasSearched" class="result-meta">
        <div class="result-count">
          "{{ searchedKeyword }}" 검색 결과
          <em v-if="filteredPlans.length > 0">{{ filteredPlans.length }}개</em>
          <span v-else class="zero">0개</span>
        </div>
      </div>

      <div v-if="!hasSearched || filteredPlans.length > 0" class="grid">
        <div v-for="plan in displayedPlans" :key="plan.id" class="card" @click="goToDetail(plan.id)">
          <div class="card-img-wrap">
            <div class="card-img" :style="{ backgroundImage: `url(${plan.thumbImage})` }"></div>
            <div class="badge-days">{{ plan.days }}일</div>
          </div>
          <div class="card-body">
            <div class="card-top">
              <span class="region-badge" :class="'region-' + regionColorKey(plan.region)">{{ plan.region }}</span>
            </div>
            <div class="card-title">{{ plan.title }}</div>
            <div class="card-foot">
              <div class="author">
                <div class="avatar" :style="plan.authorAvatar ? { backgroundImage: `url(${plan.authorAvatar})` } : {}">
                  <span v-if="!plan.authorAvatar">{{ plan.authorInitials }}</span>
                </div>
                <span class="author-name">{{ plan.authorName }}</span>
              </div>
              <div class="stats">
                <span class="stat stat-like"><i class="ti ti-heart" aria-hidden="true"></i>{{ plan.likeCount }}</span>
                <span class="stat"><i class="ti ti-eye" aria-hidden="true"></i>{{ formatCount(plan.viewCount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="hasMore" class="more">
        <button class="more-btn" @click="loadMore">일정 더 보기</button>
      </div>

      <div v-if="hasSearched && filteredPlans.length === 0" class="empty-wrap">
        <div class="divider"></div>
        <div class="empty-illus" aria-hidden="true">
          <svg width="200" height="200" viewBox="0 0 148 148" xmlns="http://www.w3.org/2000/svg">
            <circle cx="74" cy="74" r="62" fill="#fdf5f4" stroke="#f0e0de" stroke-width="1" />
            <circle cx="74" cy="74" r="40" fill="none" stroke="#e8d5d2" stroke-width="1.5" />
            <line x1="74" y1="34" x2="74" y2="114" stroke="#e0d0ce" stroke-width="1" stroke-dasharray="3 3" />
            <line x1="34" y1="74" x2="114" y2="74" stroke="#e0d0ce" stroke-width="1" stroke-dasharray="3 3" />
            <text x="74" y="28" text-anchor="middle" font-size="9" fill="#bbb">N</text>
            <text x="74" y="124" text-anchor="middle" font-size="9" fill="#bbb">S</text>
            <text x="122" y="78" text-anchor="middle" font-size="9" fill="#bbb">E</text>
            <text x="26" y="78" text-anchor="middle" font-size="9" fill="#bbb">W</text>
            <polygon points="74,48 78,74 74,70 70,74" fill="#D94530" />
            <polygon points="74,100 78,74 74,78 70,74" fill="#ccc" />
            <circle cx="74" cy="74" r="4" fill="#fff" stroke="#ddd" stroke-width="1.5" />
            <circle cx="104" cy="44" r="13" fill="#fff" stroke="#f0e0de" stroke-width="1" />
            <line x1="98.5" y1="38.5" x2="109.5" y2="49.5" stroke="#D94530" stroke-width="2.2" stroke-linecap="round" />
            <line x1="109.5" y1="38.5" x2="98.5" y2="49.5" stroke="#D94530" stroke-width="2.2" stroke-linecap="round" />
          </svg>
        </div>

        <div class="empty-head">일정을 찾을 수 없어요</div>
        <div class="empty-sub">
          <em>"{{ searchedKeyword }}"</em>에 대한 여행 일정이 아직 없어요.<br />
          다른 국내 도시로 검색하거나 아래 추천 여행지를 둘러보세요.
        </div>

        <div class="suggest-label">이런 일정은 어떠세요?</div>
        <div class="suggest-chips">
          <button v-for="city in suggestedCities" :key="city" class="suggest-chip" @click="searchSuggested(city)">
            {{ city }}
          </button>
        </div>

        <button class="browse-btn" @click="resetSearch">모든 일정 보기</button>
      </div>

    </div>
  </DefaultLayout>
</template>

<script setup>
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

function goToDetail(planId) {
  router.push({ name: 'plan-detail', params: { id: planId } })
}

// ── mock 데이터: 백엔드 연동 시 이 부분을 API 호출로 교체 ──
// 예: const plans = ref([]); onMounted(async () => { plans.value = await api.get('/plans') })
const plans = ref([
  {
    id: 1, title: '남산에서 한강까지 서울 야경 코스', region: '서울',
    description: 'N서울타워부터 반포 달빛무지개분수까지, 서울의 밤을 완성하는 루트',
    days: 3, likeCount: 318, viewCount: 2100, authorInitials: 'YK', authorName: '여행하는광인', authorAvatar: 'https://i.pravatar.cc/80?u=YK1',
    thumbImage: 'https://picsum.photos/seed/seoul-namsan/640/440',
  },
  {
    id: 2, title: '경복궁 & 북촌 한옥마을 당일 코스', region: '서울',
    description: '한복 입고 경복궁 거닐고 북촌 골목길 산책, 서울 속 조선 시대 여행',
    days: 2, likeCount: 452, viewCount: 3000, authorInitials: 'HS', authorName: '한스랑산책', authorAvatar: 'https://i.pravatar.cc/80?u=HS2',
    thumbImage: 'https://picsum.photos/seed/seoul-hanok/640/440',
  },
  {
    id: 3, title: '성수동 카페 & 팝업 투어', region: '서울',
    description: '서울에서 가장 힙한 동네, 성수동 핫플레이스를 반나절에 다 돌기',
    days: 1, likeCount: 267, viewCount: 1700, authorInitials: 'MJ', authorName: '민지의하루', authorAvatar: 'https://i.pravatar.cc/80?u=MJ3',
    thumbImage: 'https://picsum.photos/seed/seoul-cafe/640/440',
  },
  {
    id: 4, title: '뚝섬 한강공원 피크닉 & 자전거', region: '서울',
    description: '돗자리, 치맥, 자전거까지 한강을 200% 즐기는 도심 힐링 하루',
    days: 1, likeCount: 389, viewCount: 2400, authorInitials: 'PL', authorName: '플랜러', authorAvatar: 'https://i.pravatar.cc/80?u=PL4',
    thumbImage: 'https://picsum.photos/seed/seoul-han-river/640/440',
  },
  {
    id: 5, title: '해운대 & 광안리 부산 바다 여행', region: '부산',
    description: '해운대 해수욕장부터 광안대교 야경까지, 부산 바다의 정수만 담은 일정',
    days: 2, likeCount: 521, viewCount: 3800, authorInitials: 'KW', authorName: '강원도사람', authorAvatar: 'https://i.pravatar.cc/80?u=KW5',
    thumbImage: 'https://picsum.photos/seed/busan-beach/640/440',
  },
  {
    id: 6, title: '한라산 등반 & 제주 동쪽 드라이브', region: '제주',
    description: '한라산 정상부터 성산일출봉까지, 제주 자연을 깊게 경험하는 4일 코스',
    days: 4, likeCount: 476, viewCount: 3200, authorInitials: 'SR', authorName: '설레는여행', authorAvatar: 'https://i.pravatar.cc/80?u=SR6',
    thumbImage: 'https://picsum.photos/seed/jeju-mountain/640/440',
  },
  {
    id: 7, title: '전주 한옥마을 & 비빔밥 미식 여행', region: '전주',
    description: '700채 한옥이 모인 전주 한옥마을에서 먹고 걷고 쉬는 느린 여행',
    days: 2, likeCount: 334, viewCount: 2000, authorInitials: 'JH', authorName: '진하게한잔', authorAvatar: 'https://i.pravatar.cc/80?u=JH7',
    thumbImage: 'https://picsum.photos/seed/jeonju-hanok/640/440',
  },
  {
    id: 8, title: '경주 천년 고도 역사 탐방 3일', region: '경주',
    description: '불국사, 석굴암, 대릉원까지 신라 천년의 숨결을 따라가는 역사 여행',
    days: 3, likeCount: 298, viewCount: 1900, authorInitials: 'TK', authorName: '탐구생활', authorAvatar: 'https://i.pravatar.cc/80?u=TK8',
    thumbImage: 'https://picsum.photos/seed/gyeongju-history/640/440',
  },
  {
    id: 9, title: '속초 산과 바다 힐링 여행', region: '속초',
    description: '설악산과 바다를 하루씩 나눠 즐기는 2박3일 속초 투어',
    days: 3, likeCount: 241, viewCount: 1500, authorInitials: 'WJ', authorName: '원정대장', authorAvatar: 'https://i.pravatar.cc/80?u=WJ9',
    thumbImage: 'https://picsum.photos/seed/sokcho-nature/640/440',
  },
  {
    id: 10, title: '강릉 바다와 커피 여행', region: '강릉',
    description: '2025-2026년 베케이션으로 여름 붐빔을 위한 카페 투어',
    days: 2, likeCount: 356, viewCount: 2400, authorInitials: 'DH', authorName: '동해바다', authorAvatar: 'https://i.pravatar.cc/80?u=DH10',
    thumbImage: 'https://picsum.photos/seed/gangneung-coffee/640/440',
  },
  {
    id: 11, title: '여수 밤바다 완벽 코스', region: '여수',
    description: '4박5일 여수를 제대로 보러 낮과 밤 여행 코스로 오다',
    days: 3, likeCount: 289, viewCount: 1700, authorInitials: 'YT', authorName: '여수밤바다', authorAvatar: 'https://i.pravatar.cc/80?u=YT11',
    thumbImage: 'https://picsum.photos/seed/yeosu-night/640/440',
  },
  {
    id: 12, title: '인천 차이나타운 & 송도 야경 투어', region: '인천',
    description: '짜장면의 원조 차이나타운부터 송도 센트럴파크 야경까지',
    days: 1, likeCount: 178, viewCount: 1100, authorInitials: 'IC', authorName: '인천사는사람', authorAvatar: 'https://i.pravatar.cc/80?u=IC12',
    thumbImage: 'https://picsum.photos/seed/incheon-town/640/440',
  },
  {
    id: 13, title: '통영 바다케이블카 & 동피랑 벽화마을', region: '통영',
    description: '한려수도 절경을 케이블카로, 알록달록 벽화마을 산책까지',
    days: 2, likeCount: 312, viewCount: 2000, authorInitials: 'TY', authorName: '통영투어러', authorAvatar: 'https://i.pravatar.cc/80?u=TY13',
    thumbImage: 'https://picsum.photos/seed/tongyeong-cable/640/440',
  },
  {
    id: 14, title: '춘천 닭갈비 & 남이섬 당일치기', region: '춘천',
    description: '숯불 닭갈비 맛집 투어와 남이섬 메타세쿼이아 길 산책',
    days: 1, likeCount: 203, viewCount: 1400, authorInitials: 'CC', authorName: '춘천치즈', authorAvatar: 'https://i.pravatar.cc/80?u=CC14',
    thumbImage: 'https://picsum.photos/seed/chuncheon-food/640/440',
  },
  {
    id: 15, title: '거제도 바람의 언덕 드라이브', region: '거제',
    description: '해안도로를 따라 바람의 언덕, 외도 보타니아까지 이어지는 코스',
    days: 2, likeCount: 267, viewCount: 1800, authorInitials: 'GJ', authorName: '거제도민', authorAvatar: 'https://i.pravatar.cc/80?u=GJ15',
    thumbImage: 'https://picsum.photos/seed/geoje-drive/640/440',
  },
  {
    id: 16, title: '안동 하회마을 전통문화 체험', region: '안동',
    description: '유네스코 세계유산 하회마을에서 즐기는 조선시대 전통 문화 체험',
    days: 2, likeCount: 156, viewCount: 980, authorInitials: 'AD', authorName: '안동선비', authorAvatar: 'https://i.pravatar.cc/80?u=AD16',
    thumbImage: 'https://picsum.photos/seed/andong-culture/640/440',
  },
  {
    id: 17, title: '목포 근대문화유산 골목 여행', region: '목포',
    description: '일제강점기 건축물이 남아있는 근대문화거리를 걷는 느린 여행',
    days: 1, likeCount: 134, viewCount: 890, authorInitials: 'MP', authorName: '목포항구', authorAvatar: 'https://i.pravatar.cc/80?u=MP17',
    thumbImage: 'https://picsum.photos/seed/mokpo-history/640/440',
  },
  {
    id: 18, title: '보성 녹차밭 힐링 산책', region: '보성',
    description: '초록빛 녹차밭 사이를 걷는 초여름 힐링 코스',
    days: 1, likeCount: 198, viewCount: 1200, authorInitials: 'BS', authorName: '보성녹차', authorAvatar: 'https://i.pravatar.cc/80?u=BS18',
    thumbImage: 'https://picsum.photos/seed/boseong-tea/640/440',
  },
  {
    id: 19, title: '군산 근대역사 & 이성당 빵지순례', region: '군산',
    description: '일제강점기 건축물 탐방과 전국구 유명 빵집 투어까지',
    days: 1, likeCount: 223, viewCount: 1500, authorInitials: 'GS', authorName: '군산빵순이', authorAvatar: 'https://i.pravatar.cc/80?u=GS19',
    thumbImage: 'https://picsum.photos/seed/gunsan-bakery/640/440',
  },
  {
    id: 20, title: '단양 패러글라이딩 & 도담삼봉', region: '단양',
    description: '하늘에서 내려다보는 단양팔경과 도담삼봉 트레킹 코스',
    days: 2, likeCount: 287, viewCount: 1900, authorInitials: 'DY', authorName: '단양패러', authorAvatar: 'https://i.pravatar.cc/80?u=DY20',
    thumbImage: 'https://picsum.photos/seed/danyang-activity/640/440',
  },
])

const suggestedCities = ['서울', '제주', '부산', '경주', '전주']
const pageSize = 8

// ── URL 쿼리에서 검색 상태 복원 (뒤로가기로 돌아왔을 때 검색어/더보기 개수 유지) ──
const keyword = ref(route.query.keyword || '') // 입력받은 키워드 (사용자가 이미 검색했을 시 검색한 키워드 할당)
const searchedKeyword = ref(route.query.keyword || '') // 사용자가 실제로 검색한 키워드
const hasSearched = ref(!!route.query.keyword) // 사용자가 검색했는지 여부
const visibleCount = ref(Number(route.query.count) || pageSize) // 화면에 보여줄 플랜 수

const filteredPlans = computed(() => {
  if (!hasSearched.value) return plans.value
  const kw = searchedKeyword.value.trim()
  if (!kw) return plans.value
  return plans.value.filter(
    (p) => p.title.includes(kw) || p.region.includes(kw)
  )
})

const displayedPlans = computed(() => filteredPlans.value.slice(0, visibleCount.value))
const hasMore = computed(() => visibleCount.value < filteredPlans.value.length)

// 검색 상태가 바뀔 때마다 URL 쿼리에 반영한다.
// push가 아니라 replace를 쓰는 이유: 검색어를 입력하거나 "더 보기"를 누를 때마다
// 히스토리가 쌓이면, 상세페이지에서 뒤로가기를 여러 번 눌러야 탐색 페이지로 돌아오게 된다.
// replace는 현재 히스토리 항목을 덮어써서, 뒤로가기 한 번으로 바로 직전 탐색 상태로 돌아오게 한다.
function syncUrl() {
  const query = {}
  if (searchedKeyword.value) query.keyword = searchedKeyword.value
  if (visibleCount.value !== pageSize) query.count = visibleCount.value
  router.replace({ query })
}

function handleSearch() {
  searchedKeyword.value = keyword.value.trim()
  hasSearched.value = true
  visibleCount.value = pageSize
  syncUrl()
}

function searchSuggested(city) {
  keyword.value = city
  handleSearch()
}

function resetSearch() {
  keyword.value = ''
  hasSearched.value = false
  visibleCount.value = pageSize
  syncUrl()
}

function loadMore() {
  visibleCount.value += pageSize
  syncUrl()
}

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

.search-page {
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
  background: #ffffff;
  color: #1a1a1a;
  max-width: 1280px;
  margin: 0 auto;
  /* 헤더/푸터가 레이아웃으로 감싸는 구조이므로,
     페이지 자체는 아래쪽에 큰 여백을 두지 않고 다음 요소(푸터)에 맡긴다. */
  padding: 0 3rem 1.5rem;
}

.pg-head {
  text-align: center;
  padding: 3.5rem 0 2rem;
}

.eyebrow {
  font-size: 13px;
  letter-spacing: .14em;
  color: #D94530;
  margin-bottom: .6rem;
  text-transform: uppercase;
}

.pg-title {
  font-size: 34px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: .75rem;
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
}

.search-input {
  width: 100%;
  padding: 16px 22px 16px 52px;
  border: 1px solid #e0e0e0;
  border-radius: 30px;
  font-size: 16px;
  color: #1a1a1a;
  background: #fafafa;
  outline: none;
}

.search-input:focus {
  border-color: #D94530;
}

.suggested-tags {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 2.5rem;
}

.suggested-label {
  font-size: 13px;
  color: #bbb;
  margin-right: 4px;
}

.suggested-tag {
  padding: 7px 18px;
  border-radius: 20px;
  border: 1px solid #eee;
  background: #fafafa;
  color: #666;
  font-size: 13.5px;
  cursor: pointer;
  transition: all .15s;
}

.suggested-tag:hover {
  border-color: #D94530;
  background: #fdf5f4;
  color: #D94530;
}

.result-meta {
  margin-bottom: 1.25rem;
}

.result-count {
  font-size: 15px;
  color: #666;
}

.result-count em {
  color: #D94530;
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
  border: 1px solid #ebebeb;
  overflow: hidden;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgba(0, 0, 0, .1);
  border-color: #f0e0de;
}

/* 이미지를 감싸는 별도 wrap을 둬서, 카드 자체는 overflow: hidden으로 모서리를 유지하면서
   내부 이미지만 hover 시 확대(zoom)되도록 한다. */
.card-img-wrap {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  background-color: #f0f0f0;
  /* 이미지 로딩 전/실패 시 대체 배경 */
  transition: transform .35s ease;
}

.card:hover .card-img {
  transform: scale(1.08);
}

.badge-days {
  position: absolute;
  top: 14px;
  left: 14px;
  background: rgba(0, 0, 0, .5);
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
  letter-spacing: -.2px;
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
  background-color: #fde8e5;
  background-size: cover;
  background-position: center;
  font-size: 11px;
  color: #D94530;
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
  transition: color .15s;
}

/* 좋아요 수치는 카드 hover 시 브랜드 컬러로 살짝 강조해서 "인터랙션 대상"이라는 신호를 준다. */
.card:hover .stat-like {
  color: #D94530;
}


.more {
  display: flex;
  justify-content: center;
  margin-top: 2.5rem;
}

.more-btn {
  padding: 13px 40px;
  border-radius: 26px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #666;
  font-size: 15px;
  cursor: pointer;
}

.more-btn:hover {
  border-color: #ccc;
}

.empty-wrap {
  text-align: center;
  padding: 0 0 1.5rem;
}

.divider {
  height: 1px;
  background: #f0f0f0;
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
  margin-bottom: .75rem;
}

.empty-sub {
  font-size: 16px;
  color: #888;
  line-height: 1.75;
  margin-bottom: 2.5rem;
}

.empty-sub em {
  color: #D94530;
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
  border-color: #D94530;
  color: #D94530;
}

.browse-btn {
  padding: 14px 36px;
  background: #D94530;
  color: #fff;
  border-radius: 26px;
  font-size: 16px;
  border: none;
  cursor: pointer;
}

.browse-btn:hover {
  background: #c23d2b;
}
</style>