<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const reportId = computed(() => route.params.reportId)
const selectedAction = ref('warning')
const processingNote = ref('')

const report = {
  tripId: 'P-7788',
  reporter: '김민수',
  reportedAt: '2026-07-20 14:21',
  reason: '부적절한 내용',
  status: '검토 대기',
  content: '여행 플랜에 부적절한 내용이 포함되어 있습니다.',
}

const openReportedTrip = () => {
  router.push({
    name: 'admin-trip-detail',
    params: { tripId: report.tripId },
  })
}

const completeReview = () => {
  alert(`${selectedAction.value} 처리로 검토를 완료했습니다.`)
}
</script>

<template>
  <section class="report-detail-page">
    <header class="page-header">
      <div>
        <h1>신고 상세</h1>
        <p>여행 플랜 신고 내용과 처리 상태를 확인합니다.</p>
      </div>
      <button class="list-button" type="button" @click="router.push({ name: 'admin-trips' })">
        목록으로
      </button>
    </header>

    <div class="report-grid">
      <article class="report-card">
        <section>
          <h2>신고 정보</h2>
          <dl class="information-grid">
            <div><dt>신고 번호</dt><dd>{{ reportId }}</dd></div>
            <div><dt>플랜 번호</dt><dd>{{ report.tripId }}</dd></div>
            <div><dt>신고인</dt><dd>{{ report.reporter }}</dd></div>
            <div><dt>신고일</dt><dd>{{ report.reportedAt }}</dd></div>
            <div><dt>신고 사유</dt><dd>{{ report.reason }}</dd></div>
            <div><dt>처리 상태</dt><dd><span class="status-badge">{{ report.status }}</span></dd></div>
          </dl>
        </section>

        <section class="report-content">
          <h2>신고 내용</h2>
          <div>{{ report.content }}</div>
        </section>
      </article>

      <aside class="processing-card">
        <section>
          <h2>신고 대상 플랜</h2>
          <button class="trip-link-button" type="button" @click="openReportedTrip">
            <span>플랜 번호 {{ report.tripId }}</span>
            <strong>여행 플랜 상세로 이동</strong>
          </button>
        </section>

        <section class="processing-section">
          <h2>처리 사유</h2>

          <div class="action-options">
            <label><input v-model="selectedAction" type="radio" value="dismiss" /> 반려</label>
            <label><input v-model="selectedAction" type="radio" value="warning" /> 경고</label>
            <label><input v-model="selectedAction" type="radio" value="hide" /> 숨김</label>
          </div>

          <textarea v-model="processingNote" placeholder="처리 사유 입력" />
          <button class="complete-button" type="button" @click="completeReview">검토 완료</button>
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.report-detail-page { min-height: 100%; color: var(--admin-text); }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }
.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }
.page-header p { margin: 9px 0 0; color: var(--admin-muted); font-size: 14px; }
.list-button { height: 38px; padding: 0 15px; border: 1px solid var(--admin-orange); border-radius: 6px; background: var(--admin-surface); color: var(--admin-orange); font-size: 13px; font-weight: 800; cursor: pointer; }
.report-grid { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(300px, .8fr); gap: 24px; }
.report-card, .processing-card { padding: 26px; border: 1px solid var(--admin-border); border-radius: 14px; background: var(--admin-surface); box-shadow: 0 8px 24px rgb(37 42 49 / 5%); }
.report-card h2, .processing-card h2 { margin: 0; font-size: 17px; }
.information-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 22px 0 0; }
.information-grid > div { min-height: 66px; padding: 10px 0; border-bottom: 1px solid var(--admin-border); }
.information-grid > div:nth-child(odd) { padding-right: 22px; }.information-grid > div:nth-child(even) { padding-left: 22px; }
.information-grid dt { margin-bottom: 7px; color: var(--admin-muted); font-size: 12px; }.information-grid dd { margin: 0; font-size: 14px; }
.status-badge { display: inline-flex; padding: 5px 9px; border-radius: 20px; background: var(--admin-orange-soft); color: var(--admin-orange); font-size: 11px; font-weight: 800; }
.report-content { margin-top: 46px; }.report-content > div { min-height: 150px; margin-top: 16px; padding: 16px; border: 1px solid var(--admin-border); border-radius: 8px; background: #fffbf8; font-size: 13px; }
.processing-card { display: grid; gap: 42px; }
.trip-link-button { display: grid; gap: 8px; width: 100%; margin-top: 16px; padding: 18px; border: 1px solid var(--admin-border); border-radius: 8px; background: #fffbf8; text-align: left; cursor: pointer; }.trip-link-button span { color: var(--admin-muted); font-size: 12px; }.trip-link-button strong { color: var(--admin-orange); font-size: 14px; }.trip-link-button:hover { border-color: var(--admin-orange); box-shadow: 0 0 0 3px rgb(243 136 59 / 12%); }
.processing-section h2 { margin-bottom: 16px; }
.action-options { display: flex; gap: 10px; margin-bottom: 14px; }.action-options label { display: flex; align-items: center; gap: 5px; padding: 7px 11px; border: 1px solid var(--admin-border); border-radius: 6px; font-size: 12px; cursor: pointer; }
.processing-section textarea { width: 100%; min-height: 120px; padding: 14px; resize: vertical; border: 1px solid var(--admin-border); border-radius: 8px; outline: none; font: inherit; }.processing-section textarea:focus { border-color: var(--admin-orange); box-shadow: 0 0 0 3px rgb(243 136 59 / 14%); }
.complete-button { width: 100%; height: 42px; margin-top: 12px; border: 1px solid var(--admin-orange); border-radius: 7px; background: var(--admin-orange); color: #fff; font-weight: 800; cursor: pointer; }
@media (max-width: 950px) { .report-grid { grid-template-columns: 1fr; } }
@media (max-width: 650px) { .page-header { align-items: flex-start; flex-direction: column; }.information-grid { grid-template-columns: 1fr; }.information-grid > div:nth-child(n) { padding-right: 0; padding-left: 0; }.action-options { flex-wrap: wrap; } }
</style>
