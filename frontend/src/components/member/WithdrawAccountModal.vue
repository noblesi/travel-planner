<script setup>
import { ref, watch } from 'vue'
import { isNavigationFailure, useRouter } from 'vue-router'

import { withdrawMyAccount } from '@/api/member'
import BaseModal from '@/components/ui/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { validateWithdrawalPassword } from '@/utils/memberProfile'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close'])
const authStore = useAuthStore()
const toastStore = useToastStore()
const router = useRouter()
const currentPassword = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)

watch(
  () => props.open,
  (open) => {
    if (open) resetForm()
  },
)

function resetForm() {
  currentPassword.value = ''
  errorMessage.value = ''
}

function close() {
  if (isSubmitting.value) return
  resetForm()
  emit('close')
}

async function navigateHome() {
  const message =
    '회원탈퇴는 완료되었지만 홈 화면으로 이동하지 못했습니다. 새로고침해 주세요.'
  try {
    const failure = await router.replace({ name: 'home' })
    if (!isNavigationFailure(failure)) return true
  } catch {
    toastStore.error(message)
    return false
  }

  toastStore.error(message)
  return false
}

async function submit() {
  if (isSubmitting.value) return

  errorMessage.value = validateWithdrawalPassword(currentPassword.value)
  if (errorMessage.value) return

  isSubmitting.value = true
  try {
    try {
      await withdrawMyAccount(currentPassword.value)
    } catch (error) {
      errorMessage.value =
        error?.response?.data?.message ||
        '회원탈퇴를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
      return
    }

    authStore.clearSession()
    resetForm()
    emit('close')
    toastStore.info('회원탈퇴가 완료되었습니다.')
    await navigateHome()
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <BaseModal
    :open="open"
    title="회원탈퇴"
    description="탈퇴 후에는 현재 계정으로 다시 로그인할 수 없습니다."
    :close-on-overlay="!isSubmitting"
    :close-on-escape="!isSubmitting"
    @close="close"
  >
    <form id="withdraw-account-form" class="account-modal-form withdraw-form" @submit.prevent="submit">
      <p>본인 확인을 위해 현재 비밀번호를 입력해 주세요.</p>
      <label for="withdrawal-password">현재 비밀번호</label>
      <input
        id="withdrawal-password"
        v-model="currentPassword"
        type="password"
        autocomplete="current-password"
        maxlength="72"
        :disabled="isSubmitting"
        :aria-invalid="Boolean(errorMessage)"
      />
      <p v-if="errorMessage" class="withdrawal-error" role="alert">{{ errorMessage }}</p>
    </form>
    <template #footer>
      <button type="button" class="modal-cancel-button" :disabled="isSubmitting" @click="close">
        취소
      </button>
      <button
        type="submit"
        form="withdraw-account-form"
        class="modal-withdraw-button"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? '처리 중' : '탈퇴하기' }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.withdraw-form > p:first-child {
  margin: 0 0 18px;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.account-modal-form label {
  display: block;
  color: var(--color-text);
  font-size: 12px;
  font-weight: 750;
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

.withdrawal-error {
  margin: 10px 0 0;
  color: var(--color-danger);
  font-size: 12px;
}

.modal-cancel-button,
.modal-withdraw-button {
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

.modal-withdraw-button {
  border: 1px solid var(--color-danger);
  background: var(--color-danger);
  color: #fff;
}

.modal-cancel-button:disabled,
.modal-withdraw-button:disabled {
  cursor: wait;
  opacity: 0.55;
}
</style>
