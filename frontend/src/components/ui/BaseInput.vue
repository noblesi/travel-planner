<script setup>
import { computed, useId } from 'vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: '',
  },
  id: {
    type: String,
    default: '',
  },
  label: {
    type: String,
    default: '',
  },
  type: {
    type: String,
    default: 'text',
  },
  hint: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
  disabled: Boolean,
  readonly: Boolean,
  required: Boolean,
})

const emit = defineEmits(['update:modelValue', 'blur'])
const generatedId = useId()
const inputId = computed(() => props.id || `base-input-${generatedId}`)
const hintId = computed(() => `${inputId.value}-hint`)
const errorId = computed(() => `${inputId.value}-error`)
const describedBy = computed(() => {
  const ids = []
  if (props.hint) ids.push(hintId.value)
  if (props.error) ids.push(errorId.value)
  return ids.length > 0 ? ids.join(' ') : undefined
})

function handleInput(event) {
  emit('update:modelValue', event.target.value)
}
</script>

<template>
  <div :class="['base-input', { 'base-input--error': error }]">
    <label v-if="label" class="base-input__label" :for="inputId">
      {{ label }}<span v-if="required" aria-hidden="true"> *</span>
    </label>
    <input
      v-bind="$attrs"
      :id="inputId"
      class="base-input__control"
      :type="type"
      :value="modelValue"
      :disabled="disabled"
      :readonly="readonly"
      :required="required"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      @input="handleInput"
      @blur="emit('blur', $event)"
    />
    <p v-if="hint" :id="hintId" class="base-input__hint">{{ hint }}</p>
    <p v-if="error" :id="errorId" class="base-input__error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.base-input {
  display: grid;
  gap: 7px;
  width: 100%;
}

.base-input__label {
  color: var(--color-text);
  font-size: 13px;
  font-weight: 700;
}

.base-input__label span {
  color: var(--color-danger);
}

.base-input__control {
  width: 100%;
  min-height: 44px;
  padding: 0 13px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  outline: 0;
  background: var(--color-surface);
  color: var(--color-text);
  transition: border-color 150ms ease, box-shadow 150ms ease;
}

.base-input__control::placeholder {
  color: #94a3b8;
}

.base-input__control:focus {
  border-color: var(--color-brand-accent);
  box-shadow: 0 0 0 3px var(--color-brand-focus);
}

.base-input__control:disabled {
  cursor: not-allowed;
  background: var(--color-surface-muted);
  color: #94a3b8;
}

.base-input--error .base-input__control {
  border-color: var(--color-danger);
}

.base-input__hint,
.base-input__error {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
}

.base-input__hint {
  color: var(--color-text-muted);
}

.base-input__error {
  color: var(--color-danger);
}
</style>
