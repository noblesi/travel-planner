<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getPlanDetail, toggleLike as toggleLikeApi } from '@/api/planSearch'
import PublicPlanDayMap from '@/components/plan/PublicPlanDayMap.vue'
import PublicPlanDetailHeader from '@/components/plan/PublicPlanDetailHeader.vue'
import PublicPlanSchedule from '@/components/plan/PublicPlanSchedule.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import {
  formatKoreanTravelDate,
  formatPeriodDate,
  formatShortTravelDate,
} from '@/utils/travelDate'
import ImportModal from './ImportModal.vue'
import ReportModal from './ReportModal.vue'

const props = defineProps({
  id: { type: [String, Number], default: null },
})

const router = useRouter()
const route = useRoute()
const toast = useToastStore()
const authStore = useAuthStore()
const plan = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const selectedDay = ref(1)
const showReportModal = ref(false)
const showImportModal = ref(false)
const likePending = ref(false)
let loadSequence = 0

// 신고/가져오기는 비로그인 사용자도 버튼을 누를 수 있어서, 모달을 열기 전에
// 여기서 먼저 로그인 여부를 확인한다 (백엔드 401을 받은 뒤 안내하면 원인이 불명확해짐).
// /plans/:id는 requiresAuth 라우트가 아니라 main.js의 restoreSession()을 기다리지 않고
// 곧바로 진입할 수 있어서, 페이지 로드 직후 클릭하면 세션 복원이 끝나기 전이라
// 실제로는 로그인된 사용자도 authStore.isAuthenticated가 아직 false일 수 있다.
async function requireLogin(action) {
  if (!authStore.initialized) {
    await authStore.restoreSession()
  }
  if (authStore.isAuthenticated) {
    action()
    return
  }
  if (window.confirm('로그인이 필요한 기능이에요. 로그인 페이지로 이동할까요?')) {
    router.push({ name: 'login' })
  }
}

async function openReportModal() {
  await requireLogin(() => {
    showReportModal.value = true
  })
}

async function openImportModal() {
  await requireLogin(() => {
    showImportModal.value = true
  })
}

const currentDay = computed(
  () =>
    plan.value?.days.find((day) => day.dayNumber === selectedDay.value) ?? {
      dayNumber: 1,
      dateLabel: '',
      fullDateLabel: '',
      places: [],
    },
)

function mapPlanDetail(detail) {
  return {
    id: detail.planId,
    title: detail.title,
    authorName: detail.authorName,
    periodLabel: `${formatPeriodDate(detail.startDate)} - ${formatPeriodDate(detail.endDate)} (${detail.days.length}일)`,
    likeCount: detail.likeCount,
    viewCount: detail.viewCount,
    liked: detail.liked,
    days: detail.days.map((day) => ({
      dayNumber: day.dayNumber,
      dateLabel: formatShortTravelDate(day.visitDate),
      fullDateLabel: formatKoreanTravelDate(day.visitDate),
      places: day.places.map((place, index) => ({
        id: `${day.dayNumber}-${index}`,
        timeSlot: place.timeSlot === 'MORNING' ? '오전' : '오후',
        name: place.placeName,
        address: place.address || '주소 정보가 없습니다.',
        lat: place.latitude,
        lng: place.longitude,
      })),
    })),
  }
}

async function loadPlan(planId = props.id ?? route.params.id) {
  const sequence = ++loadSequence
  loading.value = true
  errorMessage.value = ''
  plan.value = null
  likePending.value = false
  showReportModal.value = false
  showImportModal.value = false
  try {
    const loadedPlan = mapPlanDetail(await getPlanDetail(planId))
    if (sequence !== loadSequence) return null

    plan.value = loadedPlan
    const queryDay = Number(route.query.day)
    selectedDay.value = loadedPlan.days.some((day) => day.dayNumber === queryDay)
      ? queryDay
      : (loadedPlan.days[0]?.dayNumber ?? 1)
    return loadedPlan
  } catch {
    if (sequence !== loadSequence) return null

    plan.value = null
    errorMessage.value = '공개 일정을 찾을 수 없거나 불러오지 못했어요.'
    return null
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function goBack() {
  router.back()
}

function selectDay(dayNumber) {
  selectedDay.value = dayNumber
  router.replace({ query: { ...route.query, day: dayNumber } })
}

async function toggleLike() {
  if (likePending.value || !plan.value) return null

  const targetPlan = plan.value
  const targetPlanId = targetPlan.id
  const targetLoadSequence = loadSequence
  likePending.value = true
  try {
    const liked = await toggleLikeApi(targetPlanId)
    if (targetLoadSequence !== loadSequence || plan.value?.id !== targetPlanId) return null

    if (targetPlan.liked !== liked) targetPlan.likeCount += liked ? 1 : -1
    targetPlan.liked = liked
    return liked
  } catch (error) {
    if (targetLoadSequence !== loadSequence || plan.value?.id !== targetPlanId) return null

    // blocking alert 대신 공통 접근성 Toast를 사용해 기존 화면 흐름과 알림 정책을 일치시킨다.
    if (error?.response?.status === 401) {
      toast.info('로그인 후 좋아요를 누를 수 있습니다.')
      return null
    }
    toast.error('좋아요 처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
    return null
  } finally {
    if (targetLoadSequence === loadSequence) likePending.value = false
  }
}

watch(
  () => props.id ?? route.params.id,
  (planId) => loadPlan(planId),
  { immediate: true },
)
onBeforeUnmount(() => {
  loadSequence += 1
})
</script>

<template>
  <DefaultLayout>
    <div class="detail-page">
      <div v-if="loading" class="app-container detail-status" role="status">
        공개 일정을 불러오는 중이에요.
      </div>
      <div
        v-else-if="errorMessage"
        class="app-container detail-status detail-status--error"
        role="alert"
      >
        <span>{{ errorMessage }}</span>
        <button type="button" @click="loadPlan()">다시 시도</button>
      </div>
      <article v-else-if="plan" class="app-container detail-card">
        <PublicPlanDetailHeader
          :plan="plan"
          :like-pending="likePending"
          @back="goBack"
          @report="openReportModal"
          @toggle-like="toggleLike"
          @import="openImportModal"
        />
        <div class="detail-body">
          <PublicPlanSchedule
            :days="plan.days"
            :current-day="currentDay"
            :selected-day="selectedDay"
            @select-day="selectDay"
          />
          <PublicPlanDayMap :current-day="currentDay" :day-count="plan.days.length" />
        </div>
      </article>

      <ReportModal
        v-if="plan && showReportModal"
        :plan-id="plan.id"
        @close="showReportModal = false"
      />
      <ImportModal
        v-if="plan && showImportModal"
        :plan="plan"
        @close="showImportModal = false"
      />
    </div>
  </DefaultLayout>
</template>

<style scoped>
* { box-sizing: border-box; }
.detail-page {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  padding: 2rem 3rem;
  color: #1a1a1a;
  background:
    radial-gradient(circle at 4% 8%, rgb(249 115 22 / 8%) 0%, transparent 38%),
    radial-gradient(circle at 97% 68%, rgb(249 115 22 / 7%) 0%, transparent 34%),
    #f7f6f4;
}
.detail-status {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  gap: 14px;
  color: #777;
  border-radius: 20px;
  background: #fff;
}
.detail-status--error { flex-direction: column; color: #8a4c45; }
.detail-status button {
  padding: 8px 16px;
  color: var(--color-brand);
  border: 1px solid var(--color-brand-border);
  border-radius: 999px;
  background: #fff;
  cursor: pointer;
}
.detail-card {
  display: flex;
  min-width: 0;
  height: 640px;
  flex-direction: column;
  padding: 2rem 2.5rem;
  border-radius: 20px;
  background: #fff;
}
.detail-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-width: 0;
  min-height: 0;
  flex: 1;
  gap: 20px;
}
@media (max-width: 760px) {
  .detail-page { padding: 16px 12px 24px; }
  .detail-card { height: auto; padding: 20px 16px; border-radius: 16px; }
  .detail-body { display: flex; flex-direction: column; gap: 16px; }
}
</style>
