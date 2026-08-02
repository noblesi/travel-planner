<template>
  <DefaultLayout>
    <div class="app-container detail-page">
    <div class="detail-head">
      <span class="cat-badge" :class="'badge-' + (announcement.category ?? 'guide')">{{ announcement.categoryLabel }}</span>
      <h1 class="detail-title">{{ announcement.title }}</h1>
      <div class="detail-meta">
        <span class="meta-item"><i class="ti ti-calendar" aria-hidden="true"></i>{{ announcement.createdAt }}</span>
        <span class="meta-divider"></span>
        <span class="meta-item"><i class="ti ti-eye" aria-hidden="true"></i>조회 {{ announcement.viewCount }}</span>
      </div>
    </div>

    <div class="detail-divider"></div>

    <div class="detail-body" v-html="announcement.content"></div>

    <div class="nav-block">
      <button class="nav-row" @click="goToAnnouncement(prevAnnouncement.id)">
        <i class="ti ti-chevron-up nav-icon" aria-hidden="true"></i>
        <span class="nav-label">이전글</span>
        <span class="nav-title">{{ prevAnnouncement.title }}</span>
      </button>
      <button class="nav-row" @click="goToAnnouncement(nextAnnouncement.id)">
        <i class="ti ti-chevron-down nav-icon" aria-hidden="true"></i>
        <span class="nav-label">다음글</span>
        <span class="nav-title">{{ nextAnnouncement.title }}</span>
      </button>
    </div>

    <div class="back-wrap">
      <button class="back-btn" @click="goToList">목록으로</button>
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
const announcement = ref({
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

// 이전글/다음글은 실제로 이동할 대상이 필요하므로 id를 함께 들고 있는다.
// TODO: 백엔드 연동 시 현재 글 기준으로 이전/다음 게시물을 API에서 조회해 채운다.
const prevAnnouncement = ref({ id: 6, title: '"내가 만든 인생 코스는?" 최고의 여름 휴가 가이드북 투표 이벤트 오픈' })
const nextAnnouncement = ref({ id: 7, title: '구글 맵 트래픽 실시간 연동 및 동선 최적화 알고리즘 업데이트 완료' })

function goToAnnouncement(id) {
  router.push({ name: 'announcements-detail', params: { id } })
}

function goToList() {
  router.push({ name: 'announcements-list' })
}
</script>

<style scoped>
* { box-sizing: border-box; }

.detail-page {
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
  background: #fff;
  padding-block: 3rem;
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

.nav-block { margin-top: 3rem; border-top: 1px solid #eee; }
.nav-row {
  width: 100%; display: flex; align-items: center; gap: 14px;
  padding: 1.1rem 0; border-bottom: 1px solid #eee;
  background: none; border-left: none; border-right: none; border-top: none; cursor: pointer; text-align: left;
}
.nav-icon { font-size: 15px; color: #bbb; flex-shrink: 0; }
.nav-label { font-size: 13px; color: #999; width: 46px; flex-shrink: 0; }
.nav-title {
  font-size: 14px; color: #333;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.back-wrap { display: flex; justify-content: center; margin-top: 2.5rem; }
.back-btn {
  padding: 11px 32px; border-radius: 8px; border: 1px solid #e0e0e0;
  background: #fff; color: #666; font-size: 14px; cursor: pointer;
}
.back-btn:hover { border-color: #ccc; }
</style>
