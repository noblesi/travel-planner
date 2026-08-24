<script setup>
import { ref, watch } from 'vue'

import { changeMyPassword } from '@/api/member'
import BaseModal from '@/components/ui/BaseModal.vue'
import { useToastStore } from '@/stores/toast'
import { validatePasswordChange } from '@/utils/memberProfile'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close'])
const toastStore = useToastStore()
const form = ref(createEmptyForm())
const errorMessage = ref('')
const isSubmitting = ref(false)

watch(
  () => props.open,
  (open) => {
    if (open) resetForm()
  },
)

function createEmptyForm() {
  return {
    currentPassword: '',
    newPassword: '',
    newPasswordConfirm: '',
  }
}

function resetForm() {
  form.value = createEmptyForm()
  errorMessage.value = ''
}

function close() {
  if (isSubmitting.value) return
  resetForm()
  emit('close')
}

async function submit() {
  if (isSubmitting.value) return

  errorMessage.value = validatePasswordChange(form.value)
  if (errorMessage.value) return

  isSubmitting.value = true
  try {
    await changeMyPassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword,
    })
    resetForm()
    emit('close')
    toastStore.success('비밀번호를 변경했습니다.')
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.message || '비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <BaseModal
    :open="open"
    title="비밀번호 변경"
    description="현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다."
    :close-on-overlay="!isSubmitting"
    :close-on-escape="!isSubmitting"
    @close="close"
  >
    <form id="change-password-form" class="account-modal-form" @submit.prevent="submit">
      <label for="current-password">현재 비밀번호</label>
      <input
        id="current-password"
        v-model="form.currentPassword"
        type="password"
        autocomplete="current-password"
        maxlength="72"
        :disabled="isSubmitting"
      />

      <label for="new-password">새 비밀번호</label>
      <input
        id="new-password"
        v-model="form.newPassword"
        type="password"
        autocomplete="new-password"
        minlength="10"
        maxlength="72"
        :disabled="isSubmitting"
      />

      <label for="new-password-confirm">새 비밀번호 확인</label>
      <input
        id="new-password-confirm"
        v-model="form.newPasswordConfirm"
        type="password"
        autocomplete="new-password"
        minlength="10"
        maxlength="72"
        :disabled="isSubmitting"
        :aria-invalid="Boolean(errorMessage)"
      />
      <p v-if="errorMessage" class="account-modal-error" role="alert">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <button type="button" class="modal-cancel-button" :disabled="isSubmitting" @click="close">
        취소
      </button>
      <button
        type="submit"
        form="change-password-form"
        class="modal-primary-button"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? '변경 중' : '변경하기' }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.account-modal-form label {
  display: block;
  color: var(--color-text);
  font-size: 12px;
  font-weight: 750;
}

.account-modal-form label:not(:first-child) {
  margin-top: 16px;
}

.account-modal-form input {
  width: 100%;
  min-height: 44px;
  margin-top: 8px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 9px;
}

.account-modal-form input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.account-modal-error {
  margin: 10px 0 0;
  color: var(--color-danger);
  font-size: 12px;
}

.modal-cancel-button,
.modal-primary-button {
  min-height: 40px;
  padding: 0 15px;
  border-radius: 9px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.modal-cancel-button {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
}

.modal-primary-button {
  border: 1px solid var(--color-brand);
  background: var(--color-brand);
  color: var(--color-brand-on);
}

.modal-cancel-button:disabled,
.modal-primary-button:disabled {
  cursor: wait;
  opacity: 0.55;
}
</style>
