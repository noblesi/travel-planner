<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')
const selectedStatus = ref('all')

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

const deleteNotice = (notice) => {
  const shouldDelete = window.confirm(`'${notice.title}' 공지사항을 삭제하시겠습니까?`)

  if (!shouldDelete) {
    return
  }

  notices.value = notices.value.filter((item) => item.id !== notice.id)
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
        <input v-model="keyword" type="search" placeholder="공지 제목 목록" />
        <select v-model="selectedStatus">
          <option value="all">상태 전체</option>
          <option value="published">게시중</option>
          <option value="hidden">숨김</option>
        </select>
        <button class="search-button" type="button">검색</button>
        <button class="create-button" type="button" @click="router.push({ name: 'admin-notice-create' })">새 공지 작성</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead><tr><th>번호</th><th>제목</th><th>노출 상태</th><th>등록일</th><th>관리</th></tr></thead>
          <tbody>
            <tr v-for="notice in filteredNotices" :key="notice.id">
              <td>{{ notice.id }}</td>
              <td class="notice-title" @click="router.push({ name: 'admin-notice-detail', params: { noticeId: notice.id } })">{{ notice.title }}</td>
              <td><span class="status-badge">게시중</span></td>
              <td>{{ notice.createdAt }}</td>
              <td>
                <div class="management-buttons">
                  <button class="edit-button" type="button" @click="router.push({ name: 'admin-notice-edit', params: { noticeId: notice.id } })">수정</button>
                  <button class="delete-button" type="button" @click="deleteNotice(notice)">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>
</template>

<style scoped>
.notice-page { min-height: 100%; color: var(--admin-text); }.page-header { margin-bottom: 26px; }.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }.page-header p { margin: 9px 0 0; color: var(--admin-muted); font-size: 14px; }
.notice-card { min-height: 480px; padding: 24px; border: 1px solid var(--admin-border); border-radius: 14px; background: var(--admin-surface); box-shadow: 0 8px 24px rgb(37 42 49 / 5%); }.card-heading h2 { margin: 0 0 20px; font-size: 20px; }
.filters { display: grid; grid-template-columns: minmax(260px, 1fr) 170px 100px 120px; gap: 14px; margin-bottom: 26px; }.filters input, .filters select { height: 40px; padding: 0 13px; border: 1px solid var(--admin-border); border-radius: 6px; outline: none; background: #fff; }.filters input:focus, .filters select:focus { border-color: var(--admin-orange); }
.filters button { height: 40px; border-radius: 6px; font-weight: 800; cursor: pointer; }.search-button { border: 1px solid var(--admin-orange); background: var(--admin-orange); color: #fff; }.create-button { border: 1px solid var(--admin-orange); background: #fff; color: var(--admin-orange); }
.table-wrapper { overflow-x: auto; border: 1px solid var(--admin-border); border-radius: 7px; }table { width: 100%; min-width: 720px; border-collapse: collapse; }thead { background: #f4eee9; }th { height: 48px; font-size: 13px; }td { height: 54px; padding: 8px 14px; border-bottom: 1px solid var(--admin-border); text-align: center; font-size: 13px; }.notice-title { text-align: left; cursor: pointer; }.notice-title:hover { color: var(--admin-orange); }.status-badge { padding: 5px 9px; border-radius: 20px; background: var(--admin-orange-soft); color: var(--admin-orange); font-size: 11px; font-weight: 800; }.management-buttons { display: flex; justify-content: center; gap: 6px; }.management-buttons button { height: 27px; padding: 0 10px; border-radius: 5px; background: #fff; font-size: 11px; cursor: pointer; }.edit-button { border: 1px solid var(--admin-border); color: var(--admin-text); }.delete-button { border: 1px solid #ef857d; color: #e45f58; }
@media (max-width: 850px) { .filters { grid-template-columns: 1fr 1fr; } }@media (max-width: 560px) { .filters { grid-template-columns: 1fr; } }
</style>
