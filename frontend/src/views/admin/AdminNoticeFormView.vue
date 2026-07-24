<script setup>
import { computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const isEditMode = computed(() => route.name === 'admin-notice-edit')

const form = reactive({
  type: 'normal',
  target: 'all',
  title: isEditMode.value ? '서비스 이용약관 변경 안내' : '',
  startAt: '2026-07-19T10:00',
  endAt: '',
  endDateMode: 'unselected',
  content: isEditMode.value ? '안녕하세요. TripPlan입니다.\n서비스 이용약관 변경 내용을 안내드립니다.' : '',
  adminMemo: '',
})

const submitNotice = () => {
  router.push({ name: 'admin-notices' })
}
</script>

<template>
  <section class="form-page">
    <header class="page-header">
      <div><h1>{{ isEditMode ? '공지사항 수정' : '공지사항 작성' }}</h1><p>서비스 공지를 작성하고 게시 일정을 설정합니다.</p></div>
      <div class="header-actions"><button type="button" @click="router.push({ name: 'admin-notices' })">취소</button><button class="submit" type="button" @click="submitNotice">{{ isEditMode ? '수정' : '등록' }}</button></div>
    </header>

    <form class="notice-form" @submit.prevent="submitNotice">
      <div class="two-columns">
        <label><span>공지 유형</span><select v-model="form.type"><option value="normal">일반 공지</option><option value="required">필독 공지</option></select></label>
        <label><span>공지 대상</span><select v-model="form.target"><option value="all">전체 회원</option><option value="planner">파워 플래너</option></select></label>
      </div>
      <label><span>제목</span><input v-model="form.title" type="text" placeholder="공지 제목을 입력하세요" /></label>
      <div class="two-columns">
        <label><span>게시 시작일</span><input v-model="form.startAt" type="datetime-local" /></label>
        <label>
          <span class="field-title">
            게시 종료일
            <label class="end-date-option">
              <input v-model="form.endDateMode" type="radio" value="unselected" />
              선택 안 함
            </label>
          </span>
          <input
            v-model="form.endAt"
            type="datetime-local"
          />
        </label>
      </div>
      <label><span>공지 내용</span><textarea v-model="form.content" placeholder="공지 내용을 입력하세요" /></label>
      <label class="memo-field"><span>관리자 메모</span><textarea v-model="form.adminMemo" placeholder="사용자에게 노출되지 않는 관리자 메모" /></label>
    </form>
  </section>
</template>

<style scoped>
.form-page { min-height: 100%; color: var(--admin-text); }.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }.page-header h1 { margin: 0; font-size: 34px; }.page-header p { margin: 9px 0 0; color: var(--admin-muted); font-size: 14px; }.header-actions { display: flex; gap: 8px; }.header-actions button { height: 38px; padding: 0 15px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; cursor: pointer; }.header-actions .submit { border-color: var(--admin-orange); background: var(--admin-orange); color: #fff; font-weight: 800; }
.notice-form { display: grid; gap: 20px; padding: 26px; border: 1px solid var(--admin-border); border-radius: 14px; background: var(--admin-surface); box-shadow: 0 8px 24px rgb(37 42 49 / 5%); }.two-columns { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 28px; }.notice-form label { display: grid; gap: 8px; }.notice-form label > span { font-size: 14px; font-weight: 800; }.notice-form input, .notice-form select, .notice-form textarea { width: 100%; border: 1px solid var(--admin-border); border-radius: 7px; outline: none; background: #fff; font: inherit; }.notice-form input, .notice-form select { height: 42px; padding: 0 12px; }.notice-form textarea { min-height: 180px; padding: 14px; resize: vertical; }.notice-form input:focus, .notice-form select:focus, .notice-form textarea:focus { border-color: var(--admin-orange); box-shadow: 0 0 0 3px rgb(243 136 59 / 14%); }.memo-field { width: 45%; margin-left: auto; }.memo-field textarea { min-height: 100px; }
.notice-form .field-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.notice-form .end-date-option { display: flex; align-items: center; grid-template-columns: auto 1fr; gap: 5px; font-size: 12px; font-weight: 500; cursor: pointer; }.notice-form .end-date-option input { width: 14px; height: 14px; padding: 0; accent-color: var(--admin-orange); }.notice-form input:disabled { background: #f5f1ee; color: var(--admin-muted); cursor: not-allowed; }
@media (max-width: 700px) { .page-header { align-items: flex-start; flex-direction: column; }.two-columns { grid-template-columns: 1fr; }.memo-field { width: 100%; } }
</style>
