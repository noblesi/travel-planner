<template>
  <DefaultLayout>
    <div class="detail-page">
      <div v-if="loading" class="app-container detail-status" role="status">
        공개 일정을 불러오는 중이에요.
      </div>
      <div v-else-if="errorMessage" class="app-container detail-status detail-status--error" role="alert">
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadPlan">다시 시도</button>
      </div>
      <div v-else-if="plan" class="app-container detail-card">
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
              :class="{ active: selectedDay === day.dayNumber }" @click="selectDay(day.dayNumber)">
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

      <ReportModal v-if="plan && showReportModal" @close="showReportModal = false" @submit="handleReportSubmit" />
      <ImportModal v-if="plan && showImportModal" :plan="plan" @close="showImportModal = false" />
    </div>
  </DefaultLayout>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPublicTravelPlan } from '@/api/plans'
import KakaoMap from '@/components/map/KakaoMap.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import ReportModal from './ReportModal.vue'
import ImportModal from './ImportModal.vue'

const props = defineProps({
  id: { type: [String, Number], required: false, default: null },
})

const router = useRouter()
const route = useRoute()
const plan = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const selectedDay = ref(1)

function goBack() {
  // 브라우저 히스토리를 한 칸 뒤로 이동한다.
  // 탐색 페이지가 router.replace로 검색 상태(keyword, count)를 URL 쿼리에 반영해두었기 때문에
  // 뒤로가기만으로 검색 결과가 그대로 복원되고,
  // router의 scrollBehavior(savedPosition)가 스크롤 위치도 함께 복원해준다.
  router.back()
}

async function loadPlan() {
  const planId = props.id ?? route.params.id
  loading.value = true
  errorMessage.value = ''
  try {
    const detail = await getPublicTravelPlan(planId)
    plan.value = mapPlanDetail(detail)
    const queryDay = Number(route.query.day)
    selectedDay.value = plan.value.days.some((day) => day.dayNumber === queryDay)
      ? queryDay
      : (plan.value.days[0]?.dayNumber ?? 1)
  } catch {
    plan.value = null
    errorMessage.value = '공개 일정을 찾을 수 없거나 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

function mapPlanDetail(detail) {
  const summary = detail.plan
  return {
    id: summary.planId,
    title: summary.title,
    authorName: summary.authorName,
    periodLabel: `${formatPeriodDate(summary.startDate)} - ${formatPeriodDate(summary.endDate)} (${summary.dayCount}일)`,
    likeCount: summary.likeCount,
    viewCount: summary.viewCount,
    liked: false,
    days: detail.days.map((day) => ({
      dayNumber: day.dayNo,
      dateLabel: formatShortDate(day.travelDate),
      fullDateLabel: formatFullDate(day.travelDate),
      places: day.items.map((item) => ({
        id: item.scheduleItemId,
        timeSlot: item.timeSlot === 'MORNING' ? '오전' : '오후',
        name: item.placeName,
        description: item.description || item.address || item.categoryName || '장소 설명이 없습니다.',
        lat: item.latitude == null ? null : Number(item.latitude),
        lng: item.longitude == null ? null : Number(item.longitude),
      })),
    })),
  }
}

function parseLocalDate(value) {
  return new Date(`${value}T00:00:00`)
}

function formatPeriodDate(value) {
  const date = parseLocalDate(value)
  return `${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

function formatShortDate(value) {
  const date = parseLocalDate(value)
  const weekday = new Intl.DateTimeFormat('ko-KR', { weekday: 'short' }).format(date)
  return `${date.getMonth() + 1}/${date.getDate()}(${weekday.replace('요일', '')})`
}

function formatFullDate(value) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(parseLocalDate(value))
}

const currentDay = computed(() => (
  plan.value?.days.find((day) => day.dayNumber === selectedDay.value)
  ?? { dayNumber: 1, dateLabel: '', fullDateLabel: '', places: [] }
))

function selectDay(dayNumber) {
  selectedDay.value = dayNumber
  // push가 아니라 replace를 쓰는 이유: DAY를 바꿀 때마다 히스토리가 쌓이면
  // 뒤로가기 한 번으로 이전 DAY로 안 돌아가고 탐색 페이지까지 나가버린다.
  router.replace({ query: { ...route.query, day: dayNumber } })
}

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

watch(() => props.id ?? route.params.id, loadPlan, { immediate: true })
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
  min-width: 0;
}

.detail-status {
  min-height: 320px;
  border-radius: 20px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: #777;
}

.detail-status--error {
  flex-direction: column;
  color: #8a4c45;
}

.detail-status button {
  border: 1px solid var(--color-brand-border);
  border-radius: 999px;
  padding: 8px 16px;
  background: #fff;
  color: var(--color-brand);
  cursor: pointer;
}

.detail-card {
  /* 100vh를 그대로 쓰면 DefaultLayout의 헤더/푸터까지 더해져 전체 페이지가
     뷰포트보다 길어진다. 고정 픽셀 높이로 바꿔 "한눈에 보이는" 크기로 줄이고,
     DAY 목록이나 장소가 넘칠 때는 이 안에서만 스크롤되게 한다. */
  height: 640px;
  background: #fff;
  border-radius: 20px;
  padding: 2rem 2.5rem;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 1.5rem;
  flex-shrink: 0;
  min-width: 0;
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
  min-width: 0;
}

/* 작성자/날짜 정보를 제목 옆에 나란히 배치. margin-left는 주지 않고 gap으로만 간격을 준다. */
.plan-author {
  font-size: 13px;
  color: var(--color-brand);
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
  background: var(--color-brand-accent);
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
  color: var(--color-brand);
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
  border-color: var(--color-brand-accent);
  color: var(--color-brand);
}

.import-btn {
  padding: 9px 18px;
  background: var(--color-brand);
  color: var(--color-brand-on);
  border-radius: 20px;
  font-size: 13.5px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  white-space: nowrap;
}

.import-btn:hover {
  background: var(--color-brand-hover);
}

.detail-body {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) 320px;
  gap: 20px;
  /* detail-card가 이제 고정 height를 가지므로, 헤더(detail-head)를 제외한
     나머지 공간을 정확히 차지한다. min-height: 0은 grid 자식이 내용 크기만큼
     늘어나 버리는(overflow가 무시되는) 문제를 막기 위해 필요하다. */
  flex: 1;
  min-height: 0;
  min-width: 0;
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
  background: var(--color-brand-soft);
}

.day-num {
  font-size: 15px;
  font-weight: 700;
  color: #888;
  margin-bottom: 3px;
}

.day-item.active .day-num {
  color: var(--color-brand);
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
  --slot-color: var(--color-brand-accent);
  --slot-color-dark: var(--color-brand);
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

@media (max-width: 760px) {
  .detail-page {
    padding: 16px 12px 24px;
  }

  .detail-card {
    height: auto;
    padding: 20px 16px;
    border-radius: 16px;
  }

  .detail-head {
    display: grid;
    grid-template-columns: 36px minmax(0, 1fr);
    gap: 6px 12px;
    margin-bottom: 20px;
  }

  .back-link {
    grid-row: 1 / span 2;
  }

  .plan-title {
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .plan-author {
    grid-column: 2;
    min-width: 0;
    font-size: 12px;
  }

  .head-actions {
    grid-column: 1 / -1;
    width: 100%;
    margin-left: 0;
    padding-top: 6px;
    gap: 8px;
    flex-wrap: wrap;
  }

  .import-btn {
    margin-left: auto;
    padding: 8px 14px;
  }

  .detail-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .day-sidebar {
    min-height: auto;
    flex-direction: row;
    overflow-x: auto;
    overflow-y: hidden;
    padding: 0 0 8px;
    scroll-snap-type: x proximity;
  }

  .day-sidebar::-webkit-scrollbar {
    width: auto;
    height: 6px;
  }

  .day-item {
    flex: 0 0 104px;
    padding: 12px 14px;
    scroll-snap-align: start;
  }

  .day-content {
    min-height: auto;
    padding: 18px 16px;
    overflow: visible;
  }

  .day-content-head {
    position: static;
    margin: 0 0 16px;
    padding: 0;
  }

  .day-content-num {
    font-size: 18px;
  }

  .place-card {
    padding: 14px;
  }

  .place-name,
  .place-desc {
    overflow-wrap: anywhere;
  }

  .day-map {
    min-height: auto;
    padding: 16px;
    overflow: visible;
  }

  .map-canvas {
    aspect-ratio: 16 / 10;
  }
}

@media (max-width: 360px) {
  .import-btn {
    width: 100%;
    margin-left: 0;
  }
}
</style>
