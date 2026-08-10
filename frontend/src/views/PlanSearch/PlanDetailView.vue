<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getPlanDetail, toggleLike as toggleLikeApi } from '@/api/planSearch'
import PublicPlanDayMap from '@/components/plan/PublicPlanDayMap.vue'
import PublicPlanDetailHeader from '@/components/plan/PublicPlanDetailHeader.vue'
import PublicPlanSchedule from '@/components/plan/PublicPlanSchedule.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
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
const plan = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const selectedDay = ref(1)
const showReportModal = ref(false)
const showImportModal = ref(false)

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
        description: place.description || '장소 설명이 없습니다.',
        lat: place.latitude,
        lng: place.longitude,
      })),
    })),
  }
}

async function loadPlan() {
  const planId = props.id ?? route.params.id
  loading.value = true
  errorMessage.value = ''
  try {
    plan.value = mapPlanDetail(await getPlanDetail(planId))
    const queryDay = Number(route.query.day)
    selectedDay.value = plan.value.days.some((day) => day.dayNumber === queryDay)
      ? queryDay
      : (plan.value.days[0]?.dayNumber ?? 1)
  } catch {
    plan.value = null
    errorMessage.value = '공개 일정을 찾을 수 없거나 불러오지 못했어요.'
  } finally {
    loading.value = false
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
  try {
    const liked = await toggleLikeApi(plan.value.id)
    plan.value.liked = liked
    plan.value.likeCount += liked ? 1 : -1
  } catch (error) {
    // blocking alert 대신 공통 접근성 Toast를 사용해 기존 화면 흐름과 알림 정책을 일치시킨다.
    if (error?.response?.status === 401) {
      toast.info('로그인 후 좋아요를 누를 수 있습니다.')
      return
    }
    toast.error('좋아요 처리에 실패했어요. 잠시 후 다시 시도해 주세요.')
  }
}

watch(() => props.id ?? route.params.id, loadPlan, { immediate: true })
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
        <button type="button" @click="loadPlan">다시 시도</button>
      </div>
      <article v-else-if="plan" class="app-container detail-card">
        <PublicPlanDetailHeader
          :plan="plan"
          @back="goBack"
          @report="showReportModal = true"
          @toggle-like="toggleLike"
          @import="showImportModal = true"
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
