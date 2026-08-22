<script setup>
import { computed, ref } from 'vue'

import { updateMyProfile } from '@/api/member'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import {
  createProfileForm,
  displayProfileValue,
  formatBirthDate,
  getGenderLabel,
  getLatestBirthDate,
  mapServerFieldErrors,
  toProfileUpdatePayload,
  validateProfileForm,
} from '@/utils/memberProfile'

const props = defineProps({
  profile: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['updated', 'open-password', 'open-withdrawal'])
const authStore = useAuthStore()
const toastStore = useToastStore()
const isEditing = ref(false)
const isSaving = ref(false)
const formError = ref('')
const fieldErrors = ref({})
const form = ref(createProfileForm())
const latestBirthDate = getLatestBirthDate()
const genderLabel = computed(() => getGenderLabel(props.profile.genderCode))
const birthDateLabel = computed(() => formatBirthDate(props.profile.birthDate))

function startEditing() {
  form.value = createProfileForm(props.profile)
  formError.value = ''
  fieldErrors.value = {}
  isEditing.value = true
}

function cancelEditing() {
  if (isSaving.value) return
  isEditing.value = false
  formError.value = ''
  fieldErrors.value = {}
  form.value = createProfileForm()
}

async function saveProfile() {
  if (isSaving.value) return

  fieldErrors.value = validateProfileForm(form.value)
  if (Object.keys(fieldErrors.value).length > 0) return

  isSaving.value = true
  formError.value = ''
  try {
    const updatedProfile = await updateMyProfile(toProfileUpdatePayload(form.value))
    if (authStore.currentUser) {
      authStore.setCurrentUser({
        ...authStore.currentUser,
        displayName: updatedProfile.nickname,
      })
    }
    isEditing.value = false
    fieldErrors.value = {}
    emit('updated', updatedProfile)
    toastStore.success('회원정보를 수정했습니다.')
  } catch (error) {
    fieldErrors.value = mapServerFieldErrors(error?.response?.data?.errors)
    formError.value =
      error?.response?.data?.message || '회원정보를 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <form class="profile-details" @submit.prevent="saveProfile">
    <div class="details-heading">
      <div>
        <p>ACCOUNT INFORMATION</p>
        <h2>나의 정보</h2>
      </div>
      <div class="details-actions">
        <template v-if="isEditing">
          <button type="button" :disabled="isSaving" @click="cancelEditing">취소</button>
          <button class="primary-action" type="submit" :disabled="isSaving">
            {{ isSaving ? '저장 중' : '저장' }}
          </button>
        </template>
        <template v-else>
          <button type="button" @click="$emit('open-password')">비밀번호 변경</button>
          <button class="primary-action" type="button" @click="startEditing">수정하기</button>
        </template>
      </div>
    </div>

    <dl>
      <div>
        <dt>이름</dt>
        <dd v-if="!isEditing">{{ displayProfileValue(profile.name) }}</dd>
        <template v-else>
          <input
            v-model="form.name"
            class="profile-input"
            name="name"
            type="text"
            maxlength="10"
            aria-label="이름"
            :aria-invalid="Boolean(fieldErrors.name)"
          />
          <small v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</small>
        </template>
      </div>
      <div>
        <dt>닉네임</dt>
        <dd v-if="!isEditing">{{ displayProfileValue(profile.nickname) }}</dd>
        <template v-else>
          <input
            v-model="form.nickname"
            class="profile-input"
            name="nickname"
            type="text"
            maxlength="50"
            aria-label="닉네임"
            :aria-invalid="Boolean(fieldErrors.nickname)"
          />
          <small v-if="fieldErrors.nickname" class="field-error">
            {{ fieldErrors.nickname }}
          </small>
        </template>
      </div>
      <div>
        <dt>이메일</dt>
        <dd>{{ profile.email }}</dd>
        <small v-if="isEditing" class="readonly-note">이메일은 변경할 수 없습니다.</small>
      </div>
      <div>
        <dt>휴대전화</dt>
        <dd v-if="!isEditing">{{ displayProfileValue(profile.phoneNumber) }}</dd>
        <template v-else>
          <input
            v-model="form.phoneNumber"
            class="profile-input"
            name="phoneNumber"
            type="tel"
            maxlength="20"
            placeholder="010-1234-5678"
            aria-label="휴대전화"
            :aria-invalid="Boolean(fieldErrors.phoneNumber)"
          />
          <small v-if="fieldErrors.phoneNumber" class="field-error">
            {{ fieldErrors.phoneNumber }}
          </small>
        </template>
      </div>
      <div>
        <dt>성별</dt>
        <dd v-if="!isEditing">{{ genderLabel }}</dd>
        <template v-else>
          <select
            v-model="form.genderCode"
            class="profile-input"
            name="genderCode"
            aria-label="성별"
            :aria-invalid="Boolean(fieldErrors.genderCode)"
          >
            <option value="N">선택 안 함</option>
            <option value="M">남성</option>
            <option value="F">여성</option>
          </select>
          <small v-if="fieldErrors.genderCode" class="field-error">
            {{ fieldErrors.genderCode }}
          </small>
        </template>
      </div>
      <div>
        <dt>생년월일</dt>
        <dd v-if="!isEditing">{{ birthDateLabel }}</dd>
        <template v-else>
          <input
            v-model="form.birthDate"
            class="profile-input"
            name="birthDate"
            type="date"
            :max="latestBirthDate"
            aria-label="생년월일"
            :aria-invalid="Boolean(fieldErrors.birthDate)"
          />
          <small v-if="fieldErrors.birthDate" class="field-error">
            {{ fieldErrors.birthDate }}
          </small>
        </template>
      </div>
    </dl>

    <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
    <footer class="details-footer">
      <span v-if="isEditing">입력 내용을 확인한 뒤 저장해 주세요.</span>
      <template v-else>
        <span>더 이상 서비스를 이용하지 않는다면 계정을 탈퇴할 수 있습니다.</span>
        <button class="withdraw-open-button" type="button" @click="$emit('open-withdrawal')">
          회원탈퇴
        </button>
      </template>
    </footer>
  </form>
</template>

<style scoped>
.profile-details {
  padding: 34px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: 0 16px 45px rgb(15 23 42 / 6%);
}

.details-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--color-border);
}

.details-heading p {
  margin: 0;
  color: var(--color-brand-accent);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.14em;
}

.details-heading h2 {
  margin: 6px 0 0;
  color: var(--color-text);
  font-size: 25px;
}

.details-heading button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  border: 1px solid var(--color-brand-border);
  border-radius: 10px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.details-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.details-heading .primary-action {
  border-color: var(--color-brand);
  background: var(--color-brand);
  color: var(--color-brand-on);
}

.details-heading button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.profile-details dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;
  margin: 26px 0 0;
}

.profile-details dl > div {
  min-width: 0;
  padding: 17px 18px;
  border-radius: 12px;
  background: var(--color-surface-muted);
}

.profile-details dt {
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 700;
}

.profile-details dd {
  margin: 8px 0 0;
  overflow-wrap: anywhere;
  color: var(--color-text);
  font-weight: 750;
}

.profile-input {
  width: 100%;
  min-height: 43px;
  margin-top: 8px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 9px;
  background: var(--color-surface);
  color: var(--color-text);
}

.profile-input[aria-invalid='true'] {
  border-color: var(--color-danger);
}

.field-error,
.readonly-note {
  display: block;
  margin-top: 7px;
  font-size: 11px;
  line-height: 1.45;
}

.field-error,
.form-error {
  color: var(--color-danger);
}

.readonly-note {
  color: var(--color-text-muted);
}

.form-error {
  margin: 20px 0 0;
  padding: 13px 15px;
  border-radius: 10px;
  background: var(--color-danger-soft);
  font-size: 12px;
}

.details-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 28px;
  padding: 16px 18px;
  border-radius: 12px;
  background: var(--color-brand-soft);
  color: var(--color-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.withdraw-open-button {
  flex: 0 0 auto;
  padding: 7px 10px;
  border: 1px solid rgb(185 28 28 / 24%);
  border-radius: 8px;
  background: var(--color-surface);
  color: var(--color-danger);
  cursor: pointer;
  font-size: 11px;
  font-weight: 750;
}

@media (max-width: 760px) {
  .profile-details {
    padding: 24px 18px;
  }

  .details-heading {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-details dl {
    grid-template-columns: 1fr;
  }

  .details-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
