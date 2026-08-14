<template>
  <BaseModal
    title="일정 가져오기"
    :description="step === 'new' ? '새 플랜의 이름과 날짜를 설정해요.' : ''"
    width="520px"
    @close="handleClose"
  >
    <div v-if="step === 'new'" class="form-grid">
      <BaseInput v-model="newPlanName" label="플랜 이름" required />
      <div class="date-row">
        <BaseInput v-model="newStartDate" label="시작일" type="date" required />
        <BaseInput v-model="newEndDate" label="종료일" type="date" required />
      </div>
      <div class="notice-box">
        💡 원본은 {{ plan.days.length }}일 일정이에요. 여행 기간을 더 짧게 설정하면, 넘어가는
        날짜의 일정은 담기지 않아요.
      </div>
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
    </div>

    <div v-else class="success-wrap">
      <div class="success-icon" aria-hidden="true">🎉</div>
      <strong>새 플랜이 만들어졌어요!</strong>
      <p>{{ newPlanName }}가 저장됐어요.<br />날짜나 장소는 언제든지 수정할 수 있어요.</p>
    </div>

    <template #footer>
      <BaseButton
        v-if="step === 'new'"
        class="modal-action"
        :disabled="!newPlanName.trim() || !newStartDate || !newEndDate || submitting"
        @click="submitNew"
      >
        {{ submitting ? '만드는 중...' : '플랜 만들기' }}
      </BaseButton>
      <BaseButton v-else class="modal-action" @click="goToMyPlan">내 플랜 보러 가기</BaseButton>
    </template>
  </BaseModal>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { copyPlan } from '@/api/planSearch'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseModal from '@/components/ui/BaseModal.vue'

const props = defineProps({
  plan: { type: Object, required: true },
})
const emit = defineEmits(['close'])

const router = useRouter()
const step = ref('new')
const newPlanName = ref(`${props.plan.title} (복사)`)
const newStartDate = ref('')
const newEndDate = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const newPlanId = ref(null)

function apiErrorMessage(error) {
  if (error?.response?.status === 401) return '로그인 후 일정을 가져올 수 있어요.'

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '일정을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.'
}

async function submitNew() {
  submitting.value = true
  errorMessage.value = ''
  try {
    newPlanId.value = await copyPlan(props.plan.id, {
      title: newPlanName.value,
      startDate: newStartDate.value,
      endDate: newEndDate.value,
    })
    step.value = 'success'
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

function handleClose() {
  emit('close')
}

function goToMyPlan() {
  if (newPlanId.value != null) {
    router.push({ name: 'plan-editor', params: { planId: newPlanId.value } })
    return
  }
  handleClose()
}
</script>

<style scoped>
.form-grid {
  display: grid;
  gap: 18px;
}

.error-text {
  margin: 0;
  color: #b23a24;
  font-size: 12.5px;
}

.date-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.notice-box {
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 12px;
  line-height: 1.6;
}

.success-wrap {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 12px 0 4px;
  text-align: center;
}

.success-icon {
  display: grid;
  width: 56px;
  height: 56px;
  margin-bottom: 8px;
  border-radius: 50%;
  place-items: center;
  background: var(--color-success-soft);
  font-size: 24px;
}

.success-wrap strong {
  color: var(--color-text);
}

.success-wrap p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.65;
}

.modal-action {
  flex: 1;
}

@media (max-width: 520px) {
  .date-row {
    grid-template-columns: 1fr;
  }
}
</style>
