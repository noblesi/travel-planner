<template>
  <DefaultLayout>
    <div class="detail-page">
    <div class="app-container detail-page__inner">
    <div class="detail-head">
      <span class="cat-badge" :class="'badge-' + (notice.category ?? 'guide')">{{ notice.categoryLabel }}</span>
      <h1 class="detail-title">{{ notice.title }}</h1>
      <div class="detail-meta">
        <span class="meta-item"><i class="ti ti-calendar" aria-hidden="true"></i>{{ notice.createdAt }}</span>
        <span class="meta-divider"></span>
        <span class="meta-item"><i class="ti ti-eye" aria-hidden="true"></i>조회 {{ notice.viewCount }}</span>
      </div>
    </div>

    <div class="detail-divider"></div>

    <div class="detail-body" v-html="notice.content"></div>

    <div class="back-wrap">
      <button class="back-btn" @click="goToList">목록으로</button>
    </div>
    </div>
    </div>
  </DefaultLayout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = useRouter()

// ── mock 데이터: 백엔드 연동 시 API 호출로 교체 ──
const notice = ref({
  id: 2,
  category: 'maintenance',
  categoryLabel: '시스템 점검',
  title: '데이터베이스 서버 이중화 작업을 위한 시스템 정기 점검 안내 (02:00 ~ 06:00)',
  createdAt: '2026.07.10',
  viewCount: 945,
  content: `
    <p>안녕하세요, Wanderlog 팀입니다.</p>
    <p>안정적인 서비스 환경 구축과 데이터 보호 안정성 강화를 위해 데이터베이스 서버 이중화 및 정기 시스템 점검을 진행하고자 합니다.</p>
    <p>점검이 진행되는 동안에는 Wanderlog의 웹사이트 방문 및 모바일 앱을 통한 일정 편집, 조회 등 모든 서비스 이용이 일시적으로 제한되오니 여행 일정을 계획 중이신 유저분들께서는 아래 점검 시간과 영향 범위를 미리 확인하시어 이용에 불편함이 없으시길 바랍니다.</p>
    <div class="notice-box">
      <div class="notice-title">정기 점검 세부 내용</div>
      <dl class="notice-dl">
        <div class="notice-row">
          <dt>점검 일시</dt>
          <dd>2026년 7월 20일 (월) 새벽 02:00 ~ 새벽 06:00 (약 4시간)</dd>
        </div>
        <div class="notice-row">
          <dt>점검 목적</dt>
          <dd>데이터베이스 서버 트래픽 분산 처리를 위한 이중화 인프라 구축 및 전체 시스템 최적화</dd>
        </div>
        <div class="notice-row">
          <dt>작업 영향</dt>
          <dd>점검 시간 동안 Wanderlog 웹 및 앱 서비스 전체 접속 불가능</dd>
        </div>
      </dl>
    </div>
    <p>서버 점검 중에는 실시간 동시 편집 보드나 공유된 지도 동선 정보가 일시적으로 연결되지 않을 수 있습니다. 소중한 여행 데이터의 안전한 반영을 위해 점검이 시작되기 전, 편집 중이던 일정을 안전하게 마무리해 주시기를 권장합니다.</p>
    <p>작업 상황에 따라 점검 시간은 조기 종료되거나 약간 연장될 수 있으며, 변동 사항 발생 시 본 공지사항을 통해 신속하게 다시 안내해 드리겠습니다.</p>
    <p>이용에 불편을 드리는 점 너른 양해 부탁드리며, 더욱 신뢰할 수 있고 쾌적한 Wanderlog 서비스를 만들기 위해 최선을 다하겠습니다.</p>
    <p>감사합니다.<br /><strong>Wanderlog 인프라 운영팀 배상</strong></p>
  `,
})

function goToList() {
  router.push({ name: 'notice-list' })
}
</script>

<style scoped>
* { box-sizing: border-box; }

/* 배경은 이 바깥 래퍼가 뷰포트 전체 폭으로 칠하고, 폭 제한은 안쪽 .detail-page__inner(app-container)가 맡는다.
   합쳐놓으면 app-container의 max-width 바깥으로 body의 --color-page(크림색)가 그대로 보인다. */
/* 홈 화면과 같은 브랜드 글로우를 좌우 여백 곳곳에 비정형적으로 흩뿌린다. */
.detail-page {
  min-height: calc(100vh - var(--layout-header-height));
  padding-block: 2rem;
  background:
    radial-gradient(circle at 4% 8%, rgb(249 115 22 / 8%) 0%, rgb(249 115 22 / 0%) 38%),
    radial-gradient(circle at 97% 22%, rgb(249 115 22 / 6.5%) 0%, rgb(249 115 22 / 0%) 32%),
    radial-gradient(circle at 2% 55%, rgb(249 115 22 / 6%) 0%, rgb(249 115 22 / 0%) 35%),
    radial-gradient(circle at 96% 68%, rgb(249 115 22 / 7%) 0%, rgb(249 115 22 / 0%) 34%),
    radial-gradient(circle at 6% 90%, rgb(249 115 22 / 5%) 0%, rgb(249 115 22 / 0%) 28%),
    var(--color-page);
}

.detail-page__inner {
  /* 본문 글 읽기 영역은 app-container 기본 gutter(20px)보다 넉넉하게 덮어쓴다. */
  padding-inline: clamp(20px, 5vw, 64px);
  padding-block: 3rem;
  /* 한 줄이 너무 길어지면 읽기 어려우므로, 읽기 편한 폭으로 제한하고 가운데 정렬한다. */
  max-width: 900px;
  margin-inline: auto;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .08);
}

.cat-badge {
  display: inline-block; font-size: 12px; font-weight: 600;
  padding: 4px 12px; border-radius: 12px; margin-bottom: 1rem;
}
.badge-guide { background: var(--color-secondary-soft); color: var(--color-secondary); }
.badge-maintenance { background: #fdeee0; color: #96601a; }

.detail-title { font-size: 26px; font-weight: 700; color: #1a1a1a; line-height: 1.4; margin-bottom: .9rem; }
.detail-meta { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #bbb; }
.meta-item { display: flex; align-items: center; gap: 5px; }
.meta-divider { width: 1px; height: 11px; background: #ddd; }

.detail-divider { height: 1px; background: #1a1a1a; margin: 1.5rem 0 2.5rem; }

.detail-body { font-size: 15px; color: #333; line-height: 1.9; }
.detail-body :deep(p) { margin-bottom: 1.1rem; }
.detail-body :deep(.notice-box) {
  background: #fafafa; border-radius: 12px; padding: 1.5rem 1.75rem; margin: 1.5rem 0 2rem;
}
.detail-body :deep(.notice-title) { font-weight: 700; font-size: 14px; margin-bottom: 1rem; }
.detail-body :deep(.notice-dl) { margin: 0; }
.detail-body :deep(.notice-row) {
  display: grid; grid-template-columns: 88px 1fr; gap: 12px;
  padding: 10px 0; border-bottom: 1px solid #ececec;
}
.detail-body :deep(.notice-row:last-child) { border-bottom: none; }
.detail-body :deep(.notice-row dt) { font-size: 13px; color: #999; font-weight: 600; margin: 0; }
.detail-body :deep(.notice-row dd) { font-size: 13.5px; color: #333; margin: 0; line-height: 1.6; }

.back-wrap { display: flex; justify-content: center; margin-top: 2.5rem; }
.back-btn {
  padding: 11px 32px; border-radius: 8px; border: 1px solid #e0e0e0;
  background: #fff; color: #666; font-size: 14px; cursor: pointer;
}
.back-btn:hover { border-color: #ccc; }
</style>
