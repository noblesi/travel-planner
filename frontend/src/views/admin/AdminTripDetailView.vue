<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tripId = computed(() => route.params.tripId)

const schedules = [
  { order: 1, name: '경복궁', time: '09:30', detail: '서촌 여행 시작 장소' },
  { order: 2, name: '토속촌 삼계탕', time: '12:30', detail: '점심 / 예상 비용 15,000원' },
  { order: 3, name: '창덕궁', time: '14:30', detail: '고궁 관람' },
]

const reports = [
  { date: '2026-07-13', reason: '과도한 광고', status: '조치' },
  { date: '2026-07-16', reason: '불법 생성', status: '상태' },
]

const metrics = [
  { label: '좋아요', value: '1,067' },
  { label: '저장', value: '60' },
  { label: '조회', value: '74' },
]

const scores = [
  { label: '좋아요', value: 92 },
  { label: '조회수', value: 28 },
  { label: '저장수', value: 76 },
]
</script>

<template>
  <section class="trip-detail-page">
    <header class="page-header">
      <div>
        <h1>여행 플랜 상세</h1>
        <p>공개 플랜의 일정, 방문 지점과 신고 이력을 확인합니다.</p>
      </div>
      <button class="list-button" type="button" @click="router.push({ name: 'admin-trips' })">목록으로</button>
    </header>

    <article class="hero-card">
      <span>공개</span>
      <h2>서울 궁궐 여행</h2>
      <p>작성자 김민수 / 2026-08-16 ~ 2026-08-17 / 참여자 3명</p>
      <small>{{ tripId }}</small>
    </article>

    <div class="top-grid">
      <article class="panel schedule-panel">
        <div class="panel-heading"><h2>날짜별 여행 일정</h2><span>1일차</span></div>
        <div class="schedule-list">
          <div v-for="schedule in schedules" :key="schedule.order" class="schedule-item">
            <b>{{ schedule.order }}</b>
            <div><strong>{{ schedule.name }}</strong><p>{{ schedule.time }} / {{ schedule.detail }}</p></div>
          </div>
        </div>
        <button class="more-button" type="button">+ 더보기</button>
      </article>

      <div class="side-stack">
        <article class="panel map-panel">
          <h2>일정 지도</h2>
          <div class="map-placeholder">
            <span class="pin pin--one">●</span><span class="pin pin--two">●</span><span class="pin pin--three">●</span>
            <strong>지도 영역</strong>
          </div>
        </article>
        <article class="panel metrics-panel">
          <h2>콘텐츠 지표</h2>
          <div class="metric-list">
            <div v-for="metric in metrics" :key="metric.label"><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong></div>
          </div>
        </article>
      </div>
    </div>

    <div class="bottom-grid">
      <article class="panel report-panel">
        <div class="panel-heading"><h2>신고 및 운영 기록</h2><span class="report-count">신고 2건</span></div>
        <div v-for="report in reports" :key="report.date" class="report-row">
          <span>{{ report.date }}</span><strong>{{ report.reason }}</strong><em>{{ report.status }}</em>
        </div>
      </article>
      <article class="panel score-panel">
        <h2>추천 점수 상세</h2>
        <div v-for="score in scores" :key="score.label" class="score-row">
          <span>{{ score.label }}</span><div><i :style="{ width: `${score.value}%` }" /></div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.trip-detail-page { min-height: 100%; color: #292d33; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 24px; }
.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }
.page-header p { margin: 9px 0 0; color: #8c929c; font-size: 14px; }
.list-button { height: 38px; padding: 0 15px; border: 1px solid #9198a1; border-radius: 5px; background: #fff; color: #565c65; font-size: 13px; font-weight: 700; cursor: pointer; }
.hero-card { position: relative; padding: 22px 24px; border-radius: 7px; background: linear-gradient(135deg, #ef936c, #f2d45e); color: #fff; }
.hero-card > span { display: inline-flex; padding: 4px 8px; border-radius: 16px; background: #fff; color: #55a8d8; font-size: 11px; font-weight: 800; }
.hero-card h2 { margin: 9px 0; font-size: 25px; }.hero-card p { margin: 0; font-size: 12px; font-weight: 700; }.hero-card small { position: absolute; top: 20px; right: 22px; opacity: .8; }
.top-grid, .bottom-grid { display: grid; grid-template-columns: minmax(0, 1.65fr) minmax(300px, 1fr); gap: 28px; margin-top: 26px; }
.panel { padding: 20px 22px; border: 1px solid #dfe3e8; border-radius: 6px; background: #fff; box-shadow: 0 3px 12px rgb(31 41 55 / 4%); }.panel h2 { margin: 0; font-size: 17px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; }.panel-heading > span { padding: 5px 9px; border-radius: 5px; background: #ff712a; color: #fff; font-size: 11px; font-weight: 800; }
.schedule-list { display: grid; gap: 10px; margin-top: 18px; }.schedule-item { display: flex; align-items: center; gap: 12px; padding: 11px 13px; border: 1px solid #afb5bd; border-radius: 5px; }.schedule-item b { display: grid; width: 25px; height: 25px; place-items: center; border-radius: 50%; background: #ff6c20; color: #fff; }.schedule-item strong { font-size: 13px; }.schedule-item p { margin: 4px 0 0; color: #8e949d; font-size: 11px; }.more-button { display: block; margin: 12px auto 0; padding: 7px 16px; border: 0; border-radius: 4px; background: #292d33; color: #fff; cursor: pointer; }
.side-stack { display: grid; gap: 16px; }.map-panel { padding-bottom: 16px; }.map-placeholder { position: relative; display: grid; height: 145px; margin-top: 14px; overflow: hidden; place-items: center; background: repeating-linear-gradient(135deg, #2e2d4d 0 18px, #44425f 19px 22px); color: rgb(255 255 255 / 35%); }.pin { position: absolute; color: #6170ff; font-size: 22px; }.pin--one { top: 20px; left: 14%; }.pin--two { top: 72px; left: 30%; }.pin--three { top: 63px; right: 17%; }
.metric-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-top: 14px; }.metric-list > div { padding: 10px 12px; border: 1px solid #f09a79; border-radius: 10px; background: #fff1eb; }.metric-list span, .metric-list strong { display: block; }.metric-list span { color: #9298a0; font-size: 11px; }.metric-list strong { margin-top: 4px; font-size: 17px; }
.bottom-grid { margin-bottom: 10px; }.report-row { display: grid; grid-template-columns: 110px 1fr auto; gap: 16px; padding: 15px 0; border-bottom: 1px solid #e2e5e9; font-size: 12px; }.report-row > span { color: #959ba4; }.report-row em { padding: 4px 8px; border-radius: 14px; background: #eef4ff; color: #5793c1; font-style: normal; }.report-count { background: #e8f8ed !important; color: #3c9b66 !important; }
.score-panel { display: grid; align-content: start; gap: 17px; }.score-row { display: grid; grid-template-columns: 60px 1fr; align-items: center; gap: 10px; font-size: 12px; }.score-row div { height: 18px; background: #f1f2f4; }.score-row i { display: block; height: 100%; background: #ed8d68; }
@media (max-width: 1000px) { .top-grid, .bottom-grid { grid-template-columns: 1fr; } }
@media (max-width: 650px) { .page-header { align-items: flex-start; flex-direction: column; }.metric-list { grid-template-columns: 1fr; }.report-row { grid-template-columns: 1fr; gap: 5px; } }
</style>
