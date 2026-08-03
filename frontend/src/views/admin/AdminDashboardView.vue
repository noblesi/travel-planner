<script setup>
import { useRouter } from 'vue-router'

import newUserIcon from '@/assets/icons/admin/newmember_icon.png'
import peopleIcon from '@/assets/icons/admin/member_icon.png'
import planIcon from '@/assets/icons/admin/plan_icon.png'
import reportIcon from '@/assets/icons/admin/report_icon.png'

const router = useRouter()

// 대시보드 통계 API 응답으로 교체할 요약 카드 데이터입니다.
const summaryCards = [
  { label: '전체 회원', value: '3,000', image: peopleIcon, imageClass: 'summary-image--people', caption: '전월 대비 12% 증가', tone: 'positive', route: { name: 'admin-members' } },
  { label: '신규 가입자 수', value: '15', image: newUserIcon, caption: '오늘 신규 가입', tone: 'positive', route: { name: 'admin-members' } },
  { label: '공개 여행 플랜', value: '200', image: planIcon, caption: '이번 달 42개 등록', tone: 'neutral', route: { name: 'admin-trips' } },
  { label: '신고 검토 대기', value: '13', image: reportIcon, caption: '확인이 필요합니다', tone: 'warning', route: { name: 'admin-trips', query: { tab: 'reported' } } },
]

// 최근 7일 플랜 생성 통계 API 응답으로 교체합니다.
const weeklyPlans = [
  { day: '월', value: 50 },
  { day: '화', value: 38 },
  { day: '수', value: 101 },
  { day: '목', value: 24 },
  { day: '금', value: 20 },
  { day: '토', value: 11 },
  { day: '일', value: 28 },
]

// 지역별 공개 여행 플랜 수를 많은 순서대로 표시합니다.
const popularRegions = [
  { rank: 1, name: '제주', count: 142, percentage: 100 },
  { rank: 2, name: '서울', count: 118, percentage: 83 },
  { rank: 3, name: '부산', count: 96, percentage: 68 },
  { rank: 4, name: '강원', count: 72, percentage: 51 },
  { rank: 5, name: '경주', count: 51, percentage: 36 },
]
</script>

<template>
  <section class="dashboard">
    <div class="dashboard-content">
      <header class="page-header">
        <h1>대시보드</h1>
        <p>WithTrip 서비스의 주요 운영 현황을 확인합니다.</p>
      </header>

      <div class="summary-grid">
        <article
          v-for="card in summaryCards"
          :key="card.label"
          class="summary-card"
          role="link"
          tabindex="0"
          @click="router.push(card.route)"
          @keydown.enter="router.push(card.route)"
        >
          <div>
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <p :class="['summary-caption', `summary-caption--${card.tone}`]">
              {{ card.caption }}
            </p>
          </div>

          <div class="summary-icon">
            <img v-if="card.image" :class="card.imageClass" :src="card.image" alt="" />
            <span v-else>{{ card.icon }}</span>
          </div>
        </article>
      </div>

      <div class="detail-grid">
        <article class="panel">
          <header class="panel-header">
            <div>
              <h2>최근 7일 여행 플랜 생성 추이</h2>
              <p>일별로 새롭게 생성된 플랜입니다.</p>
            </div>
            <span class="panel-unit">단위: 개</span>
          </header>

          <div class="chart">
            <div
              v-for="item in weeklyPlans"
              :key="item.day"
              class="chart-column"
            >
              <div class="bar-area">
                <span class="bar-value">{{ item.value }}</span>

                <div
                  class="bar"
                  :style="{ height: `${Math.max(item.value, 8) * 2}px` }"
                />
              </div>

              <span class="bar-label">{{ item.day }}</span>
            </div>
          </div>
        </article>

        <article class="panel region-panel">
          <header class="panel-header">
            <div>
              <h2>인기 여행 지역 TOP 5</h2>
              <p>공개 여행 플랜이 많이 등록된 지역입니다.</p>
            </div>
            <span class="panel-unit">단위: 개</span>
          </header>

          <ol class="region-list">
            <li
              v-for="region in popularRegions"
              :key="region.name"
              class="region-item"
            >
              <span class="region-rank">{{ region.rank }}</span>
              <strong class="region-name">{{ region.name }}</strong>

              <div class="region-progress" aria-hidden="true">
                <span :style="{ width: `${region.percentage}%` }" />
              </div>

              <strong class="region-count">{{ region.count }}</strong>
            </li>
          </ol>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.dashboard {
  min-height: 100%;
  background: var(--admin-page-bg);
  color: var(--admin-text);
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 30px;
  border-bottom: 1px solid #e7e9ed;
  background: #ffffff;
  color: #555b64;
  font-size: 12px;
}

.dashboard-content {
  padding: 0;
}

.page-header {
  margin-bottom: 26px;
}

.page-header h1 {
  margin: 0;
  font-size: 34px;
  letter-spacing: -1px;
}

.page-header p {
  margin: 8px 0 0;
  color: #9297a0;
  font-size: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.summary-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 132px;
  padding: 22px 24px;
  border: 1px solid var(--admin-border);
  border-radius: 5px;
  background: var(--admin-surface);
  box-shadow: 0 2px 7px rgb(31 41 55 / 4%);
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease;
}

.summary-card:hover { box-shadow: 0 10px 26px rgb(37 42 49 / 10%); transform: translateY(-2px); }
.summary-card:focus-visible { outline: 3px solid rgb(243 136 59 / 24%); outline-offset: 3px; }

.summary-card span {
  color: #9499a2;
  font-size: 14px;
  font-weight: 600;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 32px;
}

.summary-caption {
  margin: 8px 0 0;
  font-size: 12px;
  font-weight: 600;
}

.summary-caption--positive {
  color: #16966a;
}

.summary-caption--neutral {
  color: #6f7782;
}

.summary-caption--warning {
  color: #f06b3c;
}

.summary-icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 5px;
  background: var(--admin-orange-soft);
  color: var(--admin-orange);
  font-size: 17px;
  font-weight: 900;
}

.summary-icon img {
  display: block;
  width: 30px;
  height: 30px;
  object-fit: contain;
  object-position: center;
}

.summary-icon .summary-image--people {
  width: 34px;
  height: 34px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(340px, 1fr);
  gap: 28px;
  margin-top: 34px;
}

.panel {
  min-height: 430px;
  padding: 26px;
  border: 1px solid var(--admin-border);
  border-radius: 5px;
  background: var(--admin-surface);
}

.panel h2 {
  margin: 0;
  font-size: 17px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.panel-header p {
  margin: 8px 0 0;
  color: #9399a2;
  font-size: 13px;
}

.panel-unit {
  flex-shrink: 0;
  color: #9399a2;
  font-size: 12px;
}

.chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 320px;
  padding-top: 35px;
  border-bottom: 1px solid #e8ebef;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0,
    transparent 59px,
    #eef0f3 60px
  );
}

.chart-column {
  display: flex;
  flex: 1;
  align-items: center;
  flex-direction: column;
  justify-content: flex-end;
  height: 100%;
}

.bar-area {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: flex-end;
  height: 230px;
}

.bar-value {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
}

.bar {
  width: 42px;
  max-height: 210px;
  background: var(--admin-orange);
}

.bar-label {
  margin-top: 10px;
  color: #8f949c;
  font-size: 13px;
}

.region-list {
  display: grid;
  gap: 24px;
  margin: 34px 0 0;
  padding: 0;
  list-style: none;
}

.region-item {
  display: grid;
  grid-template-columns: 28px 54px minmax(100px, 1fr) 42px;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.region-rank {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border-radius: 50%;
  background: var(--admin-orange-soft);
  color: var(--admin-orange);
  font-size: 12px;
  font-weight: 800;
}

.region-name {
  font-size: 14px;
}

.region-progress {
  height: 14px;
  overflow: hidden;
  border-radius: 20px;
  background: #f1ebe7;
}

.region-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--admin-orange);
}

.region-count {
  text-align: right;
  font-size: 14px;
}

.region-panel {
  display: flex;
  flex-direction: column;
}

.region-panel .panel-header {
  flex: 0 0 auto;
}

.region-list {
  flex: 1;
  align-content: center;
}

@media (max-width: 480px) {
  .region-item {
    grid-template-columns: 28px 46px minmax(80px, 1fr) 36px;
    gap: 8px;
  }
}

/*
 * 추천 점수 규칙은 여행 플랜 관리 화면의 모달로 이동할 예정입니다.
 * 대시보드에서는 운영 현황을 보여주는 인기 지역 통계만 표시합니다.
 */

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 650px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-content {
    padding: 0;
  }
}
</style>
