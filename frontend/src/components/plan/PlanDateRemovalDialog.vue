<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, required: true },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'confirm'])

const dialog = ref(null)
const cancelButton = ref(null)

function closeDialog() {
  if (!props.busy) emit('close')
}

function handleKeydown(event) {
  if (event.key === 'Escape') {
    closeDialog()
    event.preventDefault()
    return
  }
  if (event.key !== 'Tab') return

  const focusableElements = Array.from(
    dialog.value?.querySelectorAll('button:not(:disabled)') ?? [],
  )
  if (focusableElements.length === 0) {
    event.preventDefault()
    return
  }

  const firstElement = focusableElements[0]
  const lastElement = focusableElements.at(-1)
  const activeElement = document.activeElement
  if (
    event.shiftKey &&
    (activeElement === firstElement || !dialog.value.contains(activeElement))
  ) {
    lastElement.focus()
    event.preventDefault()
  } else if (!event.shiftKey && activeElement === lastElement) {
    firstElement.focus()
    event.preventDefault()
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) nextTick(() => cancelButton.value?.focus())
  },
)
</script>

<template>
  <div v-if="open" class="confirmation-backdrop" @click.self="closeDialog">
    <section
      ref="dialog"
      class="confirmation-dialog"
      role="alertdialog"
      tabindex="-1"
      aria-modal="true"
      aria-labelledby="date-removal-title"
      aria-describedby="date-removal-description"
      @keydown="handleKeydown"
    >
      <span class="confirmation-dialog__icon" aria-hidden="true">!</span>
      <h2 id="date-removal-title">일정이 포함된 날짜를 제외할까요?</h2>
      <p id="date-removal-description">
        변경 범위에서 빠지는 DAY와 그 안의 오전·오후 일정이 삭제됩니다. 이 작업은 저장 후 되돌릴
        수 없습니다.
      </p>
      <div class="confirmation-dialog__actions">
        <button ref="cancelButton" type="button" :disabled="busy" @click="closeDialog">
          다시 확인
        </button>
        <button type="button" :disabled="busy" @click="$emit('confirm')">
          {{ busy ? '변경 중...' : '일정 삭제 후 변경' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.confirmation-backdrop {
  position: fixed; z-index: 50; inset: 0; display: grid; padding: 20px;
  place-items: center; background: rgb(15 23 42 / 48%); backdrop-filter: blur(4px);
}
.confirmation-dialog {
  width: min(100%, 440px); padding: 30px; border-radius: 20px; background: #fff;
  box-shadow: 0 28px 80px rgb(15 23 42 / 24%);
}
.confirmation-dialog__icon {
  display: grid; width: 44px; height: 44px; place-items: center; color: #b91c1c;
  border-radius: 50%; background: #fee2e2; font-size: 20px; font-weight: 850;
}
.confirmation-dialog h2 {
  margin: 18px 0 0; color: #1e293b; font-size: 21px; letter-spacing: -0.03em;
}
.confirmation-dialog p {
  margin: 10px 0 0; color: #64748b; font-size: 13px;
  line-height: 1.7; word-break: keep-all;
}
.confirmation-dialog__actions {
  display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 24px;
}
.confirmation-dialog__actions button {
  min-height: 38px; padding: 0 14px; border: 1px solid #d8dee8;
  border-radius: 10px; background: #fff; font-size: 12px; font-weight: 750; cursor: pointer;
}
.confirmation-dialog__actions button:last-child {
  color: #fff; border-color: var(--color-brand); background: var(--color-brand);
}
.confirmation-dialog__actions button:disabled { cursor: wait; opacity: 0.65; }
</style>
