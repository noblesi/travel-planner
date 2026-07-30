<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createTravelPlan } from '@/api/plans'
import { getRegions } from '@/api/regions'
import PlanSetupForm from '@/components/plan/PlanSetupForm.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const router = useRouter()
const regions = ref([])
const regionStatus = ref('loading')
const regionError = ref('')
const createError = ref('')
const submitting = ref(false)

function apiErrorMessage(error, fallbackMessage) {
  if (error?.response?.status === 401) {
    return '로그인 후 여행 계획을 만들 수 있습니다.'
  }

  const message = error?.response?.data?.message
  return typeof message === 'string' && message ? message : fallbackMessage
}

async function loadRegions() {
  regionStatus.value = 'loading'
  regionError.value = ''

  try {
    regions.value = await getRegions()
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

  try {
    const plan = await createTravelPlan(payload)
    await router.push({ name: 'plan-editor', params: { planId: plan.planId } })
  } catch (error) {
    createError.value = apiErrorMessage(
      error,
      '여행 계획을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.',
    )
  } finally {
    submitting.value = false
  }
}

onMounted(loadRegions)
</script>

<template>
  <DefaultLayout>
    <section class="setup-page">
      <div class="setup-card">
        <header class="setup-card__header">
          <p>NEW TRAVEL PLAN</p>
          <h1>새로운 여행 계획하기</h1>
          <span>여행지역과 날짜를 선택하면 일차별 계획을 바로 시작할 수 있어요.</span>
        </header>

        <div v-if="regionStatus === 'loading'" class="state-card" aria-live="polite">
          <span class="state-card__spinner" aria-hidden="true" />
          <strong>여행지역을 불러오고 있어요.</strong>
        </div>

        <div v-else-if="regionStatus === 'error'" class="state-card" role="alert">
          <strong>{{ regionError }}</strong>
          <button type="button" @click="loadRegions">다시 시도</button>
        </div>

        <div v-else-if="regionStatus === 'empty'" class="state-card" role="status">
          <strong>선택할 수 있는 여행지역이 없습니다.</strong>
          <button type="button" @click="loadRegions">새로고침</button>
        </div>

        <PlanSetupForm
          v-else
          :regions="regions"
          :submitting="submitting"
          :server-error="createError"
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
  background: radial-gradient(circle at 50% 0%, rgb(255 90 78 / 10%), transparent 38%), #f8fafc;
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
  color: #ff5a4e;
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

.state-card {
  display: grid;
  min-height: 220px;
  gap: 16px;
  place-items: center;
  align-content: center;
  color: #475569;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  text-align: center;
}

.state-card button {
  padding: 10px 16px;
  color: #fff;
  border: 0;
  border-radius: 10px;
  background: #ff5a4e;
  font-weight: 700;
  cursor: pointer;
}

.state-card__spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #fee2e2;
  border-top-color: #ff5a4e;
  border-radius: 50%;
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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

@media (prefers-reduced-motion: reduce) {
  .state-card__spinner {
    animation: none;
  }
}
</style>
