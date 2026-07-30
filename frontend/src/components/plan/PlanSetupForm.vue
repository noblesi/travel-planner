<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps({
  regions: {
    type: Array,
    required: true,
  },
  submitting: {
    type: Boolean,
    default: false,
  },
  serverError: {
    type: String,
    default: '',
  },
  serverFieldErrors: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['field-change', 'submit'])

const formElement = ref(null)
const regionPicker = ref(null)
const regionTrigger = ref(null)
const regionDropdownOpen = ref(false)
const activeRegionIndex = ref(-1)

const form = reactive({
  regionCode: '',
  startDate: '',
  endDate: '',
  visibility: 'PUBLIC',
})

const errors = reactive({
  regionCode: '',
  startDate: '',
  endDate: '',
})

function toDateInputValue(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 10)
}

const today = toDateInputValue(new Date())
const maxEndDate = computed(() => {
  if (!form.startDate) return undefined

  const date = new Date(`${form.startDate}T00:00:00`)
  date.setDate(date.getDate() + 13)
  return toDateInputValue(date)
})

const tripDayCount = computed(() => {
  if (!form.startDate || !form.endDate) return 0
  return inclusiveDayCount(form.startDate, form.endDate)
})

const tripDateSummary = computed(() => {
  if (tripDayCount.value < 1 || tripDayCount.value > 14) return ''
  return `${tripDayCount.value}일 여행으로 계획을 시작합니다.`
})

const selectedRegion = computed(() =>
  props.regions.find((region) => region.regionCode === form.regionCode),
)

const activeRegionId = computed(() =>
  regionDropdownOpen.value && activeRegionIndex.value >= 0
    ? `region-option-${activeRegionIndex.value}`
    : undefined,
)

function fieldError(field) {
  return errors[field] || props.serverFieldErrors[field] || ''
}

function clearError(field) {
  errors[field] = ''
  emit('field-change', field)
}

function clearDateErrors() {
  clearError('startDate')
  clearError('endDate')
}

function notifyVisibilityChange() {
  emit('field-change', 'visibility')
}

function openRegionDropdown(preferLast = false) {
  if (props.regions.length === 0) return

  const selectedIndex = props.regions.findIndex(
    (region) => region.regionCode === form.regionCode,
  )
  activeRegionIndex.value =
    selectedIndex >= 0 ? selectedIndex : preferLast ? props.regions.length - 1 : 0
  regionDropdownOpen.value = true
}

function closeRegionDropdown() {
  regionDropdownOpen.value = false
}

function toggleRegionDropdown() {
  if (regionDropdownOpen.value) {
    closeRegionDropdown()
    return
  }

  openRegionDropdown()
}

function moveActiveRegion(offset) {
  if (!regionDropdownOpen.value) {
    openRegionDropdown(offset < 0)
    return
  }

  activeRegionIndex.value =
    (activeRegionIndex.value + offset + props.regions.length) % props.regions.length
}

function selectRegion(index) {
  const region = props.regions[index]
  if (!region) return

  form.regionCode = region.regionCode
  activeRegionIndex.value = index
  clearError('regionCode')
  closeRegionDropdown()
  nextTick(() => regionTrigger.value?.focus())
}

function handleRegionKeydown(event) {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    moveActiveRegion(event.key === 'ArrowDown' ? 1 : -1)
    return
  }

  if (event.key === 'Home' || event.key === 'End') {
    event.preventDefault()
    if (!regionDropdownOpen.value) openRegionDropdown()
    activeRegionIndex.value = event.key === 'Home' ? 0 : props.regions.length - 1
    return
  }

  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    if (regionDropdownOpen.value) {
      selectRegion(activeRegionIndex.value)
    } else {
      openRegionDropdown()
    }
    return
  }

  if (event.key === 'Escape' && regionDropdownOpen.value) {
    event.preventDefault()
    closeRegionDropdown()
  }

  if (event.key === 'Tab') closeRegionDropdown()
}

function handleOutsidePointerDown(event) {
  if (!regionPicker.value?.contains(event.target)) closeRegionDropdown()
}

function inclusiveDayCount(startDate, endDate) {
  const start = Date.parse(`${startDate}T00:00:00Z`)
  const end = Date.parse(`${endDate}T00:00:00Z`)
  return Math.floor((end - start) / 86_400_000) + 1
}

function validate() {
  errors.regionCode = form.regionCode ? '' : '여행지역을 선택해 주세요.'
  errors.startDate = form.startDate ? '' : '시작 날짜를 선택해 주세요.'
  errors.endDate = form.endDate ? '' : '종료 날짜를 선택해 주세요.'

  if (form.startDate && form.startDate < today) {
    errors.startDate = '오늘 이후의 시작 날짜를 선택해 주세요.'
  }

  if (form.startDate && form.endDate) {
    if (form.startDate > form.endDate) {
      errors.endDate = '종료 날짜는 시작 날짜보다 빠를 수 없습니다.'
    } else if (inclusiveDayCount(form.startDate, form.endDate) > 14) {
      errors.endDate = '여행 기간은 최대 14일까지 설정할 수 있습니다.'
    }
  }

  return !errors.regionCode && !errors.startDate && !errors.endDate
}

async function focusFirstInvalidField() {
  await nextTick()
  formElement.value?.querySelector('[aria-invalid="true"]')?.focus()
}

function submitForm() {
  if (!validate()) {
    focusFirstInvalidField()
    return
  }

  emit('submit', {
    regionCode: form.regionCode,
    startDate: form.startDate,
    endDate: form.endDate,
    visibility: form.visibility,
  })
}

watch(
  () => props.serverFieldErrors,
  (serverFieldErrors) => {
    if (Object.keys(serverFieldErrors).length > 0) focusFirstInvalidField()
  },
  { deep: true },
)

watch(
  () => props.regions,
  () => {
    if (!props.regions.some((region) => region.regionCode === form.regionCode)) {
      form.regionCode = ''
    }
    if (activeRegionIndex.value >= props.regions.length) activeRegionIndex.value = -1
  },
  { deep: true },
)

onMounted(() => document.addEventListener('pointerdown', handleOutsidePointerDown))
onBeforeUnmount(() => document.removeEventListener('pointerdown', handleOutsidePointerDown))
</script>

<template>
  <form ref="formElement" class="setup-form" novalidate @submit.prevent="submitForm">
    <fieldset class="setup-form__fieldset" :disabled="submitting">
      <div class="form-field">
        <label id="regionCode-label" for="regionCode">어디로 떠나시나요?</label>
        <div ref="regionPicker" class="region-picker">
          <button
            id="regionCode"
            ref="regionTrigger"
            class="region-select"
            :class="{ 'region-select--open': regionDropdownOpen }"
            type="button"
            role="combobox"
            aria-autocomplete="none"
            aria-haspopup="listbox"
            aria-controls="region-options"
            :aria-activedescendant="activeRegionId"
            :aria-describedby="fieldError('regionCode') ? 'regionCode-error' : undefined"
            :aria-expanded="regionDropdownOpen"
            :aria-invalid="Boolean(fieldError('regionCode'))"
            aria-labelledby="regionCode-label regionCode-value"
            @click="toggleRegionDropdown"
            @keydown="handleRegionKeydown"
          >
            <span
              id="regionCode-value"
              class="region-select__value"
              :class="{ 'region-select__value--placeholder': !selectedRegion }"
            >
              {{ selectedRegion?.regionName || '여행지역을 선택해 주세요' }}
            </span>
            <span class="region-select__chevron" aria-hidden="true" />
          </button>

          <Transition name="region-options">
            <ul
              v-if="regionDropdownOpen"
              id="region-options"
              class="region-options"
              role="listbox"
              aria-labelledby="regionCode-label"
            >
              <li
                v-for="(region, index) in regions"
                :id="`region-option-${index}`"
                :key="region.regionCode"
                class="region-option"
                :class="{
                  'region-option--active': activeRegionIndex === index,
                  'region-option--selected': form.regionCode === region.regionCode,
                }"
                role="option"
                :aria-selected="form.regionCode === region.regionCode"
                @pointermove="activeRegionIndex = index"
                @mousedown.prevent
                @click="selectRegion(index)"
              >
                <span>{{ region.regionName }}</span>
                <span
                  v-if="form.regionCode === region.regionCode"
                  class="region-option__check"
                  aria-hidden="true"
                  >✓</span
                >
              </li>
            </ul>
          </Transition>
        </div>
        <p v-if="fieldError('regionCode')" id="regionCode-error" class="field-error" role="alert">
          {{ fieldError('regionCode') }}
        </p>
      </div>

      <div class="date-section">
        <div class="date-section__heading">
          <span>여행 날짜</span>
          <small>최대 14일</small>
        </div>

        <div class="date-grid">
          <div class="form-field">
            <label for="startDate">시작 날짜</label>
            <input
              id="startDate"
              v-model="form.startDate"
              name="startDate"
              type="date"
              :min="today"
              :aria-describedby="fieldError('startDate') ? 'startDate-error' : undefined"
              :aria-invalid="Boolean(fieldError('startDate'))"
              @input="clearDateErrors"
            />
            <p v-if="fieldError('startDate')" id="startDate-error" class="field-error" role="alert">
              {{ fieldError('startDate') }}
            </p>
          </div>

          <div class="form-field">
            <label for="endDate">종료 날짜</label>
            <input
              id="endDate"
              v-model="form.endDate"
              name="endDate"
              type="date"
              :disabled="!form.startDate"
              :min="form.startDate || today"
              :max="maxEndDate"
              :aria-describedby="fieldError('endDate') ? 'endDate-error' : undefined"
              :aria-invalid="Boolean(fieldError('endDate'))"
              @input="clearError('endDate')"
            />
            <p v-if="fieldError('endDate')" id="endDate-error" class="field-error" role="alert">
              {{ fieldError('endDate') }}
            </p>
          </div>
        </div>

        <p v-if="tripDateSummary" class="date-summary" aria-live="polite">
          {{ tripDateSummary }}
        </p>
      </div>

      <label class="visibility-card" for="visibility">
        <span>
          <strong>공개 여행</strong>
          <small>다른 사람들이 이 여행을 검색하고 볼 수 있어요.</small>
        </span>
        <span class="switch">
          <input
            id="visibility"
            v-model="form.visibility"
            class="switch__input"
            name="visibility"
            type="checkbox"
            true-value="PUBLIC"
            false-value="PRIVATE"
            @change="notifyVisibilityChange"
          />
          <span class="switch__track" aria-hidden="true" />
        </span>
      </label>

      <p v-if="serverError" class="server-error" role="alert">{{ serverError }}</p>

      <button class="submit-button" type="submit" :aria-busy="submitting">
        {{ submitting ? '여행 계획을 만들고 있어요...' : '계획을 시작하세요' }}
      </button>
    </fieldset>
  </form>
</template>

<style scoped>
.setup-form,
.setup-form__fieldset {
  margin: 0;
}

.setup-form__fieldset {
  display: grid;
  gap: 28px;
  padding: 0;
  border: 0;
}

.form-field {
  display: grid;
  gap: 9px;
}

.form-field label,
.date-section__heading span {
  color: #1f2937;
  font-size: 15px;
  font-weight: 750;
}

.region-select,
.form-field input {
  width: 100%;
  min-height: 54px;
  padding: 0 16px;
  color: #111827;
  border: 1px solid #d7dce3;
  border-radius: 14px;
  background: #fff;
  outline: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.region-select:focus-visible,
.region-select--open,
.form-field input:focus {
  border-color: #ff5a4e;
  box-shadow: 0 0 0 4px rgb(255 90 78 / 13%);
}

.region-select[aria-invalid='true'],
.form-field input[aria-invalid='true'] {
  border-color: #dc2626;
}

.region-picker {
  position: relative;
}

.region-select {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  text-align: left;
  cursor: pointer;
}

.region-select:hover:not(:disabled) {
  border-color: #b9c1cd;
}

.region-select__value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.region-select__value--placeholder {
  color: #94a3b8;
}

.region-select__chevron {
  width: 9px;
  height: 9px;
  flex: 0 0 auto;
  border-right: 2px solid #64748b;
  border-bottom: 2px solid #64748b;
  transform: translateY(-2px) rotate(45deg);
  transition: transform 160ms ease;
}

.region-select--open .region-select__chevron {
  transform: translateY(2px) rotate(225deg);
}

.region-options {
  position: absolute;
  z-index: 30;
  top: calc(100% + 8px);
  right: 0;
  left: 0;
  max-height: 280px;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 48px rgb(15 23 42 / 16%);
  list-style: none;
  overscroll-behavior: contain;
  scrollbar-color: #cbd5e1 transparent;
  scrollbar-width: thin;
}

.region-option {
  display: flex;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 12px;
  color: #334155;
  border-radius: 10px;
  cursor: pointer;
  transition:
    color 120ms ease,
    background 120ms ease;
}

.region-option--active {
  color: #d83a31;
  background: #fff1f0;
}

.region-option--selected {
  color: #d83a31;
  font-weight: 750;
}

.region-option__check {
  color: #ff5a4e;
  font-size: 16px;
  font-weight: 800;
}

.region-options-enter-active,
.region-options-leave-active {
  transition:
    opacity 140ms ease,
    transform 140ms ease;
  transform-origin: top;
}

.region-options-enter-from,
.region-options-leave-to {
  opacity: 0;
  transform: translateY(-5px) scale(0.99);
}

.date-section {
  display: grid;
  gap: 12px;
}

.date-section__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.date-section__heading small {
  color: #64748b;
}

.date-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-error,
.server-error {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
  line-height: 1.5;
}

.date-summary {
  margin: 0;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
}

.visibility-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 19px 20px;
  border: 1px solid #d7dce3;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
}

.visibility-card > span:first-child {
  display: grid;
  gap: 5px;
}

.visibility-card strong {
  color: #1f2937;
  font-size: 15px;
}

.visibility-card small {
  color: #64748b;
  line-height: 1.5;
}

.switch {
  position: relative;
  width: 50px;
  height: 28px;
  flex: 0 0 auto;
}

.switch__input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}

.switch__track {
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: #cbd5e1;
  transition: background 160ms ease;
}

.switch__track::after {
  position: absolute;
  top: 4px;
  left: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 1px 4px rgb(15 23 42 / 24%);
  content: '';
  transition: transform 160ms ease;
}

.switch__input:checked + .switch__track {
  background: #ff5a4e;
}

.switch__input:checked + .switch__track::after {
  transform: translateX(22px);
}

.switch__input:focus-visible + .switch__track {
  outline: 4px solid rgb(255 90 78 / 20%);
  outline-offset: 2px;
}

.server-error {
  padding: 12px 14px;
  border: 1px solid #fecaca;
  border-radius: 10px;
  background: #fef2f2;
}

.submit-button {
  min-height: 56px;
  color: white;
  border: 0;
  border-radius: 999px;
  background: #ff5a4e;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background 160ms ease,
    transform 160ms ease;
}

.submit-button:hover:not(:disabled) {
  background: #e8443a;
  transform: translateY(-1px);
}

.setup-form__fieldset:disabled .submit-button {
  cursor: wait;
  opacity: 0.72;
}

@media (max-width: 620px) {
  .date-grid {
    grid-template-columns: 1fr;
  }

  .visibility-card {
    align-items: flex-start;
  }
}

@media (prefers-reduced-motion: reduce) {
  .region-select,
  .region-select__chevron,
  .region-option,
  .region-options-enter-active,
  .region-options-leave-active,
  .form-field input,
  .switch__track,
  .switch__track::after,
  .submit-button {
    transition: none;
  }
}
</style>
