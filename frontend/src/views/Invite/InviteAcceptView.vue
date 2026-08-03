<template>
  <div class="accept-page">
    <div class="accept-header">
      <span class="logo"><i class="ti ti-infinity" aria-hidden="true"></i> WithTrip</span>
    </div>

    <div class="accept-body">
      <div class="accept-card">

        <!-- 로딩: 토큰 검증 중 -->
        <template v-if="status === 'loading'">
          <div class="loading-spinner" aria-hidden="true"></div>
          <div class="loading-text">초대 정보를 확인하고 있어요...</div>
        </template>

        <template v-else-if="status === 'valid'">
          <div class="inviter-row">
            <div class="inviter-avatar" aria-hidden="true">W</div>
            <span class="inviter-text"><strong>WithTrip</strong> 동행자 초대예요.</span>
          </div>

          <h1 class="accept-title">"{{ invitation.planTitle }}"에 함께 하시겠어요?</h1>

          <div class="info-box">
            <div class="info-row">
              <span class="info-label">여행 날짜</span>
              <span class="info-value">{{ invitation.startDate }} ~ {{ invitation.endDate }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">여행 지역</span>
              <span class="info-value">{{ invitation.regionName }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">초대 이메일</span>
              <span class="info-value">{{ invitation.inviteeEmail }}</span>
            </div>
          </div>

          <button
            class="accept-btn"
            type="button"
            :disabled="accepting"
            :aria-busy="accepting"
            @click="acceptCurrentInvitation"
          >
            {{ accepting ? '처리 중...' : '초대 수락하고 여행 계획 열기' }}
          </button>
          <div v-if="errorMessage" class="error-text" role="alert">{{ errorMessage }}</div>
        </template>

        <!-- 만료: 링크 만료 -->
        <template v-else-if="status === 'expired'">
          <div class="expired-icon"><i class="ti ti-clock-exclamation" aria-hidden="true"></i></div>
          <div class="expired-title">링크가 만료됐어요</div>
          <div class="expired-sub">초대 링크는 24시간만 유효해요</div>
          <button class="home-btn" @click="goHome">홈으로 가기</button>
        </template>

        <template v-else-if="status === 'error'">
          <div class="expired-icon"><i class="ti ti-alert-triangle" aria-hidden="true"></i></div>
          <div class="expired-title">{{ errorMessage || '초대를 확인할 수 없어요' }}</div>
          <div class="expired-sub">링크를 다시 확인해주시거나, 초대한 분께 문의해주세요</div>
          <button class="home-btn" @click="goHome">홈으로 가기</button>
        </template>

      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { acceptPlanInvitation, getPlanInvitation } from '@/api/invitations'

const route = useRoute()
const router = useRouter()

const status = ref('loading')
const accepting = ref(false)
const errorMessage = ref('')
const invitation = ref(null)

async function verifyToken() {
  const token = route.query.token

  if (!token) {
    status.value = 'error'
    errorMessage.value = '잘못된 초대 링크예요.'
    return
  }

  status.value = 'loading'
  errorMessage.value = ''

  try {
    invitation.value = await getPlanInvitation(token)
    status.value = 'valid'
  } catch (error) {
    if (error?.response?.status === 410) {
      status.value = 'expired'
      return
    }
    status.value = 'error'
    errorMessage.value =
      error?.response?.data?.message ?? '초대 정보를 불러오지 못했어요.'
  }
}

async function acceptCurrentInvitation() {
  if (accepting.value) return

  accepting.value = true
  errorMessage.value = ''

  try {
    const data = await acceptPlanInvitation(route.query.token)
    router.push({ name: 'plan-editor', params: { planId: data.planId } })
  } catch (error) {
    if (error?.response?.status === 401) {
      router.push({
        name: 'login',
        query: { redirect: route.fullPath },
      })
      return
    }
    if (error?.response?.status === 410) {
      status.value = 'expired'
      return
    }
    errorMessage.value =
      error?.response?.data?.message ?? '수락 처리에 실패했어요. 다시 시도해 주세요.'
  } finally {
    accepting.value = false
  }
}

function goHome() {
  router.push({ name: 'home' })
}

onMounted(verifyToken)
</script>

<style scoped>
* { box-sizing: border-box; }

.accept-page {
  min-height: 100vh;
  background:
    linear-gradient(rgba(255, 245, 235, .55), rgba(255, 235, 220, .7)),
    linear-gradient(135deg, #fbe4c8 0%, #f7d6b8 40%, #f0c8a8 100%);
  display: flex;
  flex-direction: column;
}

.accept-header {
  padding: 1.25rem 2rem;
  background: rgba(255, 255, 255, .6);
  backdrop-filter: blur(4px);
}

.logo {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-brand);
  display: flex;
  align-items: center;
  gap: 6px;
}

.accept-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 2rem;
}

.accept-card {
  background: #fff;
  border-radius: 20px;
  padding: 2.5rem 2.75rem;
  width: 460px;
  max-width: 100%;
  box-shadow: 0 20px 50px rgba(0, 0, 0, .1);
  text-align: center;
}

/* 로딩 상태 */
.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0e0de;
  border-top-color: var(--color-brand-accent);
  border-radius: 50%;
  margin: 0 auto 1.25rem;
  animation: spin .8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-text {
  font-size: 14px;
  color: #999;
}

/* 정상 상태 */
.inviter-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 1.5rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #f0f0f0;
}
.inviter-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background-color: #1DA97C;
  background-size: cover;
  background-position: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.inviter-text {
  font-size: 14px;
  color: #666;
}
.inviter-text strong {
  color: #1a1a1a;
}

.accept-title {
  font-size: 21px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1.4;
  margin-bottom: 1.75rem;
}

.info-box {
  background: #fafafa;
  border-radius: 12px;
  padding: 1.1rem 1.4rem;
  margin-bottom: 1.75rem;
  text-align: left;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
}
.info-label {
  font-size: 13px;
  color: #999;
}
.info-value {
  font-size: 13.5px;
  color: #333;
  font-weight: 600;
}

.participant-avatars {
  display: flex;
}
.participant-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #fafafa;
  margin-left: -8px;
}
.participant-avatar:first-child {
  margin-left: 0;
}

.accept-btn {
  width: 100%;
  padding: 14px;
  background: var(--color-brand);
  color: #fff;
  border: none;
  border-radius: 26px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.accept-btn:hover {
  background: var(--color-brand-hover);
}
.accept-btn:disabled {
  background: #e0b8b0;
  cursor: not-allowed;
}

.error-text {
  font-size: 12.5px;
  color: var(--color-danger);
  margin-top: 10px;
}

/* 만료/오류 상태 */
.expired-icon {
  font-size: 40px;
  color: var(--color-brand-accent);
  margin-bottom: 1.25rem;
}
.expired-title {
  font-size: 19px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}
.expired-sub {
  font-size: 13.5px;
  color: #999;
  margin-bottom: 1.75rem;
}
.home-btn {
  width: 100%;
  padding: 14px;
  background: var(--color-brand);
  color: #fff;
  border: none;
  border-radius: 26px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.home-btn:hover {
  background: var(--color-brand-hover);
}
</style>
