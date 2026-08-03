<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import AdminAsyncState from '@/components/admin/AdminAsyncState.vue'
import AdminConfirmModal from '@/components/admin/AdminConfirmModal.vue'
import AdminFormControl from '@/components/admin/AdminFormControl.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import AdminStatusBadge from '@/components/admin/AdminStatusBadge.vue'
import AdminTable from '@/components/admin/AdminTable.vue'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const toast = useToastStore()
const keyword = ref('')
const selectedStatus = ref('all')
const page = ref(1)
const pageSize = 2
const pendingDelete = ref(null)
const statusOptions = [{ label: '상태 전체', value: 'all' }, { label: '게시중', value: 'published' }, { label: '숨김', value: 'hidden' }]

const notices = ref([
  { id: 1, title: '[필독] 서비스 이용약관 변경 안내', status: 'published', createdAt: '2026-07-16' },
  { id: 2, title: '카카오 지도 점검 안내', status: 'published', createdAt: '2026-05-16' },
  { id: 3, title: '여행 플랜 공유 기능 업데이트', status: 'published', createdAt: '2026-06-16' },
])

const filteredNotices = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return notices.value.filter((notice) => {
    const matchesKeyword = !query || notice.title.toLowerCase().includes(query)
    const matchesStatus = selectedStatus.value === 'all' || notice.status === selectedStatus.value
    return matchesKeyword && matchesStatus
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredNotices.value.length / pageSize)))
const paginatedNotices = computed(() => filteredNotices.value.slice((page.value - 1) * pageSize, page.value * pageSize))
watch([keyword, selectedStatus], () => { page.value = 1 })

const deleteNotice = (notice) => {
  notices.value = notices.value.filter((item) => item.id !== notice.id)
  toast.success('공지사항이 삭제되었습니다.')
  pendingDelete.value = null
}
</script>

<template>
  <section class="notice-page">
    <header class="page-header">
      <div><h1>공지사항 관리</h1><p>서비스 공지사항의 등록 및 노출 상태를 관리합니다.</p></div>
    </header>

    <article class="notice-card">
      <div class="card-heading"><h2>공지사항 목록</h2></div>
      <div class="filters">
        <AdminFormControl v-model="keyword" type="search" aria-label="공지 제목 검색" placeholder="공지 제목 검색" />
        <AdminFormControl v-model="selectedStatus" type="select" aria-label="공지 상태 선택" :options="statusOptions" />
        <button class="search-button" type="button">검색</button>
        <button class="create-button" type="button" @click="router.push({ name: 'admin-notice-create' })">새 공지 작성</button>
      </div>

      <AdminTable>
        <table>
          <thead><tr><th>번호</th><th class="text-column">제목</th><th>노출 상태</th><th>등록일</th><th>관리</th></tr></thead>
          <tbody>
            <tr v-for="notice in paginatedNotices" :key="notice.id" class="notice-row" tabindex="0" @click="router.push({ name: 'admin-notice-detail', params: { noticeId: notice.id } })" @keydown.enter="router.push({ name: 'admin-notice-detail', params: { noticeId: notice.id } })">
              <td>{{ notice.id }}</td>
              <td class="notice-title">{{ notice.title }}</td>
              <td><AdminStatusBadge tone="warning">게시중</AdminStatusBadge></td>
              <td>{{ notice.createdAt }}</td>
              <td>
                <div class="management-buttons">
                  <button class="edit-button" type="button" @click.stop="router.push({ name: 'admin-notice-edit', params: { noticeId: notice.id } })">수정</button>
                  <button class="delete-button" type="button" @click.stop="pendingDelete = notice">삭제</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredNotices.length === 0"><td colspan="5"><AdminAsyncState title="조회된 공지사항이 없습니다." description="검색어나 노출 상태를 변경해 보세요." /></td></tr>
          </tbody>
        </table>
      </AdminTable>
      <AdminPagination v-model:page="page" :total-pages="totalPages" />
    </article>
    <AdminConfirmModal v-if="pendingDelete" title="공지사항을 삭제할까요?" :message="`'${pendingDelete.title}' 공지사항은 삭제 후 복구할 수 없습니다.`" confirm-label="삭제" danger @cancel="pendingDelete = null" @confirm="deleteNotice(pendingDelete)" />
  </section>
</template>

<style scoped>
.notice-page { min-height: 100%; color: var(--admin-text); }.page-header { margin-bottom: 26px; }.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }.page-header p { margin: 9px 0 0; color: var(--admin-muted); font-size: 14px; }
.notice-card { min-height: 480px; padding: 24px; border: 1px solid var(--admin-border); border-radius: 14px; background: var(--admin-surface); box-shadow: 0 8px 24px rgb(37 42 49 / 5%); }.card-heading h2 { margin: 0 0 20px; font-size: 20px; }
.filters { display: grid; grid-template-columns: minmax(260px, 1fr) 170px 100px 120px; gap: 14px; margin-bottom: 26px; }.filters input, .filters select { height: 40px; padding: 0 13px; border: 1px solid var(--admin-border); border-radius: 6px; outline: none; background: #fff; }.filters input:focus, .filters select:focus { border-color: var(--admin-orange); }
.filters button { height: 40px; border-radius: 6px; font-weight: 800; cursor: pointer; }.search-button { border: 1px solid var(--admin-orange); background: var(--admin-orange); color: #fff; }.create-button { border: 1px solid var(--admin-orange); background: #fff; color: var(--admin-orange); }
.table-wrapper { overflow-x: auto; border: 1px solid var(--admin-border); border-radius: 7px; }table { width: 100%; min-width: 720px; border-collapse: collapse; table-layout: fixed; }thead { background: #f4eee9; }th { height: 48px; font-size: 13px; }td { height: 54px; padding: 8px 14px; border-bottom: 1px solid var(--admin-border); text-align: center; vertical-align: middle; font-size: 13px; }th.text-column, td.notice-title { text-align: left; }th.text-column { padding: 0 14px; }th:nth-child(1) { width: 10%; }th:nth-child(2) { width: 46%; }th:nth-child(3) { width: 14%; }th:nth-child(4) { width: 16%; }th:nth-child(5) { width: 14%; }.notice-row { cursor: pointer; }.notice-row:hover { background: #fffaf6; }.notice-row:focus-visible { outline: 3px solid rgb(243 136 59 / 22%); outline-offset: -3px; }.notice-title { font-weight: 700; }.notice-title:hover { color: var(--admin-orange); }.management-buttons { display: flex; justify-content: center; gap: 6px; }.management-buttons button { height: 27px; padding: 0 10px; border-radius: 5px; background: #fff; font-size: 11px; cursor: pointer; }.edit-button { border: 1px solid var(--admin-border); color: var(--admin-text); }.delete-button { border: 1px solid #ef857d; color: #e45f58; }
@media (max-width: 850px) { .filters { grid-template-columns: 1fr 1fr; } }@media (max-width: 560px) { .filters { grid-template-columns: 1fr; } }
</style>
