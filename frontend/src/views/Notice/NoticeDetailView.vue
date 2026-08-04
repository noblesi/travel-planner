<template>
  <DefaultLayout>
    <div class="detail-page">
    <div class="app-container detail-page__inner">

    <div v-if="loading" class="status-wrap" role="status">공지사항을 불러오는 중이에요.</div>
    <div v-else-if="errorMessage" class="status-wrap status-wrap--error" role="alert">
      <span>{{ errorMessage }}</span>
      <button type="button" @click="fetchNotice">다시 시도</button>
    </div>

    <template v-else>
    <div class="detail-head">
      <span class="cat-badge" :class="'badge-' + notice.category.toLowerCase()">{{ notice.categoryLabel }}</span>
      <h1 class="detail-title">{{ notice.title }}</h1>
      <div class="detail-meta">
        <span class="meta-item"><i class="ti ti-calendar" aria-hidden="true"></i>{{ notice.createdAt }}</span>
        <span class="meta-divider"></span>
        <span class="meta-item"><i class="ti ti-eye" aria-hidden="true"></i>조회 {{ notice.viewCount }}</span>
      </div>
    </div>

    <div class="detail-divider"></div>

    <div class="detail-body">{{ notice.content }}</div>

    <div class="back-wrap">
      <button class="back-btn" @click="goToList">목록으로</button>
    </div>
    </template>

    </div>
    </div>
  </DefaultLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getNoticeDetail } from '@/api/notices'
import { NOTICE_CATEGORY_LABELS, formatNoticeDate } from '@/utils/noticeCategory'

const route = useRoute()
const router = useRouter()

const notice = ref(null)
const loading = ref(true)
const errorMessage = ref('')

async function fetchNotice() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getNoticeDetail(route.params.id)
    notice.value = {
      id: result.noticeId,
      category: result.category,
      categoryLabel: NOTICE_CATEGORY_LABELS[result.category] ?? result.category,
      title: result.title,
      createdAt: formatNoticeDate(result.createdAt),
      viewCount: result.viewCount,
      content: result.content,
    }
  } catch {
    notice.value = null
    errorMessage.value = '공지사항을 찾을 수 없어요.'
  } finally {
    loading.value = false
  }
}

function goToList() {
  router.push({ name: 'notice-list' })
}

onMounted(fetchNotice)
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

/* CONTENT는 관리자가 일반 textarea로 입력한 순수 텍스트라 v-html이 아니라 그냥 텍스트로 출력하고,
   줄바꿈(\n)만 pre-wrap으로 그대로 살린다. */
.detail-body { font-size: 15px; color: #333; line-height: 1.9; white-space: pre-wrap; }

.status-wrap {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #777;
  text-align: center;
}
.status-wrap--error { flex-direction: column; color: #8a4c45; }
.status-wrap button {
  border: 1px solid var(--color-brand-border);
  border-radius: 999px;
  padding: 8px 16px;
  background: #fff;
  color: var(--color-brand);
  cursor: pointer;
}

.back-wrap { display: flex; justify-content: center; margin-top: 2.5rem; }
.back-btn {
  padding: 11px 32px; border-radius: 8px; border: 1px solid #e0e0e0;
  background: #fff; color: #666; font-size: 14px; cursor: pointer;
}
.back-btn:hover { border-color: #ccc; }
</style>
