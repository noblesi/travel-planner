<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { createPlanInvitations } from '@/api/invitations'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

const props = defineProps({
  id: { type: [String, Number], required: true },
})

const router = useRouter()

const PLAN_EDIT_ROUTE_NAME = 'plan-editor'

function goBackToPlan() {
  // 1순위: 브라우저 히스토리로 돌아가기.
  // "편집 화면 → 동행자 초대 버튼 → 이 페이지"로 들어온 일반적인 경우,
  // 이전 화면(스크롤 위치 포함)으로 정확히 되돌아간다.
  //
  // 2순위(폴백): 히스토리가 없는 경우 — 예를 들어 초대 이메일 안의 링크를 통해
  // 이 페이지로 "새로 진입"한 경우(뒤로 갈 히스토리 자체가 없음) — 에는
  // planId를 이용해 편집 화면으로 직접 이동한다.
  // window.history.state.back이 null이면 이 탭에서 뒤로 갈 히스토리가 없다는 뜻이다.
  const hasHistory = window.history.state && window.history.state.back !== null

  if (hasHistory) {
    router.back()
    return
  }

  if (PLAN_EDIT_ROUTE_NAME && props.id) {
    // plan-editor 라우트는 파라미터명이 planId이다 (/plans/:planId/edit).
    router.push({ name: PLAN_EDIT_ROUTE_NAME, params: { planId: props.id } })
  } else {
    // 편집 화면 라우트가 아직 없으므로 임시로 탐색 페이지로 보낸다.
    router.push({ name: 'plan-search' })
  }
}

const emailInput = ref('')
const invitedEmails = ref([])
const createdInvitations = ref([])
const submitting = ref(false)
const errorMessage = ref('')
const copiedInvitationId = ref(null)

const hasCreatedInvitations = computed(() => createdInvitations.value.length > 0)
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function addEmail() {
  const value = emailInput.value.trim().toLowerCase()
  errorMessage.value = ''

  if (!value) return
  if (!emailPattern.test(value)) {
    errorMessage.value = '올바른 이메일 형식이 아니에요.'
    return
  }
  if (invitedEmails.value.includes(value)) {
    errorMessage.value = '이미 추가된 이메일이에요.'
    return
  }
  if (invitedEmails.value.length >= 20) {
    errorMessage.value = '한 번에 최대 20명까지 초대할 수 있어요.'
    return
  }

  invitedEmails.value.push(value)
  emailInput.value = ''
}

function removeEmail(index) {
  invitedEmails.value.splice(index, 1)
}

function invitationLink(token) {
  const path = router.resolve({ name: 'invite-accept', query: { token } }).href
  return new URL(path, window.location.origin).toString()
}

function apiErrorMessage(error) {
  if (error?.response?.status === 401) return '로그인 후 동행자를 초대할 수 있어요.'

  const message = error?.response?.data?.message
  return typeof message === 'string' && message
    ? message
    : '초대 링크를 만들지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

async function createLinks() {
  if (submitting.value || invitedEmails.value.length === 0) return

  submitting.value = true
  errorMessage.value = ''
  copiedInvitationId.value = null

  try {
    const data = await createPlanInvitations(props.id, {
      inviteeEmails: invitedEmails.value,
    })
    createdInvitations.value = data.invitations.map((invitation) => ({
      ...invitation,
      link: invitationLink(invitation.token),
    }))
  } catch (error) {
    errorMessage.value = apiErrorMessage(error)
  } finally {
    submitting.value = false
  }
}

async function copyLink(invitation) {
  errorMessage.value = ''

  try {
    if (!navigator.clipboard?.writeText) throw new Error('Clipboard API unavailable')
    await navigator.clipboard.writeText(invitation.link)
    copiedInvitationId.value = invitation.invitationId
  } catch {
    errorMessage.value = '링크를 복사하지 못했습니다. 링크를 직접 선택해 복사해 주세요.'
  }
}

function createMore() {
  createdInvitations.value = []
  invitedEmails.value = []
  copiedInvitationId.value = null
  errorMessage.value = ''
}

function goBackToPlan() {
  router.push({ name: 'plan-editor', params: { planId: String(props.id) } })
}
</script>

<template>
  <DefaultLayout>
    <div class="invite-page">
      <section class="invite-card" aria-labelledby="invite-title">
        <template v-if="!hasCreatedInvitations">
          <p class="eyebrow">INVITE TRAVELERS</p>
          <h1 id="invite-title" class="invite-title">함께 여행할 동행자를 초대하세요.</h1>
          <p class="invite-description">
            이메일을 추가하면 24시간 동안 사용할 수 있는 초대 링크를 생성합니다.
          </p>

          <form class="input-row" @submit.prevent="addEmail">
            <label class="sr-only" for="invite-email">초대할 이메일</label>
            <input
              id="invite-email"
              v-model="emailInput"
              class="email-input"
              type="email"
              placeholder="name@email.com"
              autocomplete="email"
              :disabled="submitting"
            />
            <button class="add-btn" type="submit" :disabled="submitting">추가</button>
          </form>
          <p v-if="errorMessage" class="error-text" role="alert">{{ errorMessage }}</p>

          <ul v-if="invitedEmails.length" class="email-list" aria-label="초대 대상">
            <li v-for="(email, index) in invitedEmails" :key="email" class="email-item">
              <span class="email-avatar" aria-hidden="true">{{ email[0].toUpperCase() }}</span>
              <span class="email-text">{{ email }}</span>
              <button
                class="remove-btn"
                type="button"
                :aria-label="`${email} 삭제`"
                :disabled="submitting"
                @click="removeEmail(index)"
              >
                ×
              </button>
            </li>
          </ul>

          <button
            class="send-btn"
            type="button"
            :disabled="submitting || invitedEmails.length === 0"
            :aria-busy="submitting"
            @click="createLinks"
          >
            {{ submitting ? '링크 생성 중...' : '초대 링크 생성' }}
          </button>
          <button class="back-btn" type="button" :disabled="submitting" @click="goBackToPlan">
            플랜으로 돌아가기
          </button>
        </template>

        <template v-else>
          <div class="success-icon" aria-hidden="true">✓</div>
          <h1 id="invite-title" class="success-title">초대 링크를 만들었습니다.</h1>
          <p class="success-sub">각 링크는 생성 시점부터 24시간 동안 유효합니다.</p>

          <ul class="link-list" aria-label="생성된 초대 링크">
            <li
              v-for="invitation in createdInvitations"
              :key="invitation.invitationId"
              class="link-item"
            >
              <strong>{{ invitation.inviteeEmail }}</strong>
              <div class="link-row">
                <input
                  :value="invitation.link"
                  type="text"
                  readonly
                  :aria-label="`${invitation.inviteeEmail} 초대 링크`"
                  @focus="$event.target.select()"
                />
                <button type="button" @click="copyLink(invitation)">
                  {{ copiedInvitationId === invitation.invitationId ? '복사됨' : '복사' }}
                </button>
              </div>
            </li>
          </ul>
          <p v-if="errorMessage" class="error-text" role="alert">{{ errorMessage }}</p>

          <div class="success-actions">
            <button class="back-btn" type="button" @click="createMore">추가 링크 생성</button>
            <button class="send-btn" type="button" @click="goBackToPlan">플랜으로 돌아가기</button>
          </div>
        </template>
      </section>
    </div>
  </DefaultLayout>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.invite-page {
  display: flex;
  min-height: 70vh;
  align-items: center;
  justify-content: center;
  padding: 4rem 2rem;
  background: #fafafa;
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
}

.invite-card {
  width: 680px;
  max-width: 100%;
  padding: 3rem 3.5rem;
  border: 1px solid #eee;
  border-radius: 16px;
  background: #fff;
}

.eyebrow {
  margin: 0 0 0.75rem;
  color: #999;
  font-size: 12px;
  letter-spacing: 0.1em;
}

.invite-title,
.success-title {
  margin: 0;
  color: #1a1a1a;
  font-size: 24px;
}

.invite-description,
.success-sub {
  margin: 0.75rem 0 2rem;
  color: #777;
  font-size: 14px;
  line-height: 1.6;
}

.input-row,
.link-row,
.success-actions {
  display: flex;
  gap: 10px;
}

.email-input,
.link-row input {
  min-width: 0;
  flex: 1;
  padding: 14px 18px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
}

.email-input:focus,
.link-row input:focus {
  border-color: #ff5a4e;
  outline: 3px solid rgb(255 90 78 / 12%);
}

.add-btn,
.send-btn,
.back-btn,
.link-row button {
  min-height: 44px;
  padding: 0 22px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.add-btn,
.send-btn,
.link-row button {
  color: #fff;
  border: 1px solid #ff5a4e;
  background: #ff5a4e;
}

.back-btn {
  width: 100%;
  margin-top: 10px;
  color: #555;
  border: 1px solid #e0e0e0;
  background: #fff;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.error-text {
  margin: 10px 0 0;
  color: #b91c1c;
  font-size: 13px;
}

.email-list,
.link-list {
  display: grid;
  gap: 12px;
  margin: 1.5rem 0 2rem;
  padding: 0;
  list-style: none;
}

.email-item {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 13px 18px;
  border-radius: 12px;
  background: #fafafa;
}

.email-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  place-items: center;
  color: #e8443a;
  border-radius: 50%;
  background: #fff0ee;
  font-weight: 700;
}

.email-text {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: #333;
  text-overflow: ellipsis;
}

.remove-btn {
  padding: 4px;
  color: #999;
  border: 0;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
}

.send-btn {
  width: 100%;
}

.success-icon {
  display: grid;
  width: 72px;
  height: 72px;
  margin: 0 auto 1.5rem;
  place-items: center;
  color: #e8443a;
  border-radius: 50%;
  background: #fff0ee;
  font-size: 32px;
}

.success-title,
.success-sub {
  text-align: center;
}

.link-item {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid #eee;
  border-radius: 12px;
  background: #fafafa;
}

.link-item strong {
  color: #333;
  font-size: 13px;
}

.success-actions .back-btn {
  margin-top: 0;
}

.success-actions > * {
  flex: 1;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 640px) {
  .invite-page {
    padding: 2rem 1rem;
  }

  .invite-card {
    padding: 2rem 1.25rem;
  }

  .input-row,
  .link-row,
  .success-actions {
    flex-direction: column;
  }
}
</style>
