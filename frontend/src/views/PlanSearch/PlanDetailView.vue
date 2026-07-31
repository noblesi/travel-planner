<template>
  <DefaultLayout>
    <div class="detail-page">
      <div class="detail-card">
        <div class="detail-head">
          <button class="back-link" title="여행플랜 상세페이지로 돌아가기" @click="goBack">
            <i class="ti ti-arrow-left" aria-hidden="true"></i>
          </button>

          <h1 class="plan-title">{{ plan.title }}</h1>

          <div class="plan-author">
            <span class="author-dot"></span>{{ plan.authorName }}님의 여행 · {{ plan.periodLabel }}
          </div>

          <div class="head-actions">
            <button class="like-stat" :class="{ liked: plan.liked }" @click="toggleLike">
              <i class="ti ti-heart" aria-hidden="true"></i> {{ plan.likeCount }}
            </button>
            <span class="view-stat"><i class="ti ti-eye" aria-hidden="true"></i> {{ formatCount(plan.viewCount)
            }}</span>
            <button class="report-btn" title="신고하기" @click="showReportModal = true">
              <i class="ti ti-flag" aria-hidden="true"></i>
            </button>
            <button class="import-btn" @click="showImportModal = true">전체 일정 가져오기</button>
          </div>
        </div>

        <div class="detail-body">
          <div class="day-sidebar">
            <button v-for="day in plan.days" :key="day.dayNumber" class="day-item"
              :class="{ active: selectedDay === day.dayNumber }" @click="selectedDay = day.dayNumber">
              <div class="day-num">DAY {{ day.dayNumber }}</div>
              <div class="day-date">{{ day.dateLabel }}</div>
            </button>
          </div>

          <div class="day-content">
            <div class="day-content-head">
              <span class="day-content-num">DAY {{ currentDay.dayNumber }}</span>
              <span class="day-content-date">{{ currentDay.fullDateLabel }}</span>
            </div>

            <div v-for="section in groupedPlaces" :key="section.timeSlot" class="time-section"
              :class="section.timeSlot === '오전' ? 'time-section--morning' : 'time-section--afternoon'">
              <div class="time-section-head">
                <span class="time-section-label">{{ section.timeSlot }}</span>
                <span class="time-section-count">{{ section.places.length }}곳</span>
              </div>
              <div class="place-list">
                <div v-for="place in section.places" :key="place.id" class="place-row">
                  <div class="place-bar"></div>
                  <div class="place-card">
                    <div class="place-name">{{ place.name }}</div>
                    <div class="place-desc">{{ place.description }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="day-map">
            <div class="map-head">
              <span>DAY {{ currentDay.dayNumber }} 동선</span>
              <span class="map-count">장소 {{ currentDay.places.length }}곳</span>
            </div>
            <div class="map-canvas">
              <KakaoMap :places="currentDay.places" />
            </div>

            <div class="day-summary">
              <div class="summary-row">
                <span class="summary-label">오늘 방문 장소</span>
                <span class="summary-value">{{ currentDay.places.length }}곳</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">오전 · 오후</span>
                <span class="summary-value">{{ morningCount }}곳 · {{ afternoonCount }}곳</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">여행 진행</span>
                <span class="summary-value">{{ plan.days.length }}일 중 {{ currentDay.dayNumber }}일차</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <ReportModal v-if="showReportModal" @close="showReportModal = false" @submit="handleReportSubmit" />
      <ImportModal v-if="showImportModal" :plan="plan" @close="showImportModal = false" />
    </div>
  </DefaultLayout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import ReportModal from './ReportModal.vue'
import ImportModal from './ImportModal.vue'
import KakaoMap from './KakaoMap.vue'

// 라우터가 plan-detail 경로(/plans/:id)에 props: true로 연결돼 있어 id를 prop으로 받는다.
// TODO: 백엔드 연동 시 이 id로 GET /plans/{id}를 호출해 plan 데이터를 채운다.
const props = defineProps({
  id: { type: [String, Number], required: false, default: null },
})

const router = useRouter()

function goBack() {
  // 브라우저 히스토리를 한 칸 뒤로 이동한다.
  // 탐색 페이지가 router.replace로 검색 상태(keyword, count)를 URL 쿼리에 반영해두었기 때문에
  // 뒤로가기만으로 검색 결과가 그대로 복원되고,
  // router의 scrollBehavior(savedPosition)가 스크롤 위치도 함께 복원해준다.
  router.back()
}

// ── mock 데이터: 백엔드 연동 시 props.id로 GET /plans/{id}를 호출하는 형태로 교체 ──
// DAY 2에는 장소를 많이 넣어서 "일정 카드가 넘칠 때" 스크롤 동작을 확인할 수 있게 했고,
// DAY도 8개까지 늘려서 "사이드바가 넘칠 때" 스크롤 동작을 확인할 수 있게 했다.
const plan = ref({
  id: props.id ?? 101,
  title: '부산 여행',
  authorName: '김민준',
  periodLabel: '07.20 - 07.27 (8일)',
  likeCount: 128,
  viewCount: 1200,
  liked: false,
  days: [
    {
      dayNumber: 1, dateLabel: '7/20(토)', fullDateLabel: '7월 20일 (토)',
      places: [
        { id: 1, timeSlot: '오전', name: '부산역 도착', description: 'KTX로 부산역 도착 후 짐 보관', lat: 35.1152, lng: 129.0415 },
        { id: 2, timeSlot: '오후', name: '자갈치시장', description: '싱싱한 회와 해산물, 부산의 대표 재래시장', lat: 35.0968, lng: 129.0306 },
      ],
    },
    {
      dayNumber: 2, dateLabel: '7/21(일)', fullDateLabel: '7월 21일 (일)',
      places: [
        { id: 3, timeSlot: '오전', name: '해운대해수욕장', description: '부산 대표 해변, 산책과 구경을 즐길 수 있는 곳', lat: 35.1587, lng: 129.1604 },
        { id: 4, timeSlot: '오전', name: '해운대 블루라인파크', description: '해안선을 따라 달리는 스카이캡슐과 해변열차', lat: 35.1569, lng: 129.1904 },
        { id: 5, timeSlot: '오후', name: '해운대암소갈비집', description: '30년 전통의 암소갈비, 부드럽고 진한 맛', lat: 35.1631, lng: 129.1636 },
        { id: 6, timeSlot: '오후', name: '더베이101', description: '요트가 정박된 마리나에서 즐기는 노을과 야경', lat: 35.1584, lng: 129.1512 },
        { id: 7, timeSlot: '오후', name: '동백섬 누리마루', description: 'APEC 정상회의가 열렸던 전망대, 산책로가 아름다움', lat: 35.1533, lng: 129.1560 },
        { id: 8, timeSlot: '오후', name: '광안리 포장마차촌', description: '광안대교 야경을 보며 즐기는 해산물 포장마차', lat: 35.1533, lng: 129.1186 },
        { id: 9, timeSlot: '오후', name: 'SUP 야간 체험', description: '광안리 앞바다에서 즐기는 야간 패들보드 투어', lat: 35.1531, lng: 129.1187 },
      ],
    },
    {
      dayNumber: 3, dateLabel: '7/22(월)', fullDateLabel: '7월 22일 (월)',
      places: [
        { id: 10, timeSlot: '오전', name: '감천문화마을', description: '알록달록 계단식 마을, 부산의 산토리니', lat: 35.0975, lng: 129.0106 },
        { id: 11, timeSlot: '오후', name: '광안리해수욕장', description: '광안대교 야경이 아름다운 해변', lat: 35.1532, lng: 129.1187 },
      ],
    },
    {
      dayNumber: 4, dateLabel: '7/23(화)', fullDateLabel: '7월 23일 (화)',
      places: [
        { id: 12, timeSlot: '오전', name: '태종대', description: '기암절벽과 등대, 부산 대표 자연경관', lat: 35.0511, lng: 129.0868 },
      ],
    },
    {
      dayNumber: 5, dateLabel: '7/24(수)', fullDateLabel: '7월 24일 (수)',
      places: [
        { id: 13, timeSlot: '오전', name: '용궁구름다리', description: '바다 위를 걷는 스릴 만점 출렁다리', lat: 35.1783, lng: 129.2223 },
        { id: 14, timeSlot: '오후', name: '흰여울문화마을', description: '영화 촬영지로 유명한 절벽 마을', lat: 35.0765, lng: 129.0334 },
      ],
    },
    {
      dayNumber: 6, dateLabel: '7/25(목)', fullDateLabel: '7월 25일 (목)',
      places: [
        { id: 15, timeSlot: '오전', name: '송정해수욕장', description: '서핑으로 유명한 조용한 해변', lat: 35.1786, lng: 129.2003 },
        { id: 16, timeSlot: '오후', name: '기장 대변항', description: '멸치회로 유명한 어촌 포구', lat: 35.2280, lng: 129.2264 },
      ],
    },
    {
      dayNumber: 7, dateLabel: '7/26(금)', fullDateLabel: '7월 26일 (금)',
      places: [
        { id: 17, timeSlot: '오전', name: '을숙도 생태공원', description: '낙동강 하구의 철새 도래지', lat: 35.0983, lng: 128.9317 },
        { id: 18, timeSlot: '오후', name: '몰운대', description: '낙동강과 바다가 만나는 절경', lat: 35.0508, lng: 128.9666 },
      ],
    },
    {
      dayNumber: 8, dateLabel: '7/27(토)', fullDateLabel: '7월 27일 (토)',
      places: [
        { id: 19, timeSlot: '오전', name: '부산역 귀가', description: '기념품 구매 후 KTX 탑승', lat: 35.1152, lng: 129.0415 },
      ],
    },
  ],
})

const selectedDay = ref(1)
const currentDay = computed(() => plan.value.days.find((d) => d.dayNumber === selectedDay.value))

// DAY 요약 패널용 계산: 오전/오후 개수
const morningCount = computed(() => currentDay.value.places.filter((p) => p.timeSlot === '오전').length)
const afternoonCount = computed(() => currentDay.value.places.filter((p) => p.timeSlot === '오후').length)

// 장소를 시간대(오전/오후)별로 묶어서 섹션 단위로 보여주기 위한 계산.
// 카드마다 라벨을 반복해서 붙이는 대신, 같은 시간대는 헤더 하나 아래로 묶는다.
// 순서는 항상 오전 → 오후로 고정하고, 해당 시간대에 장소가 없으면 섹션 자체를 표시하지 않는다.
const groupedPlaces = computed(() => {
  const order = ['오전', '오후']
  return order
    .map((timeSlot) => ({
      timeSlot,
      places: currentDay.value.places.filter((p) => p.timeSlot === timeSlot),
    }))
    .filter((section) => section.places.length > 0)
})

const showReportModal = ref(false)
const showImportModal = ref(false)

function toggleLike() {
  plan.value.liked = !plan.value.liked
  plan.value.likeCount += plan.value.liked ? 1 : -1
}

function handleReportSubmit(payload) {
  // TODO: 백엔드 연동 시 여기서 API 호출 (신고 접수)
  // 모달을 닫는 건 ReportModal 내부의 완료 화면 → '확인' 버튼(close 이벤트)이 담당하므로
  // 여기서는 showReportModal을 false로 바꾸지 않는다.
  console.log('신고 접수:', payload)
}

function formatCount(n) {
  if (n >= 1000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'k'
  return String(n)
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.detail-page {
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
  background: #f7f6f4;
  padding: 2rem 3rem;
  display: flex;
  align-items: flex-start;
}

.detail-card {
  max-width: 1280px;
  width: 100%;
  /* 100vh를 그대로 쓰면 DefaultLayout의 헤더/푸터까지 더해져 전체 페이지가
     뷰포트보다 길어진다. 고정 픽셀 높이로 바꿔 "한눈에 보이는" 크기로 줄이고,
     DAY 목록이나 장소가 넘칠 때는 이 안에서만 스크롤되게 한다. */
  height: 640px;
  margin: 0 auto;
  background: #fff;
  border-radius: 20px;
  padding: 2rem 2.5rem;
  display: flex;
  flex-direction: column;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 1.5rem;
  flex-shrink: 0;
}

/* 뒤로가기는 텍스트 없이 아이콘만 보여주는 원형 버튼으로 컴팩트하게 */
.back-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  font-size: 18px;
  color: #999;
  background: #fafafa;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: background .15s, color .15s;
}

.back-link:hover {
  background: #f0f0f0;
  color: #1a1a1a;
}

.plan-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  flex-shrink: 0;
  white-space: nowrap;
}

/* 작성자/날짜 정보를 제목 옆에 나란히 배치. margin-left는 주지 않고 gap으로만 간격을 준다. */
.plan-author {
  font-size: 13px;
  color: #0f766e;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.author-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0f766e;
  flex-shrink: 0;
}

/* 좋아요/조회수/신고/가져오기 버튼 그룹을 오른쪽 끝으로 밀어낸다 */
.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  margin-left: auto;
}

.like-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #999;
  background: none;
  border: none;
  cursor: pointer;
  transition: color .15s;
}

.like-stat.liked {
  color: #0f766e;
}

.view-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #bbb;
}

.report-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #eee;
  background: #fff;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
}

.report-btn:hover {
  border-color: #0f766e;
  color: #0f766e;
}

.import-btn {
  padding: 9px 18px;
  background: #0f766e;
  color: #fff;
  border-radius: 20px;
  font-size: 13.5px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}

.import-btn:hover {
  background: #0c5c56;
}

.detail-body {
  display: grid;
  grid-template-columns: 180px 1fr 320px;
  gap: 20px;
  /* detail-card가 이제 고정 height를 가지므로, 헤더(detail-head)를 제외한
     나머지 공간을 정확히 차지한다. min-height: 0은 grid 자식이 내용 크기만큼
     늘어나 버리는(overflow가 무시되는) 문제를 막기 위해 필요하다. */
  flex: 1;
  min-height: 0;
}

.day-sidebar {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
  padding-right: 4px;
  min-height: 0;
}

/* DAY가 많아져 사이드바가 넘칠 때를 위한 스크롤바 스타일 */
.day-sidebar::-webkit-scrollbar {
  width: 6px;
}

.day-sidebar::-webkit-scrollbar-thumb {
  background: #e0e0e0;
  border-radius: 3px;
}

.day-sidebar::-webkit-scrollbar-thumb:hover {
  background: #ccc;
}

.day-item {
  text-align: left;
  padding: 14px 16px;
  border-radius: 10px;
  border: none;
  background: #fafafa;
  cursor: pointer;
  transition: all .15s;
  flex-shrink: 0;
}

.day-item.active {
  background: #d9f0ed;
}

.day-num {
  font-size: 15px;
  font-weight: 700;
  color: #888;
  margin-bottom: 3px;
}

.day-item.active .day-num {
  color: #0f766e;
}

.day-date {
  font-size: 12px;
  color: #bbb;
}

.day-content {
  background: #fafafa;
  border-radius: 14px;
  padding: 24px;
  overflow-y: auto;
  min-height: 0;
}

/* 장소가 많아져 DAY 콘텐츠가 넘칠 때를 위한 스크롤바 스타일 */
.day-content::-webkit-scrollbar {
  width: 6px;
}

.day-content::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.day-content::-webkit-scrollbar-thumb:hover {
  background: #c5c5c5;
}

.day-content-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  position: sticky;
  top: -24px;
  margin: -24px -24px 20px;
  padding: 24px 24px 14px;
  background: #fafafa;
  z-index: 10;
}

.day-content-num {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
}

.day-content-date {
  font-size: 14px;
  color: #bbb;
}

.time-section {
  margin-bottom: 24px;
}

.time-section:last-child {
  margin-bottom: 0;
}

/* 오전은 레드, 오후는 블루 계열로 구분. 라벨 텍스트 색과 카드 왼쪽 컬러바에 같은 색을 사용한다.
   두 색 모두 헤더 로고(연필의 주황, 핀의 파랑)에서 뽑은 톤이라 사이트 전체 톤과 어울린다. */
.time-section--morning {
  --slot-color: #FB633C;
  --slot-color-dark: #AB4329;
}

.time-section--afternoon {
  --slot-color: #0082FC;
  --slot-color-dark: #0058AB;
}

.time-section-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}

.time-section-label {
  font-size: 15px;
  font-weight: 700;
  color: var(--slot-color-dark);
}

.time-section-count {
  font-size: 12px;
  color: #bbb;
}

.place-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.place-row {
  display: flex;
  gap: 10px;
}

.place-bar {
  width: 3px;
  border-radius: 0;
  background: var(--slot-color);
  flex-shrink: 0;
}

.place-card {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  min-width: 0;
}

.place-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 5px;
}

.place-desc {
  font-size: 13.5px;
  color: #999;
  line-height: 1.5;
}

.day-map {
  background: #fafafa;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
}

.day-map::-webkit-scrollbar {
  width: 6px;
}

.day-map::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

.day-map::-webkit-scrollbar-thumb:hover {
  background: #c5c5c5;
}

.map-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.map-count {
  font-size: 12px;
  color: #bbb;
  font-weight: 400;
}

/* 장소 수와 무관하게 지도는 항상 일정한 비율(정사각형에 가까운 형태)로 고정한다.
   이렇게 해야 장소가 1개뿐인 DAY에서도 지도 영역이 어색하게 길어지지 않는다. */
.map-canvas {
  flex-shrink: 0;
  aspect-ratio: 200 / 220;
  border-radius: 10px;
  overflow: hidden;
}

.day-summary {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-label {
  font-size: 12.5px;
  color: #999;
}

.summary-value {
  font-size: 12.5px;
  color: #1a1a1a;
  font-weight: 600;
}
</style>