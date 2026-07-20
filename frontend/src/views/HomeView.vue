<script setup>
import { onMounted, ref } from 'vue'

import { getHealth } from '@/api/system'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const serverStatus = ref('확인 중')
const serverConnected = ref(false)

onMounted(async () => {
  try {
    const health = await getHealth()
    serverConnected.value = health.status === 'UP'
    serverStatus.value = serverConnected.value ? '연결됨' : '점검 필요'
  } catch {
    serverStatus.value = '백엔드 실행 필요'
  }
})
</script>

<template>
  <DefaultLayout>
    <section class="hero">
      <div class="hero__content">
        <p class="eyebrow">PLAN TOGETHER, TRAVEL BETTER</p>
        <h1>여행의 모든 순간을<br />함께 계획하세요.</h1>
        <p class="hero__description">
          일정을 만들고, 가고 싶은 장소를 담고, 동선을 한눈에 정리하는 여행 플래너입니다.
        </p>
        <div class="hero__actions">
          <button class="primary-action" type="button">새 일정 만들기</button>
          <a class="secondary-action" href="#explore">인기 일정 둘러보기</a>
        </div>
      </div>

      <div class="status-card" aria-live="polite">
        <span :class="['status-card__dot', { 'status-card__dot--connected': serverConnected }]" />
        <div>
          <strong>개발 환경 상태</strong>
          <p>Spring Boot API: {{ serverStatus }}</p>
        </div>
      </div>
    </section>

    <section id="explore" class="feature-section">
      <article>
        <span>01</span>
        <h2>일정 구성</h2>
        <p>날짜별 여행 장소와 시간을 간편하게 정리합니다.</p>
      </article>
      <article>
        <span>02</span>
        <h2>동선 확인</h2>
        <p>방문 장소의 순서와 이동 계획을 한눈에 확인합니다.</p>
      </article>
      <article>
        <span>03</span>
        <h2>함께 계획</h2>
        <p>팀원과 여행 일정을 공유하고 의견을 모읍니다.</p>
      </article>
    </section>
  </DefaultLayout>
</template>

<style scoped>
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr);
  gap: 64px;
  align-items: center;
  width: min(1180px, calc(100% - 40px));
  min-height: 560px;
  margin: 0 auto;
  padding: 80px 0;
}

.eyebrow {
  margin: 0 0 18px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.15em;
}

h1 {
  margin: 0;
  color: #0f172a;
  font-size: clamp(42px, 6vw, 72px);
  line-height: 1.12;
  letter-spacing: -0.045em;
}

.hero__description {
  max-width: 590px;
  margin: 28px 0 0;
  color: #64748b;
  font-size: 18px;
  line-height: 1.75;
}

.hero__actions {
  display: flex;
  gap: 14px;
  margin-top: 36px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 50px;
  padding: 0 22px;
  border-radius: 12px;
  font-weight: 750;
}

.primary-action {
  color: white;
  border: 1px solid #0f766e;
  background: #0f766e;
  cursor: pointer;
}

.secondary-action {
  border: 1px solid #cbd5e1;
  background: white;
}

.status-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 24px;
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background: white;
  box-shadow: 0 24px 70px rgb(15 23 42 / 10%);
}

.status-card__dot {
  width: 11px;
  height: 11px;
  margin-top: 5px;
  border-radius: 50%;
  background: #f59e0b;
}

.status-card__dot--connected {
  background: #10b981;
}

.status-card p {
  margin: 7px 0 0;
  color: #64748b;
}

.feature-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto 96px;
}

.feature-section article {
  padding: 30px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: white;
}

.feature-section span {
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
}

.feature-section h2 {
  margin: 18px 0 10px;
  font-size: 20px;
}

.feature-section p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 820px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .feature-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .hero__actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
