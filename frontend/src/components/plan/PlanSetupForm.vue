<script setup>
import { reactive } from 'vue'

defineProps({
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
})

const emit = defineEmits(['submit'])

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

function clearError(field) {
  errors[field] = ''
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

  if (form.startDate && form.endDate) {
    if (form.startDate > form.endDate) {
      errors.endDate = '종료 날짜는 시작 날짜보다 빠를 수 없습니다.'
    } else if (inclusiveDayCount(form.startDate, form.endDate) > 14) {
      errors.endDate = '여행 기간은 최대 14일까지 설정할 수 있습니다.'
    }
  }

  return !errors.regionCode && !errors.startDate && !errors.endDate
}

function submitForm() {
  if (!validate()) return

  emit('submit', {
    regionCode: form.regionCode,
    startDate: form.startDate,
    endDate: form.endDate,
    visibility: form.visibility,
  })
}
</script>

<template>
  <form class="setup-form" novalidate @submit.prevent="submitForm">
    <fieldset class="setup-form__fieldset" :disabled="submitting">
      <div class="form-field">
        <label for="regionCode">어디로 떠나시나요?</label>
        <select
          id="regionCode"
          v-model="form.regionCode"
          name="regionCode"
          :aria-describedby="errors.regionCode ? 'regionCode-error' : undefined"
          :aria-invalid="Boolean(errors.regionCode)"
          @change="clearError('regionCode')"
        >
          <option value="" disabled>여행지역을 선택해 주세요</option>
          <option v-for="region in regions" :key="region.regionCode" :value="region.regionCode">
            {{ region.regionName }}
          </option>
        </select>
        <p v-if="errors.regionCode" id="regionCode-error" class="field-error">
          {{ errors.regionCode }}
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
              :aria-describedby="errors.startDate ? 'startDate-error' : undefined"
              :aria-invalid="Boolean(errors.startDate)"
              @input="clearError('startDate')"
            />
            <p v-if="errors.startDate" id="startDate-error" class="field-error">
              {{ errors.startDate }}
            </p>
          </div>

          <div class="form-field">
            <label for="endDate">종료 날짜</label>
            <input
              id="endDate"
              v-model="form.endDate"
              name="endDate"
              type="date"
              :min="form.startDate || undefined"
              :aria-describedby="errors.endDate ? 'endDate-error' : undefined"
              :aria-invalid="Boolean(errors.endDate)"
              @input="clearError('endDate')"
            />
            <p v-if="errors.endDate" id="endDate-error" class="field-error">
              {{ errors.endDate }}
            </p>
          </div>
        </div>
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

.form-field select,
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

.form-field select:focus,
.form-field input:focus {
  border-color: #ff5a4e;
  box-shadow: 0 0 0 4px rgb(255 90 78 / 13%);
}

.form-field select[aria-invalid='true'],
.form-field input[aria-invalid='true'] {
  border-color: #dc2626;
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
  .form-field select,
  .form-field input,
  .switch__track,
  .switch__track::after,
  .submit-button {
    transition: none;
  }
}
</style>
