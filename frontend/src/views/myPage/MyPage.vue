<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  changeMyPassword,
  getMyProfile,
  updateMyProfile,
  withdrawMyAccount,
} from '@/api/member'
import DefaultProfileImage from '@/assets/myPageImage/default_profile.webp'
import BaseModal from '@/components/ui/BaseModal.vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const authStore = useAuthStore()
const toastStore = useToastStore()
const router = useRouter()

const profile = ref(null)
const status = ref('loading')
const errorMessage = ref('')
const imageLoadFailed = ref(false)
const isEditing = ref(false)
const isSaving = ref(false)
const formError = ref('')
const fieldErrors = ref({})
const form = ref(emptyForm())
const isWithdrawalOpen = ref(false)
const isWithdrawing = ref(false)
const withdrawalPassword = ref('')
const withdrawalError = ref('')
const isPasswordModalOpen = ref(false)
const isChangingPassword = ref(false)
const passwordError = ref('')
const passwordForm = ref(emptyPasswordForm())

const currentDate = new Date()
const today = formatDateInput(currentDate)
const latestBirthDate = formatDateInput(
  new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate() - 1),
)

const profileImageUrl = computed(() => {
  if (profile.value?.profileImageUrl && !imageLoadFailed.value) {
    return profile.value.profileImageUrl
  }
  return DefaultProfileImage
})

const genderLabel = computed(() => {
  const labels = { M: '남성', F: '여성' }
  return labels[profile.value?.genderCode] || '미입력'
})

const birthDateLabel = computed(() => {
  const birthDate = profile.value?.birthDate
  if (!birthDate) return '미입력'

  const date = new Date(`${birthDate}T00:00:00`)
  if (Number.isNaN(date.getTime())) return birthDate
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date)
})

function displayValue(value) {
  return value || '미입력'
}

function formatDateInput(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function emptyForm() {
  return {
    name: '',
    nickname: '',
    genderCode: 'N',
    birthDate: '',
    phoneNumber: '',
  }
}

function emptyPasswordForm() {
  return {
    currentPassword: '',
    newPassword: '',
    newPasswordConfirm: '',
  }
}

function startEditing() {
  form.value = {
    name: profile.value.name || '',
    nickname: profile.value.nickname || '',
    genderCode: profile.value.genderCode || 'N',
    birthDate: profile.value.birthDate || '',
    phoneNumber: profile.value.phoneNumber || '',
  }
  formError.value = ''
  fieldErrors.value = {}
  isEditing.value = true
}

function cancelEditing() {
  isEditing.value = false
  formError.value = ''
  fieldErrors.value = {}
  form.value = emptyForm()
}

function validateForm() {
  const errors = {}
  const name = form.value.name.trim()
  const nickname = form.value.nickname.trim()
  const phoneNumber = form.value.phoneNumber.trim()

  if (!name) errors.name = '이름을 입력해 주세요.'
  else if (name.length > 10) errors.name = '이름은 10자 이하로 입력해 주세요.'

  if (!nickname) errors.nickname = '닉네임을 입력해 주세요.'
  else if (nickname.length > 50) errors.nickname = '닉네임은 50자 이하로 입력해 주세요.'

  if (!['M', 'F', 'N'].includes(form.value.genderCode)) {
    errors.genderCode = '성별을 선택해 주세요.'
  }
  if (form.value.birthDate && form.value.birthDate >= today) {
    errors.birthDate = '생년월일은 오늘보다 이전이어야 합니다.'
  }
  if (phoneNumber && !/^\d{2,3}-?\d{3,4}-?\d{4}$/.test(phoneNumber)) {
    errors.phoneNumber = '전화번호 형식이 올바르지 않습니다.'
  }

  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function applyServerValidationErrors(errors) {
  if (!Array.isArray(errors)) return
  fieldErrors.value = Object.fromEntries(
    errors
      .filter((error) => error?.field && error?.message)
      .map((error) => [error.field, error.message]),
  )
}

async function saveProfile() {
  if (isSaving.value || !validateForm()) return

  isSaving.value = true
  formError.value = ''
  try {
    const updatedProfile = await updateMyProfile({
      name: form.value.name.trim(),
      nickname: form.value.nickname.trim(),
      genderCode: form.value.genderCode,
      birthDate: form.value.birthDate || null,
      phoneNumber: form.value.phoneNumber.trim() || null,
    })
    profile.value = updatedProfile
    if (authStore.currentUser) {
      authStore.setCurrentUser({
        ...authStore.currentUser,
        displayName: updatedProfile.nickname,
      })
    }
    isEditing.value = false
    fieldErrors.value = {}
    toastStore.success('회원정보를 수정했습니다.')
  } catch (error) {
    applyServerValidationErrors(error?.response?.data?.errors)
    formError.value =
      error?.response?.data?.message || '회원정보를 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSaving.value = false
  }
}

function openWithdrawal() {
  withdrawalPassword.value = ''
  withdrawalError.value = ''
  isWithdrawalOpen.value = true
}

function closeWithdrawal() {
  if (isWithdrawing.value) return
  isWithdrawalOpen.value = false
  withdrawalPassword.value = ''
  withdrawalError.value = ''
}

async function submitWithdrawal() {
  if (isWithdrawing.value) return
  if (withdrawalPassword.value.length < 10) {
    withdrawalError.value = '현재 비밀번호를 정확히 입력해 주세요.'
    return
  }

  isWithdrawing.value = true
  withdrawalError.value = ''
  try {
    await withdrawMyAccount(withdrawalPassword.value)
    authStore.clearSession()
    isWithdrawalOpen.value = false
    withdrawalPassword.value = ''
    toastStore.info('회원탈퇴가 완료되었습니다.')
    await router.replace({ name: 'home' })
  } catch (error) {
    withdrawalError.value =
      error?.response?.data?.message || '회원탈퇴를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isWithdrawing.value = false
  }
}

function openPasswordChange() {
  passwordForm.value = emptyPasswordForm()
  passwordError.value = ''
  isPasswordModalOpen.value = true
}

function closePasswordChange() {
  if (isChangingPassword.value) return
  isPasswordModalOpen.value = false
  passwordForm.value = emptyPasswordForm()
  passwordError.value = ''
}

async function submitPasswordChange() {
  if (isChangingPassword.value) return
  if (!passwordForm.value.currentPassword) {
    passwordError.value = '현재 비밀번호를 입력해 주세요.'
    return
  }
  if (passwordForm.value.newPassword.length < 10) {
    passwordError.value = '새 비밀번호는 10자 이상 입력해 주세요.'
    return
  }
  if (passwordForm.value.newPassword.length > 72) {
    passwordError.value = '새 비밀번호는 72자 이하로 입력해 주세요.'
    return
  }
  if (passwordForm.value.currentPassword === passwordForm.value.newPassword) {
    passwordError.value = '새 비밀번호는 현재 비밀번호와 다르게 입력해 주세요.'
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.newPasswordConfirm) {
    passwordError.value = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  isChangingPassword.value = true
  passwordError.value = ''
  try {
    await changeMyPassword({
      currentPassword: passwordForm.value.currentPassword,
      newPassword: passwordForm.value.newPassword,
    })
    isPasswordModalOpen.value = false
    passwordForm.value = emptyPasswordForm()
    toastStore.success('비밀번호를 변경했습니다.')
  } catch (error) {
    passwordError.value =
      error?.response?.data?.message || '비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isChangingPassword.value = false
  }
}

async function loadProfile() {
  status.value = 'loading'
  errorMessage.value = ''
  imageLoadFailed.value = false

  try {
    profile.value = await getMyProfile()
    status.value = 'success'
  } catch (error) {
    profile.value = null
    status.value = 'error'
    errorMessage.value =
      error?.response?.data?.message || '회원 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}

onMounted(loadProfile)
</script>

<template>
  <DefaultLayout>
    <main class="profile-page" aria-labelledby="profile-title">
      <header class="profile-heading">
        <p>MY PAGE</p>
        <h1 id="profile-title">마이페이지</h1>
        <span>가입한 회원정보를 확인하고 계정을 관리할 수 있습니다.</span>
      </header>

      <section v-if="status === 'loading'" class="profile-state" role="status" aria-live="polite">
        <span class="loading-indicator" aria-hidden="true" />
        회원 정보를 불러오고 있습니다.
      </section>

      <section v-else-if="status === 'error'" class="profile-state profile-state--error" role="alert">
        <strong>회원정보를 표시할 수 없습니다.</strong>
        <p>{{ errorMessage }}</p>
        <button type="button" @click="loadProfile">다시 시도</button>
      </section>

      <section v-else class="profile-content" aria-label="내 회원정보">
        <aside class="profile-summary">
          <img
            class="profile-image"
            :src="profileImageUrl"
            alt=""
            width="148"
            height="148"
            @error="imageLoadFailed = true"
          />
          <strong>{{ displayValue(profile.nickname) }}</strong>
          <span>{{ profile.email }}</span>
          <small>이메일과 프로필 이미지는 현재 읽기 전용입니다.</small>
        </aside>

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
                <button type="button" @click="openPasswordChange">비밀번호 변경</button>
                <button class="primary-action" type="button" @click="startEditing">수정하기</button>
              </template>
            </div>
          </div>

          <dl>
            <div>
              <dt>이름</dt>
              <dd v-if="!isEditing">{{ displayValue(profile.name) }}</dd>
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
              <dd v-if="!isEditing">{{ displayValue(profile.nickname) }}</dd>
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
              <dd v-if="!isEditing">{{ displayValue(profile.phoneNumber) }}</dd>
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
              <button class="withdraw-open-button" type="button" @click="openWithdrawal">
                회원탈퇴
              </button>
            </template>
          </footer>
        </form>
      </section>
    </main>

    <BaseModal
      :open="isPasswordModalOpen"
      title="비밀번호 변경"
      description="현재 비밀번호를 확인한 뒤 새 비밀번호로 변경합니다."
      :close-on-overlay="!isChangingPassword"
      :close-on-escape="!isChangingPassword"
      @close="closePasswordChange"
    >
      <form id="change-password-form" class="account-modal-form" @submit.prevent="submitPasswordChange">
        <label for="current-password">현재 비밀번호</label>
        <input
          id="current-password"
          v-model="passwordForm.currentPassword"
          type="password"
          autocomplete="current-password"
          maxlength="72"
          :disabled="isChangingPassword"
        />

        <label for="new-password">새 비밀번호</label>
        <input
          id="new-password"
          v-model="passwordForm.newPassword"
          type="password"
          autocomplete="new-password"
          minlength="10"
          maxlength="72"
          :disabled="isChangingPassword"
        />

        <label for="new-password-confirm">새 비밀번호 확인</label>
        <input
          id="new-password-confirm"
          v-model="passwordForm.newPasswordConfirm"
          type="password"
          autocomplete="new-password"
          minlength="10"
          maxlength="72"
          :disabled="isChangingPassword"
          :aria-invalid="Boolean(passwordError)"
        />
        <p v-if="passwordError" class="account-modal-error" role="alert">{{ passwordError }}</p>
      </form>
      <template #footer>
        <button
          type="button"
          class="modal-cancel-button"
          :disabled="isChangingPassword"
          @click="closePasswordChange"
        >
          취소
        </button>
        <button
          type="submit"
          form="change-password-form"
          class="modal-primary-button"
          :disabled="isChangingPassword"
        >
          {{ isChangingPassword ? '변경 중' : '변경하기' }}
        </button>
      </template>
    </BaseModal>

    <BaseModal
      :open="isWithdrawalOpen"
      title="회원탈퇴"
      description="탈퇴 후에는 현재 계정으로 다시 로그인할 수 없습니다."
      :close-on-overlay="!isWithdrawing"
      :close-on-escape="!isWithdrawing"
      @close="closeWithdrawal"
    >
      <form
        id="withdraw-account-form"
        class="account-modal-form withdraw-form"
        @submit.prevent="submitWithdrawal"
      >
        <p>본인 확인을 위해 현재 비밀번호를 입력해 주세요.</p>
        <label for="withdrawal-password">현재 비밀번호</label>
        <input
          id="withdrawal-password"
          v-model="withdrawalPassword"
          type="password"
          autocomplete="current-password"
          maxlength="72"
          :disabled="isWithdrawing"
          :aria-invalid="Boolean(withdrawalError)"
        />
        <p v-if="withdrawalError" class="withdrawal-error" role="alert">
          {{ withdrawalError }}
        </p>
      </form>
      <template #footer>
        <button type="button" class="modal-cancel-button" :disabled="isWithdrawing" @click="closeWithdrawal">
          취소
        </button>
        <button
          type="submit"
          form="withdraw-account-form"
          class="modal-withdraw-button"
          :disabled="isWithdrawing"
        >
          {{ isWithdrawing ? '처리 중' : '탈퇴하기' }}
        </button>
      </template>
    </BaseModal>
  </DefaultLayout>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - var(--layout-header-height));
  padding: 56px max(var(--layout-gutter), calc((100% - var(--layout-content-width)) / 2)) 88px;
  background:
    radial-gradient(circle at 90% 5%, rgb(249 115 22 / 10%), transparent 28rem),
    var(--color-page);
}

.profile-heading > p,
.details-heading p {
  margin: 0;
  color: var(--color-brand-accent);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.14em;
}

.profile-heading h1 {
  margin: 8px 0 0;
  color: var(--color-text);
  font-size: clamp(32px, 4vw, 42px);
  letter-spacing: -0.04em;
}

.profile-heading > span {
  display: block;
  margin-top: 10px;
  color: var(--color-text-muted);
}

.profile-content {
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: 24px;
  margin-top: 34px;
}

.profile-summary,
.profile-details,
.profile-state {
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: 0 16px 45px rgb(15 23 42 / 6%);
}

.profile-summary {
  display: flex;
  min-height: 410px;
  flex-direction: column;
  align-items: center;
  padding: 42px 24px 28px;
  text-align: center;
}

.profile-image {
  width: 148px;
  height: 148px;
  border: 6px solid var(--color-brand-soft);
  border-radius: 50%;
  object-fit: cover;
}

.profile-summary strong {
  margin-top: 22px;
  color: var(--color-text);
  font-size: 21px;
}

.profile-summary > span {
  margin-top: 6px;
  color: var(--color-text-muted);
  font-size: 13px;
  overflow-wrap: anywhere;
}

.profile-summary small {
  width: 100%;
  margin-top: auto;
  padding-top: 24px;
  color: var(--color-text-muted);
  border-top: 1px solid var(--color-border);
  line-height: 1.6;
}

.profile-details { padding: 34px; }

.details-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--color-border);
}

.details-heading h2 {
  margin: 6px 0 0;
  color: var(--color-text);
  font-size: 25px;
}

.details-heading a,
.details-heading button,
.profile-state button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  color: var(--color-brand);
  border: 1px solid var(--color-brand-border);
  border-radius: 10px;
  background: var(--color-brand-soft);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.details-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.details-heading .primary-action {
  color: var(--color-brand-on);
  border-color: var(--color-brand);
  background: var(--color-brand);
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
  color: var(--color-text);
  font-weight: 750;
  overflow-wrap: anywhere;
}

.profile-input {
  width: 100%;
  min-height: 43px;
  margin-top: 8px;
  padding: 0 12px;
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: 9px;
  background: var(--color-surface);
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
  color: var(--color-text-muted);
  border-radius: 12px;
  background: var(--color-brand-soft);
  font-size: 12px;
  line-height: 1.6;
}

.withdraw-open-button {
  flex: 0 0 auto;
  padding: 7px 10px;
  color: var(--color-danger);
  border: 1px solid rgb(185 28 28 / 24%);
  border-radius: 8px;
  background: var(--color-surface);
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
}

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

.withdrawal-error,
.account-modal-error {
  margin: 10px 0 0;
  color: var(--color-danger);
  font-size: 12px;
}

.modal-cancel-button,
.modal-primary-button,
.modal-withdraw-button {
  min-height: 40px;
  padding: 0 15px;
  border-radius: 9px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.modal-cancel-button {
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}

.modal-withdraw-button {
  color: #fff;
  border: 1px solid var(--color-danger);
  background: var(--color-danger);
}

.modal-primary-button {
  color: var(--color-brand-on);
  border: 1px solid var(--color-brand);
  background: var(--color-brand);
}

.modal-cancel-button:disabled,
.modal-primary-button:disabled,
.modal-withdraw-button:disabled {
  cursor: wait;
  opacity: 0.55;
}

.profile-state {
  display: flex;
  min-height: 300px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 34px;
  padding: 40px;
  color: var(--color-text-muted);
  text-align: center;
}

.profile-state p { margin: 0; }
.profile-state--error strong { color: var(--color-danger); }

.loading-indicator {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-brand-border);
  border-top-color: var(--color-brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 760px) {
  .profile-page { padding-top: 38px; }
  .profile-content { grid-template-columns: 1fr; }
  .profile-summary { min-height: 360px; }
  .profile-details { padding: 24px 18px; }
  .details-heading { align-items: flex-start; flex-direction: column; }
  .profile-details dl { grid-template-columns: 1fr; }
  .details-footer { align-items: flex-start; flex-direction: column; }
}
</style>
