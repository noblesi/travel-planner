<template>
  <DefaultLayout>
    <div class="invite-page">
      <div class="invite-card">
        <template v-if="!sent">
          <div class="eyebrow">INVITE TRAVELERS</div>
          <div class="invite-title">초대할 친구의 이메일을 적어주세요.</div>

          <div class="input-row">
            <input class="email-input" type="email" v-model="emailInput" placeholder="name@email.com"
              @keyup.enter="addEmail" />
            <button class="add-btn" @click="addEmail">추가</button>
          </div>
          <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>

          <div class="email-list">
            <div v-for="(email, idx) in invitedEmails" :key="email" class="email-item">
              <div class="email-avatar">{{ email[0].toUpperCase() }}</div>
              <span class="email-text">{{ email }}</span>
              <button class="remove-btn" @click="removeEmail(idx)">✕</button>
            </div>
          </div>

          <button class="send-btn" :disabled="invitedEmails.length === 0" @click="sendInvites">
            <i class="ti ti-send" aria-hidden="true"></i> 링크 발송
          </button>
        </template>

        <template v-else>
          <div class="success-icon">✓</div>
          <div class="success-title">링크 발송이 완료되었습니다.</div>
          <div class="success-sub">초대링크의 유효시간은 24시간입니다.</div>
          <button class="back-btn" @click="goBackToPlan">돌아가기</button>
        </template>
      </div>
    </div>
  </DefaultLayout>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

// 이 페이지는 /plans/:id/invite 라우트에 props: true로 연결되어 있어
// planId를 prop으로 받는다. "돌아가기" 버튼에서 편집 화면으로 갈 때 사용한다.
const props = defineProps({
  id: { type: [String, Number], required: false, default: null },
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
const errorMessage = ref('')
const sent = ref(false)

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function addEmail() {
  const value = emailInput.value.trim()
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

  invitedEmails.value.push(value)
  emailInput.value = ''
}

function removeEmail(idx) {
  invitedEmails.value.splice(idx, 1)
}

function sendInvites() {
  // TODO: 백엔드 연동 시 API 호출 (초대 이메일 일괄 발송, JWT 토큰 생성)
  sent.value = true
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.invite-page {
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  /* DefaultLayout이 헤더/푸터를 더하므로 min-height:100vh를 그대로 쓰면
     전체 페이지가 뷰포트보다 길어져 위아래 여백이 과해진다.
     콘텐츠 크기 + 적당한 여백만 갖도록 padding으로만 여백을 준다. */
  padding: 4rem 2rem;
}

.invite-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #eee;
  width: 620px;
  max-width: 100%;
  padding: 3rem 3.5rem 3.5rem;
  text-align: center;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: .1em;
  color: #999;
  text-transform: uppercase;
  margin-bottom: .75rem;
  text-align: left;
}

.invite-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 2rem;
  text-align: left;
}

.input-row {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
}

.email-input {
  flex: 1;
  padding: 15px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 28px;
  font-size: 15px;
  outline: none;
}

.email-input:focus {
  border-color: #0f766e;
}

.add-btn {
  padding: 15px 30px;
  background: #0f766e;
  color: #fff;
  border: none;
  border-radius: 28px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.add-btn:hover {
  background: #0c5c56;
}

.error-text {
  font-size: 13px;
  color: #0f766e;
  text-align: left;
  margin-bottom: 12px;
}

.email-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 1.5rem 0 2rem;
  text-align: left;
}

.email-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 18px;
  background: #fafafa;
  border-radius: 12px;
}

.email-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #d9f0ed;
  color: #0f766e;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.email-text {
  flex: 1;
  font-size: 15px;
  color: #333;
}

.remove-btn {
  background: none;
  border: none;
  color: #bbb;
  cursor: pointer;
  font-size: 15px;
  padding: 4px;
}

.remove-btn:hover {
  color: #0f766e;
}

.send-btn {
  width: 100%;
  padding: 16px;
  background: #0f766e;
  color: #fff;
  border: none;
  border-radius: 28px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.send-btn:hover {
  background: #0c5c56;
}

.send-btn:disabled {
  background: #e0e0e0;
  color: #bbb;
  cursor: not-allowed;
}

.success-icon {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  background: #d9f0ed;
  color: #0f766e;
  font-size: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
}

.success-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.success-sub {
  font-size: 14px;
  color: #999;
  margin-bottom: 2rem;
}

.back-btn {
  width: 100%;
  padding: 15px;
  background: #fff;
  color: #555;
  border: 1px solid #e0e0e0;
  border-radius: 28px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.back-btn:hover {
  border-color: #ccc;
  background: #fafafa;
}
</style>