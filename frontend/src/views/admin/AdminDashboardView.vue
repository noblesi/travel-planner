<script setup>
import { reactive } from 'vue'
import newUserIcon from '@/assets/icons/admin/newmember_icon.png'
import peopleIcon from '@/assets/icons/admin/member_icon.png'
import planIcon from '@/assets/icons/admin/plan_icon.png'
import reportIcon from '@/assets/icons/admin/report_icon.png'

const summaryCards = [
  { label: '전체 회원', value: '3,000', image: peopleIcon, imageClass: 'summary-image--people', caption: '전월 대비 12% 증가', tone: 'positive' },
  { label: '신규 가입자 수', value: '15', image: newUserIcon, caption: '오늘 신규 가입', tone: 'positive' },
  { label: '공개 여행 플랜', value: '200', image: planIcon, caption: '이번 달 42개 등록', tone: 'neutral' },
  { label: '신고 검토 대기', value: '13', image: reportIcon, caption: '확인이 필요합니다', tone: 'warning' },
]

const weeklyPlans = [
  { day: '월', value: 50 },
  { day: '화', value: 38 },
  { day: '수', value: 101 },
  { day: '목', value: 24 },
  { day: '금', value: 20 },
  { day: '토', value: 11 },
  { day: '일', value: 28 },
]

const recommendationWeights = reactive({
  companion: 0,
  preference: 0,
  activity: 0,
  budget: 0,
})

const resetWeights = () => {
  Object.keys(recommendationWeights).forEach((key) => {
    recommendationWeights[key] = 0
  })
}

const saveWeights = () => {
  console.log('추천 점수 규칙 저장', recommendationWeights)
  alert('추천 점수 규칙을 저장했습니다.')
}
</script>

<template>
  <section class="dashboard">
    <div class="dashboard-content">
      <header class="page-header">
        <h1>대시보드</h1>
        <p>서비스 현황과 추천 콘텐츠 노출 규칙을 확인합니다.</p>
      </header>

      <div class="summary-grid">
        <article
          v-for="card in summaryCards"
          :key="card.label"
          class="summary-card"
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

        <article class="panel recommendation-panel">
          <h2>추천 점수 규칙 설정</h2>
          <p>메인 화면 추천 콘텐츠 노출 순위를 계산하는 기준입니다.</p>

          <div class="rule-guide">
            <strong>운영 가이드</strong>
            <span>항목별 비중을 조정해 추천 기준을 설정할 수 있습니다.</span>
          </div>

          <form class="weight-form" @submit.prevent="saveWeights">
            <label>
              <span>좋아요 수</span>
              <span class="input-wrap">
                <input
                  v-model.number="recommendationWeights.companion"
                  type="number"
                  min="0"
                  max="100"
                />
                <small>%</small>
              </span>
            </label>

            <label>
              <span>조회 수</span>
              <span class="input-wrap">
                <input
                  v-model.number="recommendationWeights.preference"
                  type="number"
                  min="0"
                  max="100"
                />
                <small>%</small>
              </span>
            </label>

            <label>
              <span>일정 저장 수</span>
              <span class="input-wrap">
                <input
                  v-model.number="recommendationWeights.activity"
                  type="number"
                  min="0"
                  max="100"
                />
                <small>%</small>
              </span>
            </label>

            <label>
              <span>최신성 점수</span>
              <span class="input-wrap">
                <input
                  v-model.number="recommendationWeights.budget"
                  type="number"
                  min="0"
                  max="100"
                />
                <small>%</small>
              </span>
            </label>

            <div class="form-actions">
              <button
                class="reset-button"
                type="button"
                @click="resetWeights"
              >
                초기화
              </button>

              <button class="save-button" type="submit">
                규칙 저장
              </button>
            </div>
          </form>
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
  background: #f5f7fa;
  color: #20242a;
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
  border: 1px solid #e0e3e8;
  border-radius: 5px;
  background: #ffffff;
  box-shadow: 0 2px 7px rgb(31 41 55 / 4%);
}

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
  background: #d5eeea;
  color: #ff771c;
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
  border: 1px solid #dce0e5;
  border-radius: 5px;
  background: #ffffff;
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
  background: #f6ae28;
}

.bar-label {
  margin-top: 10px;
  color: #8f949c;
  font-size: 13px;
}

.recommendation-panel > p {
  margin: 8px 0 30px;
  color: #9a9ea6;
  font-size: 13px;
}

.rule-guide {
  display: grid;
  gap: 5px;
  margin-bottom: 26px;
  padding: 14px 16px;
  border-radius: 6px;
  background: #fff7f1;
  color: #6f625b;
}

.rule-guide strong {
  color: #e66931;
  font-size: 13px;
}

.rule-guide span {
  font-size: 12px;
  line-height: 1.55;
}

.weight-form {
  display: grid;
  gap: 18px;
}

.weight-form label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
}

.input-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-wrap input {
  width: 70px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid #cfd4da;
  border-radius: 5px;
  outline: none;
}

.input-wrap input:focus {
  border-color: #ff7a32;
}

.input-wrap small {
  width: 12px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 34px;
}

.form-actions button {
  min-width: 92px;
  height: 40px;
  border-radius: 5px;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}

.reset-button {
  border: 1px solid #ff8a65;
  background: #ffffff;
  color: #ff7043;
}

.save-button {
  border: 1px solid #ed926d;
  background: #ed926d;
  color: #ffffff;
}

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
