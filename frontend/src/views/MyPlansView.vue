<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { deleteTravelPlan, getMyTravelPlans, restoreTravelPlan } from '@/api/plans'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useToastStore } from '@/stores/toast'
import { formatKoreanTravelDate } from '@/utils/travelDate'

const toastStore = useToastStore()
const plans = ref([])
const status = ref('loading')
const errorMessage = ref('')
const selectedFilter = ref('ACTIVE')
const busyPlanId = ref(null)

const filters = [
  { value: 'ACTIVE', label: '진행 중' },
  { value: 'OWNED', label: '내가 만든 플랜' },
  { value: 'INVITED', label: '초대받은 플랜' },
  { value: 'DELETED', label: '삭제된 플랜' },
]

const visiblePlans = computed(() =>
  plans.value.filter((plan) => {
    if (selectedFilter.value === 'DELETED') return plan.planStatus === 'DELETED'
    if (plan.planStatus !== 'ACTIVE') return false
    if (selectedFilter.value === 'OWNED') return plan.currentMemberRole === 'CREATOR'
    if (selectedFilter.value === 'INVITED') return plan.currentMemberRole === 'INVITEE'
    return true
  }),
)

function planStateLabel(plan) {
  if (plan.planStatus === 'DELETED') return '삭제됨'
  if (plan.publishStatus === 'DRAFT') return '작성 중'
  return plan.visibility === 'PUBLIC' ? '공개 완료' : '비공개 완료'
}

async function loadPlans() {
  status.value = 'loading'
  errorMessage.value = ''
  try {
    const data = await getMyTravelPlans()
    plans.value = Array.isArray(data?.plans) ? data.plans : []
    status.value = 'success'
  } catch (error) {
    status.value = 'error'
    errorMessage.value =
      error?.response?.data?.message || '내 플랜 목록을 불러오지 못했습니다.'
  }
}

async function removePlan(plan) {
  if (busyPlanId.value) return
  const confirmed = window.confirm(
    `“${plan.title}” 플랜을 삭제할까요? 삭제된 플랜 목록에서 복구할 수 있습니다.`,
  )
  if (!confirmed) return

  busyPlanId.value = plan.planId
  try {
    await deleteTravelPlan(plan.planId, plan.versionNo)
    toastStore.success('플랜을 삭제했습니다.')
    await loadPlans()
  } catch (error) {
    toastStore.error(error?.response?.data?.message || '플랜을 삭제하지 못했습니다.')
  } finally {
    busyPlanId.value = null
  }
}

async function restorePlan(plan) {
  if (busyPlanId.value) return
  busyPlanId.value = plan.planId
  try {
    await restoreTravelPlan(plan.planId, plan.versionNo)
    toastStore.success('플랜을 복구했습니다.')
    await loadPlans()
  } catch (error) {
    toastStore.error(error?.response?.data?.message || '플랜을 복구하지 못했습니다.')
  } finally {
    busyPlanId.value = null
  }
}

onMounted(loadPlans)
</script>

<template>
  <DefaultLayout>
    <main class="my-plans-page" aria-labelledby="my-plans-title">
      <header class="my-plans-header">
        <div>
          <p>MY TRAVEL PLANS</p>
          <h1 id="my-plans-title">내 플랜</h1>
          <span>작성 중인 플랜과 동행 중인 여행을 한곳에서 관리하세요.</span>
        </div>
        <RouterLink class="create-link" :to="{ name: 'plan-setup' }">새 플랜 만들기</RouterLink>
      </header>

      <nav class="plan-filters" aria-label="내 플랜 필터">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          :class="{ active: selectedFilter === filter.value }"
          :aria-pressed="selectedFilter === filter.value"
          @click="selectedFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </nav>

      <section v-if="status === 'loading'" class="plan-state" role="status">
        내 플랜을 불러오고 있습니다.
      </section>
      <section v-else-if="status === 'error'" class="plan-state plan-state--error" role="alert">
        <p>{{ errorMessage }}</p>
        <button type="button" @click="loadPlans">다시 시도</button>
      </section>
      <section v-else-if="visiblePlans.length === 0" class="plan-state">
        선택한 조건에 해당하는 플랜이 없습니다.
      </section>
      <section v-else class="plan-grid" aria-live="polite">
        <article v-for="plan in visiblePlans" :key="plan.planId" class="plan-card">
          <div class="plan-card__topline">
            <span :class="['state-badge', `state-badge--${plan.planStatus.toLowerCase()}`]">
              {{ planStateLabel(plan) }}
            </span>
            <small>{{ plan.currentMemberRole === 'CREATOR' ? '생성자' : '동행자' }}</small>
          </div>
          <h2>{{ plan.title }}</h2>
          <p>{{ plan.regionName }}</p>
          <dl>
            <div><dt>기간</dt><dd>{{ formatKoreanTravelDate(plan.startDate) }} - {{ formatKoreanTravelDate(plan.endDate) }}</dd></div>
            <div><dt>공개 범위</dt><dd>{{ plan.visibility === 'PUBLIC' ? '공개' : '비공개' }}</dd></div>
          </dl>
          <div class="plan-card__actions">
            <template v-if="plan.planStatus === 'ACTIVE'">
              <RouterLink :to="{ name: 'plan-editor', params: { planId: plan.planId } }">
                제작 화면 열기
              </RouterLink>
              <button
                v-if="plan.currentMemberRole === 'CREATOR'"
                type="button"
                :disabled="busyPlanId === plan.planId"
                @click="removePlan(plan)"
              >
                삭제
              </button>
            </template>
            <button
              v-else
              class="restore-button"
              type="button"
              :disabled="busyPlanId === plan.planId"
              @click="restorePlan(plan)"
            >
              플랜 복구
            </button>
          </div>
        </article>
      </section>
    </main>
  </DefaultLayout>
</template>

<style scoped>
.my-plans-page { min-width: 1080px; min-height: 720px; padding: 56px max(32px, calc((100% - 1180px) / 2)) 88px; background: #f6f8fb; }
.my-plans-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 32px; }
.my-plans-header p { margin: 0; color: #ff5a4e; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
.my-plans-header h1 { margin: 7px 0 0; color: #172033; font-size: 36px; letter-spacing: -.04em; }
.my-plans-header span { display: block; margin-top: 10px; color: #64748b; }
.create-link { display: inline-flex; min-height: 46px; align-items: center; padding: 0 20px; color: #fff; border-radius: 12px; background: #ff5a4e; font-weight: 800; text-decoration: none; }
.plan-filters { display: flex; gap: 8px; margin-top: 34px; padding-bottom: 14px; border-bottom: 1px solid #dfe5ed; }
.plan-filters button { min-height: 38px; padding: 0 15px; color: #64748b; border: 1px solid #dce3ec; border-radius: 999px; background: #fff; cursor: pointer; }
.plan-filters button.active { color: #fff; border-color: #334155; background: #334155; font-weight: 800; }
.plan-state { margin-top: 24px; padding: 70px 24px; color: #64748b; border: 1px dashed #cbd5e1; border-radius: 18px; background: #fff; text-align: center; }
.plan-state p { margin: 0; }.plan-state button { margin-top: 14px; padding: 10px 14px; }.plan-state--error { color: #b91c1c; }
.plan-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; margin-top: 24px; }
.plan-card { display: grid; align-content: start; padding: 22px; border: 1px solid #e1e7ef; border-radius: 18px; background: #fff; box-shadow: 0 12px 35px rgb(15 23 42 / 5%); }
.plan-card__topline { display: flex; align-items: center; justify-content: space-between; }.plan-card__topline small { color: #64748b; }
.state-badge { padding: 5px 9px; color: #9a3412; border-radius: 999px; background: #ffedd5; font-size: 10px; font-weight: 800; }.state-badge--deleted { color: #991b1b; background: #fee2e2; }
.plan-card h2 { margin: 17px 0 0; color: #1e293b; font-size: 19px; }.plan-card > p { margin: 7px 0 0; color: #64748b; }
.plan-card dl { display: grid; gap: 8px; margin: 18px 0 0; padding: 14px; border-radius: 11px; background: #f8fafc; }.plan-card dl div { display: flex; justify-content: space-between; gap: 14px; }.plan-card dt { color: #94a3b8; font-size: 11px; }.plan-card dd { margin: 0; color: #475569; font-size: 11px; }
.plan-card__actions { display: flex; gap: 8px; margin-top: 18px; }.plan-card__actions a,.plan-card__actions button { display: inline-flex; min-height: 38px; align-items: center; justify-content: center; padding: 0 13px; border: 1px solid #d8dee8; border-radius: 10px; background: #fff; color: #475569; font-size: 11px; font-weight: 800; text-decoration: none; cursor: pointer; }.plan-card__actions a,.plan-card__actions .restore-button { color: #fff; border-color: #ff5a4e; background: #ff5a4e; }.plan-card__actions button:disabled { cursor: wait; opacity: .55; }
</style>
