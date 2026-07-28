<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const noticeId = computed(() => route.params.noticeId)
const displayStatus = ref('published')

const notice = {
  title: '서비스 이용약관 변경 안내',
  type: '필독 공지',
}
</script>

<template>
  <section class="detail-page">
    <header class="page-header">
      <div><h1>공지사항 상세</h1><p>게시된 공지 내용을 확인합니다.</p></div>
      <div class="header-actions">
        <button type="button" @click="router.push({ name: 'admin-notices' })">목록으로</button>
        <button class="accent" type="button" @click="router.push({ name: 'admin-notice-edit', params: { noticeId } })">수정</button>
        <button class="danger" type="button">삭제</button>
      </div>
    </header>

    <div class="detail-grid">
      <article class="content-card">
        <span class="status-badge">게시중</span>
        <h2>{{ notice.title }}</h2>
        <p>안녕하세요. TripPlan입니다.<br />보다 안정적인 서비스 제공과 사용자 권리 보호를 위해 서비스 이용약관이 일부 변경됩니다.</p>
        <h3>주요 변경 내용</h3>
        <ol><li>공개 여행 플랜의 운영 및 신고 처리 기준이 구체화됩니다.</li><li>회원 여행 플랜 참여자 권한에 대한 안내가 추가됩니다.</li><li>부적절한 콘텐츠에 대한 노출 제한 기준이 변경됩니다.</li></ol>
        <h3>적용 일정</h3><p>변경된 약관은 2026년 8월 1일부터 적용됩니다.<br />감사합니다.</p>
        <div class="post-navigation"><span>이전 글　여행 플랜 공유 기능 업데이트 안내</span><span>다음 글　카카오 지도 서비스 점검 안내</span></div>
      </article>

      <aside class="info-card">
        <h2>게시 정보</h2>
        <dl>
          <div><dt>공지 유형</dt><dd>{{ notice.type }}</dd></div>
          <div><dt>게시 상태</dt><dd><span class="status-badge">게시중</span></dd></div>
          <div><dt>상단 고정</dt><dd><select v-model="displayStatus"><option value="published">사용</option><option value="unused">사용 안 함</option></select></dd></div>
          <div><dt>첨부 파일</dt><dd>없음</dd></div>
        </dl>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.detail-page { min-height: 100%; color: var(--admin-text); }.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }.page-header h1 { margin: 0; font-size: 34px; }.page-header p { margin: 9px 0 0; color: var(--admin-muted); font-size: 14px; }.header-actions { display: flex; gap: 8px; }.header-actions button { height: 37px; padding: 0 14px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; cursor: pointer; }.header-actions .accent { border-color: var(--admin-orange); color: var(--admin-orange); }.header-actions .danger { border-color: #ef857d; color: #e45f58; }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(280px, .75fr); gap: 24px; }.content-card, .info-card { padding: 28px; border: 1px solid var(--admin-border); border-radius: 14px; background: var(--admin-surface); box-shadow: 0 8px 24px rgb(37 42 49 / 5%); }.status-badge { display: inline-flex; padding: 5px 9px; border-radius: 20px; background: var(--admin-orange-soft); color: var(--admin-orange); font-size: 11px; font-weight: 800; }.content-card h2 { margin: 10px 0 20px; padding-bottom: 18px; border-bottom: 1px solid var(--admin-border); font-size: 23px; }.content-card h3 { margin: 28px 0 12px; font-size: 16px; }.content-card p, .content-card li { color: #656c75; font-size: 13px; line-height: 1.8; }.post-navigation { display: grid; gap: 12px; margin-top: 46px; padding-top: 18px; border-top: 1px solid var(--admin-border); color: var(--admin-muted); font-size: 12px; }
.info-card h2 { margin: 0 0 20px; font-size: 18px; }.info-card dl { margin: 0; }.info-card dl > div { padding: 13px 0; border-bottom: 1px solid var(--admin-border); }.info-card dt { margin-bottom: 7px; color: var(--admin-muted); font-size: 12px; }.info-card dd { margin: 0; font-size: 13px; }.info-card select { width: 100%; height: 36px; padding: 0 10px; border: 1px solid var(--admin-border); border-radius: 6px; background: #fff; }
@media (max-width: 900px) { .detail-grid { grid-template-columns: 1fr; } }@media (max-width: 600px) { .page-header { align-items: flex-start; flex-direction: column; }.header-actions { flex-wrap: wrap; } }
</style>
