<template>
  <DefaultLayout>
    <div class="notice-page">
    <div class="app-container notice-page__inner">
    <div class="notice-head">
      <div class="eyebrow">NOTICE</div>
      <h1 class="notice-title">공지사항</h1>
      <p class="notice-sub">WithTrip의 새로운 기능 업데이트와 서비스 소식을 전해드립니다.</p>
    </div>

    <div class="filter-row">
      <button
        v-for="cat in categories"
        :key="cat.value"
        class="filter-chip"
        :class="{ active: selectedCategory === cat.value }"
        @click="selectedCategory = cat.value"
      >
        {{ cat.label }}
      </button>
    </div>

    <div v-if="loading" class="status-wrap" role="status">공지사항을 불러오는 중이에요.</div>
    <div v-else-if="errorMessage" class="status-wrap status-wrap--error" role="alert">
      <span>{{ errorMessage }}</span>
      <button type="button" @click="fetchNotices">다시 시도</button>
    </div>

    <div v-else class="notice-list">
      <button
        v-for="item in displayedNotices"
        :key="item.id"
        class="notice-card"
        @click="goToDetail(item.id)"
      >
        <div class="card-icon" :class="'icon-' + item.category.toLowerCase()">
          <i :class="'ti ' + categoryIcon(item.category)" aria-hidden="true"></i>
        </div>
        <div class="card-body">
          <div class="card-top">
            <span class="cat-badge" :class="'badge-' + item.category.toLowerCase()">{{ item.categoryLabel }}</span>
            <span v-if="isNew(item.rawCreatedAt)" class="new-badge">NEW</span>
          </div>
          <div class="card-title">{{ item.title }}</div>
        </div>
        <div class="card-date">{{ item.createdAt }}</div>
        <i class="ti ti-chevron-right card-arrow" aria-hidden="true"></i>
      </button>

      <div v-if="displayedNotices.length === 0" class="empty-state">
        <i class="ti ti-file-off empty-icon" aria-hidden="true"></i>
        <div class="empty-title">해당하는 공지사항이 없어요</div>
        <div class="empty-sub">다른 분류를 선택해보세요.</div>
      </div>
    </div>

    <div v-if="!loading && !errorMessage" class="pagination">
      <button class="page-arrow" :disabled="currentPage === 1" @click="currentPage--">&lt;</button>
      <button
        v-for="p in totalPages"
        :key="p"
        class="page-num"
        :class="{ active: currentPage === p }"
        @click="currentPage = p"
      >{{ p }}</button>
      <button class="page-arrow" :disabled="currentPage === totalPages" @click="currentPage++">&gt;</button>
    </div>
    </div>
    </div>
  </DefaultLayout>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { getNoticeList } from '@/api/notices'
import { NOTICE_CATEGORY_LABELS, formatNoticeDate } from '@/utils/noticeCategory'

const router = useRouter()

function goToDetail(noticeId) {
  router.push({ name: 'notice-detail', params: { id: noticeId } })
}

const categories = [
  { value: 'ALL', label: '전체' },
  { value: 'GUIDE', label: '서비스 안내' },
  { value: 'MAINTENANCE', label: '시스템 점검' },
]

const selectedCategory = ref('ALL')
const currentPage = ref(1)
const pageSize = 5

const noticePage = ref({ content: [], pagination: null })
const loading = ref(false)
const errorMessage = ref('')
let loadSequence = 0

const displayedNotices = computed(() => noticePage.value.content.map((notice) => ({
  id: notice.noticeId,
  category: notice.category,
  categoryLabel: NOTICE_CATEGORY_LABELS[notice.category] ?? notice.category,
  title: notice.title,
  createdAt: formatNoticeDate(notice.createdAt),
  rawCreatedAt: notice.createdAt,
})))

const totalPages = computed(() => noticePage.value.pagination?.totalPages ?? 1)

async function fetchNotices() {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  try {
    const category = selectedCategory.value === 'ALL' ? undefined : selectedCategory.value
    const result = await getNoticeList({ category, page: currentPage.value, size: pageSize })
    if (sequence !== loadSequence) return
    noticePage.value = result
  } catch {
    if (sequence !== loadSequence) return
    noticePage.value = { content: [], pagination: null }
    errorMessage.value = '공지사항을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

watch(selectedCategory, () => { currentPage.value = 1 })
watch([selectedCategory, currentPage], fetchNotices)
onMounted(fetchNotices)

// 카테고리별 아이콘 매핑. 나중에 카테고리가 추가되면 여기에 케이스만 늘리면 된다.
function categoryIcon(category) {
  if (category === 'MAINTENANCE') return 'ti-tool'
  return 'ti-speakerphone'
}

// 작성일 기준 3일 이내 게시물을 "최신"으로 표시한다.
function isNew(isoCreatedAt) {
  const THREE_DAYS_MS = 3 * 24 * 60 * 60 * 1000
  const createdAtMs = new Date(isoCreatedAt).getTime()
  return Number.isFinite(createdAtMs) && Date.now() - createdAtMs <= THREE_DAYS_MS
}
</script>

<style scoped>
* { box-sizing: border-box; }

/* 배경은 이 바깥 래퍼가 뷰포트 전체 폭으로 칠하고, 폭 제한은 안쪽 .notice-page__inner(app-container)가 맡는다.
   합쳐놓으면 app-container의 max-width 바깥으로 body의 --color-page(크림색)가 그대로 보인다. */
/* 홈 화면과 같은 브랜드 글로우를 좌우 여백 곳곳에 비정형적으로 흩뿌린다.
   공지 카드(.notice-card)는 자체 흰 배경이라 카드가 없는 여백에서만 은은하게 드러난다. */
.notice-page {
  min-height: calc(100vh - var(--layout-header-height));
  background:
    radial-gradient(circle at 4% 8%, rgb(249 115 22 / 8%) 0%, rgb(249 115 22 / 0%) 38%),
    radial-gradient(circle at 97% 22%, rgb(249 115 22 / 6.5%) 0%, rgb(249 115 22 / 0%) 32%),
    radial-gradient(circle at 2% 55%, rgb(249 115 22 / 6%) 0%, rgb(249 115 22 / 0%) 35%),
    radial-gradient(circle at 96% 68%, rgb(249 115 22 / 7%) 0%, rgb(249 115 22 / 0%) 34%),
    radial-gradient(circle at 6% 90%, rgb(249 115 22 / 5%) 0%, rgb(249 115 22 / 0%) 28%),
    var(--color-page);
}

.notice-page__inner {
  /* 본문 컨텐츠 영역은 app-container 기본 gutter(20px)보다 넉넉하게 덮어쓴다. */
  padding-inline: clamp(20px, 5vw, 64px);
  padding-block: 3rem 4rem;
}

.notice-head { text-align: center; margin-bottom: 2.5rem; }
.eyebrow { font-size: 13px; letter-spacing: .14em; color: var(--color-brand); margin-bottom: .6rem; text-transform: uppercase; }
.notice-title { font-size: 34px; font-weight: 700; color: #1a1a1a; margin-bottom: .75rem; }
.notice-sub { font-size: 15px; color: #999; }

.filter-row { display: flex; justify-content: center; gap: 10px; margin-bottom: 2rem; }
.filter-chip {
  padding: 10px 24px; border-radius: 24px; border: 1px solid #e0e0e0;
  background: #fff; color: #666; font-size: 14px; cursor: pointer; transition: all .15s;
}
.filter-chip.active { background: var(--color-brand); border-color: var(--color-brand); color: var(--color-brand-on); }

.status-wrap {
  min-height: 180px;
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

.notice-list { display: flex; flex-direction: column; gap: 10px; }

.notice-card {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #efefef;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: border-color .15s, box-shadow .15s;
}
.notice-card:hover { border-color: #e0e0e0; box-shadow: 0 4px 14px rgba(0,0,0,.05); }

.card-icon {
  width: 44px; height: 44px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
}
.card-icon.icon-guide { background: var(--color-secondary-soft); color: var(--color-secondary); }
.card-icon.icon-maintenance { background: #fdeee0; color: #c07a1f; }

.card-body { flex: 1; min-width: 0; }
.card-top { display: flex; align-items: center; gap: 8px; margin-bottom: 5px; }
.cat-badge { font-size: 11.5px; font-weight: 600; padding: 3px 10px; border-radius: 12px; }
.badge-guide { background: var(--color-secondary-soft); color: var(--color-secondary); }
.badge-maintenance { background: #fdeee0; color: #96601a; }
.new-badge {
  font-size: 10px; font-weight: 700; color: var(--color-brand-on); background: var(--color-brand);
  padding: 2px 7px; border-radius: 8px; letter-spacing: .02em;
}
.card-title {
  font-size: 15.5px; font-weight: 500; color: #1a1a1a; line-height: 1.5;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.card-date { font-size: 13px; color: #bbb; flex-shrink: 0; }
.card-arrow { font-size: 18px; color: #ddd; flex-shrink: 0; }

.empty-state { text-align: center; padding: 4rem 0; }
.empty-icon { font-size: 40px; color: #ddd; margin-bottom: 1rem; }
.empty-title { font-size: 16px; font-weight: 600; color: #666; margin-bottom: 5px; }
.empty-sub { font-size: 13px; color: #bbb; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 8px; margin-top: 2rem; }
.page-arrow, .page-num {
  width: 36px; height: 36px; border-radius: 50%; border: none; background: none;
  color: #666; font-size: 14px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: background .15s, color .15s;
}
.page-arrow:hover:not(:disabled), .page-num:hover:not(.active) {
  background: #f0f0f0;
  color: #1a1a1a;
}
.page-arrow:disabled { color: #ccc; cursor: not-allowed; }
.page-num.active {
  background: var(--color-brand);
  color: var(--color-brand-on);
  font-weight: 700;
}
</style>
