<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId, useSlots, watch } from 'vue'

const props = defineProps({
  open: {
    type: Boolean,
    default: true,
  },
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  ariaLabel: {
    type: String,
    default: '',
  },
  closeLabel: {
    type: String,
    default: '닫기',
  },
  showClose: {
    type: Boolean,
    default: true,
  },
  closeOnOverlay: {
    type: Boolean,
    default: true,
  },
  closeOnEscape: {
    type: Boolean,
    default: true,
  },
  width: {
    type: String,
    default: '520px',
  },
})

const emit = defineEmits(['close'])
const slots = useSlots()
const dialogRef = ref(null)
const closeButtonRef = ref(null)
const modalId = useId()
const titleId = computed(() => `modal-${modalId}-title`)
const descriptionId = computed(() => `modal-${modalId}-description`)
let previouslyFocusedElement = null
let previousBodyOverflow = ''

function close() {
  emit('close')
}

function focusableElements() {
  if (!dialogRef.value) return []
  return Array.from(
    dialogRef.value.querySelectorAll(
      'a[href], button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled), [tabindex]:not([tabindex="-1"])',
    ),
  )
}

function handleKeydown(event) {
  if (!props.open) return

  if (event.key === 'Escape' && props.closeOnEscape) {
    event.preventDefault()
    close()
    return
  }

  if (event.key !== 'Tab') return

  const elements = focusableElements()
  if (elements.length === 0) {
    event.preventDefault()
    dialogRef.value?.focus()
    return
  }

  const firstElement = elements[0]
  const lastElement = elements[elements.length - 1]

  if (event.shiftKey && document.activeElement === firstElement) {
    event.preventDefault()
    lastElement.focus()
  } else if (!event.shiftKey && document.activeElement === lastElement) {
    event.preventDefault()
    firstElement.focus()
  }
}

async function activateModal() {
  previouslyFocusedElement = document.activeElement
  previousBodyOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  await nextTick()
  if (closeButtonRef.value) closeButtonRef.value.focus()
  else dialogRef.value?.focus()
}

function deactivateModal() {
  document.body.style.overflow = previousBodyOverflow
  previouslyFocusedElement?.focus?.()
  previouslyFocusedElement = null
}

function handleOverlay() {
  if (props.closeOnOverlay) close()
}

watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) activateModal()
    else deactivateModal()
  },
)

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  if (props.open) activateModal()
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (props.open) deactivateModal()
})
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="base-modal__overlay" @mousedown.self="handleOverlay">
      <section
        ref="dialogRef"
        class="base-modal"
        role="dialog"
        aria-modal="true"
        :aria-label="!title ? ariaLabel || undefined : undefined"
        :aria-labelledby="title ? titleId : undefined"
        :aria-describedby="description ? descriptionId : undefined"
        :style="{ '--modal-width': width }"
        tabindex="-1"
      >
        <header v-if="title || description || showClose" class="base-modal__header">
          <div>
            <h2 v-if="title" :id="titleId">{{ title }}</h2>
            <p v-if="description" :id="descriptionId">{{ description }}</p>
          </div>
          <button
            v-if="showClose"
            ref="closeButtonRef"
            class="base-modal__close"
            type="button"
            :aria-label="closeLabel"
            @click="close"
          >
            <span aria-hidden="true">×</span>
          </button>
        </header>

        <div class="base-modal__body"><slot /></div>

        <footer v-if="slots.footer" class="base-modal__footer">
          <slot name="footer" />
        </footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.base-modal__overlay {
  position: fixed;
  z-index: 1000;
  inset: 0;
  display: grid;
  padding: 20px;
  place-items: center;
  background: rgb(15 23 42 / 42%);
}

.base-modal {
  width: min(var(--modal-width), 100%);
  max-height: min(760px, calc(100vh - 40px));
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 18px;
  outline: 0;
  background: var(--color-surface);
  box-shadow: 0 24px 80px rgb(15 23 42 / 22%);
}

.base-modal__header {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 24px 24px 0;
}

.base-modal__header h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 18px;
  line-height: 1.35;
}

.base-modal__header p {
  margin: 6px 0 0;
  color: var(--color-text-muted);
  font-size: 13px;
  line-height: 1.55;
}

.base-modal__close {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  padding: 0;
  border: 0;
  border-radius: 50%;
  place-items: center;
  background: var(--color-surface-muted);
  color: var(--color-text-muted);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}

.base-modal__close:hover {
  background: var(--color-brand-soft);
  color: var(--color-brand);
}

.base-modal__body {
  max-height: calc(100vh - 210px);
  padding: 24px;
  overflow-y: auto;
}

.base-modal__footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 0 24px 24px;
}

@media (max-width: 520px) {
  .base-modal__overlay {
    padding: 12px;
  }

  .base-modal__header,
  .base-modal__body {
    padding-right: 20px;
    padding-left: 20px;
  }

  .base-modal__footer {
    padding-right: 20px;
    padding-left: 20px;
  }
}
</style>
