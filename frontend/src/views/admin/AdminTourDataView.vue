<script setup>
import { computed, ref } from 'vue'

import AdminConfirmModal from '@/components/admin/AdminConfirmModal.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminStatusBadge from '@/components/admin/AdminStatusBadge.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

// 화면 확인용 데이터입니다. 백엔드 연결 시 통계 조회 API 응답으로 교체합니다.
const summaryCards = [
  { label: '관광지', value: '14,500', caption: '전체 등록 데이터' },
  { label: '음식점', value: '8,321', caption: '전체 등록 데이터' },
  { label: '숙박', value: '3,432', caption: '전체 등록 데이터' },
  { label: '최근 갱신', value: '321', caption: '마지막 동기화 반영' },
]

const syncHistory = ref([
  {
    id: 'S-20260726-03',
    startedAt: '2026.07.26 14:20',
    endedAt: '2026.07.26 14:28',
    changedCount: 4826,
    failedCount: 0,
    status: 'success',
    manager: '홍길동',
  },
  {
    id: 'S-20260724-02',
    startedAt: '2026.07.24 11:05',
    endedAt: '2026.07.24 11:13',
    changedCount: 864,
    failedCount: 0,
    status: 'success',
    manager: '김관리',
  },
  {
    id: 'S-20260721-01',
    startedAt: '2026.07.21 09:30',
    endedAt: '2026.07.21 09:42',
    changedCount: 1237,
    failedCount: 3,
    status: 'partial',
    manager: '홍길동',
  },
])

const isSyncing = ref(false)
const showSyncConfirm = ref(false)
const page = ref(1)
const pageSize = 2
const connectionTitle = ref('TourAPI 연결 정상')
const connectionDescription = ref('마지막 동기화: 2026.07.26 14:28 · 변경 데이터 4,826건')

// 이후 POST /api/admin/tour-data/sync 호출로 교체할 수동 동기화 동작입니다.
const synchronizeTourData = () => {
  if (isSyncing.value) return

  isSyncing.value = true
  showSyncConfirm.value = false
  connectionTitle.value = '관광데이터 동기화 진행 중'
  connectionDescription.value = 'TourAPI에서 변경된 데이터를 확인하고 있습니다.'

  window.setTimeout(() => {
    syncHistory.value.unshift({
      id: 'S-20260728-04',
      startedAt: '2026.07.28 10:20',
      endedAt: '2026.07.28 10:21',
      changedCount: 321,
      failedCount: 0,
      status: 'success',
      manager: '홍길동',
    })

    connectionTitle.value = '수동 동기화 완료'
    connectionDescription.value = '변경 데이터 321건을 정상적으로 반영했습니다.'
    isSyncing.value = false
    toast.success('관광데이터 동기화가 완료되었습니다.')
  }, 1000)
}

const totalPages = computed(() => Math.max(1, Math.ceil(syncHistory.value.length / pageSize)))
const paginatedHistory = computed(() => syncHistory.value.slice((page.value - 1) * pageSize, page.value * pageSize))

const statusLabel = (status) => (status === 'success' ? '성공' : '부분 성공')
</script>

<template>
  <section class="tour-data-page">
    <header class="page-header">
      <div>
        <h1>관광데이터 관리</h1>
        <p>한국관광공사 TourAPI 데이터를 직접 동기화하고 실행 결과를 확인합니다.</p>
      </div>

      <button
        class="sync-button"
        type="button"
        :disabled="isSyncing"
        @click="showSyncConfirm = true"
      >
        <span class="sync-icon" :class="{ 'sync-icon--running': isSyncing }">↻</span>
        {{ isSyncing ? '동기화 중' : '지금 동기화' }}
      </button>
    </header>

    <div class="summary-grid">
      <article
        v-for="card in summaryCards"
        :key="card.label"
        class="summary-card"
      >
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.caption }}</small>
      </article>
    </div>

    <article class="connection-panel">
      <header class="panel-header">
        <div>
          <h2>TourAPI 연결 상태</h2>
          <p>데이터 제공 서버의 연결 상태와 마지막 동기화 결과입니다.</p>
        </div>
        <span class="connection-badge">정상</span>
      </header>

      <div class="connection-status">
        <span
          class="status-dot"
          :class="{ 'status-dot--running': isSyncing }"
          aria-hidden="true"
        />
        <div>
          <strong>{{ connectionTitle }}</strong>
          <p>{{ connectionDescription }}</p>
        </div>
      </div>
    </article>

    <article class="history-panel">
      <header class="panel-header">
        <div>
          <h2>최근 동기화 이력</h2>
          <p>관리자가 직접 실행한 최근 동기화 결과입니다.</p>
        </div>
        <span class="history-count">총 {{ syncHistory.length }}건</span>
      </header>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th class="text-column">작업번호</th>
              <th>시작 일시</th>
              <th>종료 일시</th>
              <th>변경</th>
              <th>실패</th>
              <th>상태</th>
              <th class="text-column">실행자</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="history in paginatedHistory" :key="history.id">
              <td class="text-column history-id">{{ history.id }}</td>
              <td>{{ history.startedAt }}</td>
              <td>{{ history.endedAt }}</td>
              <td>{{ history.changedCount.toLocaleString() }}</td>
              <td>{{ history.failedCount.toLocaleString() }}</td>
              <td>
                <AdminStatusBadge :tone="history.status === 'success' ? 'success' : 'warning'">
                  {{ statusLabel(history.status) }}
                </AdminStatusBadge>
              </td>
              <td class="text-column history-manager">{{ history.manager }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <AdminPagination v-model:page="page" :total-pages="totalPages" />
    </article>
    <AdminConfirmModal v-if="showSyncConfirm" title="관광데이터를 동기화할까요?" message="TourAPI의 최신 데이터를 확인하고 변경 사항을 반영합니다." confirm-label="동기화 시작" @cancel="showSyncConfirm = false" @confirm="synchronizeTourData" />
  </section>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.tour-data-page {
  min-height: 100%;
  color: var(--admin-text);
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 26px;
}

.page-header h1 {
  margin: 0;
  font-size: 34px;
  letter-spacing: -1px;
}

.page-header p,
.panel-header p {
  margin: 8px 0 0;
  color: var(--admin-muted);
  font-size: 13px;
}

.sync-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 132px;
  height: 42px;
  padding: 0 17px;
  border: 0;
  border-radius: 6px;
  background: var(--admin-orange);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.sync-button:hover:not(:disabled) {
  background: var(--admin-orange-hover);
}

.sync-button:disabled {
  cursor: wait;
  opacity: 0.7;
}

.sync-icon {
  font-size: 20px;
  line-height: 1;
}

.sync-icon--running {
  animation: rotate 0.8s linear infinite;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin-bottom: 28px;
}

.summary-card,
.connection-panel,
.history-panel {
  border: 1px solid var(--admin-border);
  border-radius: 7px;
  background: var(--admin-surface);
  box-shadow: 0 3px 12px rgb(31 41 55 / 4%);
}

.summary-card {
  min-height: 126px;
  padding: 22px 24px;
}

.summary-card span {
  color: var(--admin-muted);
  font-size: 14px;
  font-weight: 700;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
}

.summary-card small {
  display: block;
  margin-top: 8px;
  color: #a0a5ad;
  font-size: 11px;
}

.connection-panel,
.history-panel {
  padding: 24px;
}

.connection-panel {
  margin-bottom: 28px;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.panel-header h2 {
  margin: 0;
  font-size: 18px;
}

.connection-badge,
.history-count,
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 800;
}

.connection-badge,
.status-badge--success {
  background: #e6f7ed;
  color: #269663;
}

.history-count {
  background: var(--admin-orange-soft);
  color: var(--admin-orange);
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 22px;
  padding: 20px;
  border-radius: 7px;
  background: var(--admin-orange-soft);
}

.connection-status p {
  margin: 6px 0 0;
  color: var(--admin-muted);
  font-size: 12px;
}

.status-dot {
  flex-shrink: 0;
  width: 13px;
  height: 13px;
  border-radius: 50%;
  background: #26c86e;
}

.status-dot--running {
  background: var(--admin-orange);
  box-shadow: 0 0 0 5px rgb(243 136 59 / 16%);
}

.table-wrapper {
  margin-top: 22px;
  overflow-x: auto;
  border: 1px solid var(--admin-border);
  border-radius: 5px;
}

table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
  table-layout: fixed;
}

thead {
  background: #f6eee8;
}

th,
td {
  height: 50px;
  padding: 9px 12px;
  border-bottom: 1px solid var(--admin-border);
  color: #565c65;
  font-size: 12px;
  text-align: center;
  vertical-align: middle;
}

th.text-column,
td.text-column {
  text-align: left;
}

th.text-column {
  padding: 9px 12px;
}

.history-id,
.history-manager {
  font-weight: 700;
}

th {
  color: #656b73;
  font-weight: 800;
}

tbody tr:last-child td {
  border-bottom: 0;
}

tbody tr:hover {
  background: #fffaf6;
}

.status-badge--partial {
  background: #fff0e5;
  color: #dc7632;
}

@keyframes rotate {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 650px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .sync-button {
    width: 100%;
  }
}
</style>
