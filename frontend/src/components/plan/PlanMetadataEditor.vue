<script setup>
import { nextTick, ref, watch } from 'vue'

import { usePlanEditorStore } from '@/stores/planEditor'
import { metadataSaveErrorMessage, validatePlanMetadata } from '@/utils/planEditorSettings'

const props = defineProps({
  plan: { type: Object, default: null },
  open: { type: Boolean, required: true },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['request-open', 'close', 'busy-change'])
const editorStore = usePlanEditorStore()

const submitting = ref(false)
const errorMessage = ref('')
const errorField = ref('')
const title = ref('')
const visibility = ref('PRIVATE')
const titleInput = ref(null)
const visibilitySelect = ref(null)

function syncForm(plan = props.plan) {
  title.value = plan?.title ?? ''
  visibility.value = plan?.visibility ?? 'PRIVATE'
}

function closeEditor() {
  if (submitting.value) return
  emit('close')
}

function resetEditor() {
  errorMessage.value = ''
  errorField.value = ''
  syncForm()
  editorStore.clearDirectSaveFailure()
}

function clearFieldError(field) {
  if (errorField.value !== field) return
  errorMessage.value = ''
  errorField.value = ''
}

async function submitChange() {
  if (submitting.value) return

  const validationError = validatePlanMetadata({
    title: title.value,
    visibility: visibility.value,
  })
  if (validationError) {
    errorMessage.value = validationError.message
    errorField.value = validationError.field
    await nextTick()
    if (validationError.field === 'title') titleInput.value?.focus()
    if (validationError.field === 'visibility') visibilitySelect.value?.focus()
    return
  }

  const normalizedTitle = title.value.trim()
  if (normalizedTitle === props.plan?.title && visibility.value === props.plan?.visibility) {
    emit('close')
    return
  }

  submitting.value = true
  errorMessage.value = ''
  errorField.value = ''
  try {
    const data = await editorStore.savePlanMetadata({
      title: normalizedTitle,
      visibility: visibility.value,
      versionNo: props.plan.versionNo,
    })
    if (!data) {
      emit('close')
      return
    }
    syncForm(data.plan)
    emit('close')
  } catch (error) {
    errorMessage.value = metadataSaveErrorMessage(error)
    errorField.value = ''
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.open,
  (open, wasOpen) => {
    if (open && !wasOpen) syncForm()
    if (!open && wasOpen) resetEditor()
  },
)
watch(
  () => props.plan,
  (plan) => {
    if (!props.open) syncForm(plan)
  },
  { immediate: true },
)
watch(submitting, (value) => emit('busy-change', value), { immediate: true })
</script>

<template>
  <section
    class="metadata-editor"
    :aria-label="open ? undefined : '플랜 정보 변경'"
    :aria-labelledby="open ? 'metadata-editor-heading' : undefined"
  >
    <button
      class="metadata-editor__open"
      :class="{ 'metadata-editor__open--active': open }"
      type="button"
      aria-controls="metadata-editor-form"
      :aria-expanded="open"
      :disabled="disabled || submitting"
      @click="open ? closeEditor() : $emit('request-open')"
    >
      플랜 제목·공개 범위 변경
    </button>
    <form
      v-if="open"
      id="metadata-editor-form"
      class="metadata-editor__form"
      @submit.prevent="submitChange"
    >
      <div class="metadata-editor__heading">
        <div>
          <span>PLAN SETTINGS</span>
          <h3 id="metadata-editor-heading">플랜 정보 변경</h3>
        </div>
        <button
          type="button"
          aria-label="플랜 정보 변경 닫기"
          :disabled="submitting"
          @click="closeEditor"
        >
          ×
        </button>
      </div>
      <div class="metadata-editor__fields">
        <label
          ><span>플랜 제목</span
          ><input
            ref="titleInput"
            v-model="title"
            name="editTitle"
            type="text"
            maxlength="200"
            autocomplete="off"
            :disabled="submitting"
            :aria-invalid="errorField === 'title'"
            :aria-describedby="errorField === 'title' ? 'metadata-editor-error' : undefined"
            @input="clearFieldError('title')"
        /></label>
        <label
          ><span>공개 범위</span
          ><select
            ref="visibilitySelect"
            v-model="visibility"
            name="editVisibility"
            :disabled="submitting"
            :aria-invalid="errorField === 'visibility'"
            :aria-describedby="errorField === 'visibility' ? 'metadata-editor-error' : undefined"
            @change="clearFieldError('visibility')"
          >
            <option value="PRIVATE">비공개</option>
            <option value="PUBLIC">공개</option>
          </select></label
        >
      </div>
      <p class="metadata-editor__notice">공개 플랜은 다른 사용자가 탐색할 수 있습니다.</p>
      <p v-if="errorMessage" id="metadata-editor-error" class="metadata-editor__error" role="alert">
        {{ errorMessage }}
      </p>
      <div class="metadata-editor__actions">
        <button type="button" :disabled="submitting" @click="closeEditor">취소</button>
        <button type="submit" :disabled="submitting" :aria-busy="submitting">
          {{ submitting ? '저장 중...' : '플랜 정보 저장' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.metadata-editor {
  margin-top: 14px;
}
.metadata-editor__open {
  width: 100%;
  min-height: 42px;
  color: var(--color-brand);
  border: 1px solid var(--color-brand-border);
  border-radius: 12px;
  background: var(--color-brand-soft);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}
.metadata-editor__open:disabled {
  cursor: not-allowed;
  opacity: 0.48;
}
.metadata-editor__open--active {
  color: var(--color-brand-on);
  border-color: var(--color-brand);
  background: var(--color-brand);
}
.metadata-editor__form {
  padding: 16px;
  border: 1px solid var(--color-brand-border);
  border-radius: 16px;
  background: var(--color-brand-soft);
}
.metadata-editor__heading,
.metadata-editor__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.metadata-editor__heading span {
  color: var(--color-brand);
  font-size: 9px;
  font-weight: 850;
  letter-spacing: 0.12em;
}
.metadata-editor__heading h3 {
  margin: 3px 0 0;
  color: #334155;
  font-size: 16px;
}
.metadata-editor__heading > button {
  width: 32px;
  height: 32px;
  color: #64748b;
  border: 0;
  border-radius: 9px;
  background: #f1f5f9;
  font-size: 20px;
  cursor: pointer;
}
.metadata-editor__fields {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}
.metadata-editor__fields label {
  display: grid;
  min-width: 0;
  gap: 6px;
}
.metadata-editor__fields label > span {
  color: #64748b;
  font-size: 11px;
  font-weight: 750;
}
.metadata-editor__fields input,
.metadata-editor__fields select {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  padding: 0 10px;
  color: #334155;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  background: #fff;
  font: inherit;
  font-size: 12px;
}
.metadata-editor__fields input[aria-invalid='true'],
.metadata-editor__fields select[aria-invalid='true'] {
  border-color: var(--color-danger);
  box-shadow: 0 0 0 3px var(--color-danger-soft);
}
.metadata-editor__notice,
.metadata-editor__error {
  margin: 11px 0 0;
  font-size: 11px;
  line-height: 1.55;
  word-break: keep-all;
}
.metadata-editor__notice {
  color: #64748b;
}
.metadata-editor__error {
  color: #b91c1c;
}
.metadata-editor__actions {
  justify-content: flex-end;
  margin-top: 14px;
}
.metadata-editor__actions button {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid #d8dee8;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
}
.metadata-editor__actions button:last-child {
  color: #fff;
  border-color: var(--color-brand);
  background: var(--color-brand);
}
.metadata-editor__actions button:disabled {
  cursor: wait;
  opacity: 0.65;
}
</style>
