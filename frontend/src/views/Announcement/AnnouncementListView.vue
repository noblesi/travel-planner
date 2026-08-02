<template>
  <DefaultLayout>
    <div class="app-container announce-page">
    <div class="announce-head">
      <div class="eyebrow">NOTICE</div>
      <h1 class="announce-title">공지사항</h1>
      <p class="announce-sub">WithTrip의 새로운 기능 업데이트와 서비스 소식을 전해드립니다.</p>
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

    <div class="announce-list">
      <button
        v-for="item in pagedAnnouncements"
        :key="item.id"
        class="announce-card"
        @click="goToDetail(item.id)"
      >
        <div class="card-icon" :class="'icon-' + item.category.toLowerCase()">
          <i :class="'ti ' + categoryIcon(item.category)" aria-hidden="true"></i>
        </div>
        <div class="card-body">
          <div class="card-top">
            <span class="cat-badge" :class="'badge-' + item.category.toLowerCase()">{{ item.categoryLabel }}</span>
            <span v-if="isNew(item.createdAt)" class="new-badge">NEW</span>
          </div>
          <div class="card-title">{{ item.title }}</div>
        </div>
        <div class="card-date">{{ item.createdAt }}</div>
        <i class="ti ti-chevron-right card-arrow" aria-hidden="true"></i>
      </button>

      <div v-if="pagedAnnouncements.length === 0" class="empty-state">
        <i class="ti ti-file-off empty-icon" aria-hidden="true"></i>
        <div class="empty-title">해당하는 공지사항이 없어요</div>
        <div class="empty-sub">다른 분류를 선택해보세요.</div>
      </div>
    </div>

    <div class="pagination">
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
  </DefaultLayout>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = useRouter()

function goToDetail(announcementId) {
  router.push({ name: 'announcements-detail', params: { id: announcementId } })
}

// ── mock 데이터: 백엔드 연동 시 API 호출로 교체 ──
const announcements = ref([
  { id: 1, category: 'GUIDE', categoryLabel: '안내', title: '카카오맵을 이용한 동선 최적화 알고리즘 개선 안내', createdAt: '2026.07.15' },
  { id: 2, category: 'MAINTENANCE', categoryLabel: '점검', title: '데이터베이스 서버 이중화 작업을 위한 시스템 정기 점검 안내 (02:00 ~ 06:00)', createdAt: '2026.07.10' },
  { id: 3, category: 'GUIDE', categoryLabel: '안내', title: '서버 안정성을 개선하고 원활한 이용 가능 안내', createdAt: '2026.07.01' },
  { id: 4, category: 'GUIDE', categoryLabel: '안내', title: 'Wanderlog 친구 초대 및 이메일 동시 편집 기능 안정성 개선 완료', createdAt: '2026.06.25' },
  { id: 5, category: 'GUIDE', categoryLabel: '안내', title: '개인정보 처리방침 및 사용자 서비스 이용약관 일부 개정 안내', createdAt: '2026.06.18' },
  { id: 6, category: 'GUIDE', categoryLabel: '안내', title: '"내가 만든 인생 코스는?" 최고의 여름 휴가 가이드북 투표 이벤트 오픈', createdAt: '2026.06.10' },
  { id: 7, category: 'GUIDE', categoryLabel: '안내', title: '구글 맵 트래픽 실시간 연동 및 동선 최적화 알고리즘 업데이트 완료', createdAt: '2026.06.02' },
])

const categories = [
  { value: 'ALL', label: '전체' },
  { value: 'GUIDE', label: '서비스 안내' },
  { value: 'MAINTENANCE', label: '시스템 점검' },
]

const selectedCategory = ref('ALL')
const currentPage = ref(1)
const pageSize = 5

const filteredAnnouncements = computed(() => {
  if (selectedCategory.value === 'ALL') return announcements.value
  return announcements.value.filter((a) => a.category === selectedCategory.value)
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredAnnouncements.value.length / pageSize)))

const pagedAnnouncements = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAnnouncements.value.slice(start, start + pageSize)
})

watch(selectedCategory, () => { currentPage.value = 1 })

// 카테고리별 아이콘 매핑. 나중에 카테고리가 추가되면 여기에 케이스만 늘리면 된다.
function categoryIcon(category) {
  if (category === 'MAINTENANCE') return 'ti-tool'
  return 'ti-speakerphone'
}

// 작성일 기준 3일 이내 게시물을 "최신"으로 표시한다.
// mock 데이터의 createdAt은 고정 문자열이라, 데모 목적으로 목록의 첫 두 항목만 최신 취급한다.
// TODO: 백엔드 연동 시 실제 오늘 날짜와 createdAt을 비교하는 로직으로 교체.
function isNew(createdAt) {
  const NEW_THRESHOLD_DATES = ['2026.07.15', '2026.07.10']
  return NEW_THRESHOLD_DATES.includes(createdAt)
}
</script>

<style scoped>
* { box-sizing: border-box; }

.announce-page {
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
  background: #fff;
  padding-block: 3rem 4rem;
}

.announce-head { text-align: center; margin-bottom: 2.5rem; }
.eyebrow { font-size: 13px; letter-spacing: .14em; color: var(--color-brand); margin-bottom: .6rem; text-transform: uppercase; }
.announce-title { font-size: 34px; font-weight: 700; color: #1a1a1a; margin-bottom: .75rem; }
.announce-sub { font-size: 15px; color: #999; }

.filter-row { display: flex; justify-content: center; gap: 10px; margin-bottom: 2rem; }
.filter-chip {
  padding: 10px 24px; border-radius: 24px; border: 1px solid #e0e0e0;
  background: #fff; color: #666; font-size: 14px; cursor: pointer; transition: all .15s;
}
.filter-chip.active { background: var(--color-brand); border-color: var(--color-brand); color: var(--color-brand-on); }

.announce-list { display: flex; flex-direction: column; gap: 10px; }

.announce-card {
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
.announce-card:hover { border-color: #e0e0e0; box-shadow: 0 4px 14px rgba(0,0,0,.05); }

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

.pagination { display: flex; justify-content: center; align-items: center; gap: 6px; margin-top: 2rem; }
.page-arrow, .page-num {
  width: 32px; height: 32px; border-radius: 8px; border: none; background: none;
  color: #999; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center;
}
.page-arrow:disabled { color: #ddd; cursor: not-allowed; }
.page-num.active { color: #1a1a1a; font-weight: 700; text-decoration: underline; text-underline-offset: 4px; }
</style>
