<template>
  <BaseModal
    :title="step === 'form' ? '이 여행 플랜을 신고할게요' : '신고 접수 완료'"
    :description="step === 'form' ? '신고 사유를 선택해 주세요.' : ''"
    width="440px"
    @close="emit('close')"
  >
    <template v-if="step === 'form'">
      <div class="reason-list">
        <label v-for="option in reasons" :key="option.value" class="reason-item">
          <input v-model="selectedReason" type="radio" name="reason" :value="option.value" />
          <span>{{ option.label }}</span>
        </label>
      </div>

      <label class="detail-label" for="report-detail">상세 내용 <span>(선택)</span></label>
      <textarea
        id="report-detail"
        v-model="detail"
        class="detail-input"
        placeholder="자세한 내용을 입력해 주세요."
        rows="3"
      />
      <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
    </template>

    <div v-else class="success-wrap">
      <div class="success-icon" aria-hidden="true">✓</div>
      <strong>신고가 접수됐어요.</strong>
      <p>운영팀이 내용을 검토한 후 필요한 조치를 진행할게요.</p>
    </div>

    <template #footer>
      <template v-if="step === 'form'">
        <BaseButton class="footer-button" variant="secondary" @click="emit('close')">취소</BaseButton>
        <BaseButton
          class="footer-button footer-button--primary"
          :disabled="!selectedReason || submitting"
          @click="submit"
        >
          {{ submitting ? '접수 중...' : '신고하기' }}
        </BaseButton>
      </template>
      <BaseButton v-else class="footer-button" @click="emit('close')">확인</BaseButton>
    </template>
  </BaseModal>
</template>

<script setup>
import { ref } from 'vue'

import { reportPlan } from '@/api/planSearch'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseModal from '@/components/ui/BaseModal.vue'

const props = defineProps({
  planId: { type: [String, Number], required: true },
})
const emit = defineEmits(['close'])

const reasons = [
  { value: 'INAPPROPRIATE', label: '부적절한 콘텐츠' },
  { value: 'FALSE_INFO', label: '허위/잘못된 정보' },
  { value: 'SPAM', label: '스팸/광고성' },
  { value: 'OTHER', label: '기타' },
]

const selectedReason = ref('')
const detail = ref('')
const step = ref('form')
const submitting = ref(false)
const errorMessage = ref('')

function apiErrorMessage(error) {
  if (error?.response?.status === 401) return '로그인 후 신고할 수 있어요.'

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '신고를 접수하지 못했어요. 잠시 후 다시 시도해 주세요.'
}

async function submit() {
  if (!selectedReason.value.trim() || submitting.value) return
  submitting.value = true
  errorMessage.value = ''
  try {
    await reportPlan(props.planId, { reason: selectedReason.value, detail: detail.value })
    step.value = 'done'
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

</script>

<style scoped>
.reason-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 1.25rem;
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.reason-item input {
  width: 16px;
  height: 16px;
  accent-color: var(--color-brand-accent);
}

.detail-label {
  display: block;
  margin-bottom: 7px;
  color: var(--color-text);
  font-size: 13px;
  font-weight: 700;
}

.detail-label span {
  color: var(--color-text-muted);
  font-weight: 500;
}

.detail-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  resize: none;
  outline: none;
}

.detail-input:focus {
  border-color: var(--color-brand-accent);
  box-shadow: 0 0 0 3px var(--color-brand-focus);
}

.error-text {
  margin: 10px 0 0;
  color: #b23a24;
  font-size: 12.5px;
}

.success-wrap {
  display: grid;
  gap: 8px;
  justify-items: center;
  text-align: center;
  padding: 12px 0 4px;
}

.success-icon {
  display: grid;
  width: 52px;
  height: 52px;
  margin-bottom: 8px;
  border-radius: 50%;
  place-items: center;
  background: var(--color-success-soft);
  color: #047857;
  font-size: 22px;
  font-weight: 800;
}

.success-wrap strong {
  color: var(--color-text);
}

.success-wrap p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.footer-button {
  flex: 1;
}

.footer-button--primary {
  flex: 1.4;
}
</style>
