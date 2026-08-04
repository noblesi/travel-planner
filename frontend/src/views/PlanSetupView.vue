<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createTravelPlan } from '@/api/plans'
import { getRegions } from '@/api/regions'
import PlanSetupForm from '@/components/plan/PlanSetupForm.vue'
import AsyncState from '@/components/ui/AsyncState.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useToastStore } from '@/stores/toast'

const router = useRouter()
const toastStore = useToastStore()
const regions = ref([])
const regionStatus = ref('loading')
const regionError = ref('')
const createError = ref('')
const createFieldErrors = ref({})
const submitting = ref(false)
const createdPlanId = ref('')
const navigationPending = ref(false)
const navigationError = ref('')

const setupFields = new Set(['regionCode', 'startDate', 'endDate'])
const businessFieldErrors = {
  REGION_NOT_FOUND: ['regionCode', '선택한 여행지역을 사용할 수 없습니다. 다시 선택해 주세요.'],
  INVALID_TRAVEL_DATE_RANGE: ['endDate', '종료 날짜는 시작 날짜보다 빠를 수 없습니다.'],
  TRAVEL_PLAN_DURATION_EXCEEDED: ['endDate', '여행 기간은 최대 14일까지 설정할 수 있습니다.'],
  PAST_TRAVEL_START_DATE: ['startDate', '오늘 이후의 시작 날짜를 선택해 주세요.'],
}

function apiErrorMessage(error, fallbackMessage) {
  if (error?.response?.status === 401) {
    return '로그인 후 여행 계획을 만들 수 있습니다.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message ? message : fallbackMessage
}

function apiFieldErrors(error) {
  const errors = error?.response?.data?.errors
  if (Array.isArray(errors)) {
    const fieldErrors = Object.fromEntries(
      errors
        .filter(
          (error) =>
            error &&
            setupFields.has(error.field) &&
            typeof error.message === 'string' &&
            error.message.length > 0,
        )
        .map(({ field, message }) => [field, message]),
    )
    if (Object.keys(fieldErrors).length > 0) return fieldErrors
  }

  const businessError = businessFieldErrors[error?.response?.data?.code]
  return businessError ? { [businessError[0]]: businessError[1] } : {}
}

function clearCreateError(field) {
  createError.value = ''

  if (!(field in createFieldErrors.value)) return

  const nextErrors = { ...createFieldErrors.value }
  delete nextErrors[field]
  createFieldErrors.value = nextErrors
}

async function loadRegions() {
  regionStatus.value = 'loading'
  regionError.value = ''

  try {
    const loadedRegions = await getRegions()
    regions.value = Array.isArray(loadedRegions)
      ? loadedRegions.filter(
          (region) =>
            region &&
            typeof region.regionCode === 'string' &&
            typeof region.regionName === 'string',
        )
      : []
    regionStatus.value = regions.value.length > 0 ? 'success' : 'empty'
  } catch (error) {
    regionStatus.value = 'error'
    regionError.value = apiErrorMessage(error, '여행지역을 불러오지 못했습니다.')
  }
}

async function createPlan(payload) {
  if (submitting.value) return

  submitting.value = true
  createError.value = ''
  createFieldErrors.value = {}
  createdPlanId.value = ''
  navigationError.value = ''

  try {
    const plan = await createTravelPlan(payload)
    if (!plan?.planId) {
      throw new Error('Travel plan response does not include planId')
    }
    createdPlanId.value = plan.planId
    toastStore.success('여행 계획이 만들어졌습니다.')
  } catch (error) {
    createFieldErrors.value = apiFieldErrors(error)
    if (Object.keys(createFieldErrors.value).length === 0) {
      createError.value = apiErrorMessage(
        error,
        '여행 계획을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.',
      )
    }
  } finally {
    submitting.value = false
  }

  if (createdPlanId.value) await navigateToCreatedPlan()
}

async function navigateToCreatedPlan() {
  if (!createdPlanId.value || navigationPending.value) return

  navigationPending.value = true
  navigationError.value = ''
  try {
    const navigationFailure = await router.push({
      name: 'plan-editor',
      params: { planId: createdPlanId.value },
    })
    if (navigationFailure) throw navigationFailure
  } catch {
    navigationError.value =
      '여행 계획은 만들어졌지만 제작 화면으로 이동하지 못했습니다. 아래 버튼으로 다시 이동해 주세요.'
  } finally {
    navigationPending.value = false
  }
}

onMounted(loadRegions)
</script>

<template>
  <DefaultLayout>
    <section class="setup-page" aria-labelledby="plan-setup-title">
      <div class="setup-card">
        <header class="setup-card__header">
          <p>NEW TRAVEL PLAN</p>
          <h1 id="plan-setup-title">새로운 여행 계획하기</h1>
          <span>여행지역과 날짜를 선택하면 일차별 계획을 바로 시작할 수 있어요.</span>
        </header>

        <section v-if="createdPlanId" class="created-plan-recovery" aria-live="polite">
          <span class="created-plan-recovery__mark" aria-hidden="true">✓</span>
          <strong>여행 계획이 만들어졌습니다.</strong>
          <p v-if="navigationError" role="alert">{{ navigationError }}</p>
          <p v-else>제작 화면으로 이동하고 있습니다.</p>
          <button
            type="button"
            :disabled="navigationPending"
            :aria-busy="navigationPending"
            @click="navigateToCreatedPlan"
          >
            {{ navigationPending ? '제작 화면으로 이동 중...' : '제작 화면으로 다시 이동' }}
          </button>
        </section>

        <AsyncState
          v-else-if="regionStatus === 'loading'"
          variant="loading"
          title="여행지역을 불러오고 있어요."
        />

        <AsyncState
          v-else-if="regionStatus === 'error'"
          variant="error"
          :title="regionError"
          action-label="다시 시도"
          @action="loadRegions"
        />

        <AsyncState
          v-else-if="regionStatus === 'empty'"
          variant="empty"
          title="선택할 수 있는 여행지역이 없습니다."
          message="잠시 후 여행지역 목록을 새로고침해 주세요."
          action-label="새로고침"
          @action="loadRegions"
        />

        <PlanSetupForm
          v-else
          :regions="regions"
          :submitting="submitting"
          :server-error="createError"
          :server-field-errors="createFieldErrors"
          @field-change="clearCreateError"
          @submit="createPlan"
        />
      </div>
    </section>
  </DefaultLayout>
</template>

<style scoped>
.setup-page {
  display: grid;
  min-height: 680px;
  place-items: start center;
  padding: 72px 20px 96px;
  background: radial-gradient(circle at 50% 0%, rgb(249 115 22 / 12%), transparent 38%), var(--color-surface-muted);
}

.setup-card {
  width: min(100%, 680px);
  padding: 42px;
  border: 1px solid #e7eaf0;
  border-radius: 24px;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 24px 80px rgb(15 23 42 / 8%);
}

.setup-card__header {
  margin-bottom: 36px;
  text-align: center;
}

.setup-card__header p {
  margin: 0 0 10px;
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0.15em;
}

.setup-card__header h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(28px, 5vw, 38px);
  letter-spacing: -0.04em;
}

.setup-card__header span {
  display: block;
  margin-top: 14px;
  color: #64748b;
  line-height: 1.7;
  word-break: keep-all;
}

.created-plan-recovery {
  display: grid;
  justify-items: center;
  padding: 28px 20px;
  border: 1px solid #bbf7d0;
  border-radius: 18px;
  background: #f0fdf4;
  text-align: center;
}

.created-plan-recovery__mark {
  display: grid;
  width: 44px;
  height: 44px;
  margin-bottom: 14px;
  place-items: center;
  color: #15803d;
  border-radius: 50%;
  background: #dcfce7;
  font-size: 22px;
  font-weight: 850;
}

.created-plan-recovery strong {
  color: #166534;
  font-size: 18px;
}

.created-plan-recovery p {
  max-width: 480px;
  margin: 9px 0 0;
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
  word-break: keep-all;
}

.created-plan-recovery button {
  min-height: 44px;
  margin-top: 20px;
  padding: 0 18px;
  color: #fff;
  border: 0;
  border-radius: 12px;
  background: #16a34a;
  font-weight: 800;
  cursor: pointer;
}

.created-plan-recovery button:disabled {
  cursor: wait;
  opacity: 0.7;
}

@media (max-width: 620px) {
  .setup-page {
    padding: 32px 14px 64px;
  }

  .setup-card {
    padding: 28px 20px;
    border-radius: 18px;
  }
}

</style>
