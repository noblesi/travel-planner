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

        <!-- 정상: 초대 정보 표시 -->
        <template v-else-if="status === 'valid'">
          <div class="inviter-row">
            <div class="inviter-avatar" :style="invitation.inviterAvatar ? { backgroundImage: `url(${invitation.inviterAvatar})` } : {}">
              <span v-if="!invitation.inviterAvatar">{{ invitation.inviterName?.[0] }}</span>
            </div>
            <span class="inviter-text"><strong>{{ invitation.inviterName }}</strong>님의 초대예요.</span>
          </div>

          <h1 class="accept-title">"{{ invitation.planTitle }}"에 함께 하시겠어요?</h1>

          <div class="info-box">
            <div class="info-row">
              <span class="info-label">여행 날짜</span>
              <span class="info-value">{{ invitation.startDate }} ~ {{ invitation.endDate }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">참여 인원</span>
              <div class="participant-avatars">
                <span
                  v-for="p in invitation.participants"
                  :key="p.id"
                  class="participant-avatar"
                  :style="{ background: p.color }"
                  :title="p.name"
                >{{ p.name[0] }}</span>
              </div>
            </div>
          </div>

          <button class="accept-btn" :disabled="accepting" @click="acceptInvitation">
            {{ accepting ? '처리 중...' : '여행 계획 열기' }}
          </button>
          <div v-if="errorMessage" class="error-text">{{ errorMessage }}</div>
        </template>

        <!-- 만료: 링크 만료 -->
        <template v-else-if="status === 'expired'">
          <div class="expired-icon"><i class="ti ti-clock-exclamation" aria-hidden="true"></i></div>
          <div class="expired-title">링크가 만료됐어요</div>
          <div class="expired-sub">초대 링크는 24시간만 유효해요</div>
          <button class="home-btn" @click="goHome">홈으로 가기</button>
        </template>

        <!-- 오류: 그 외 실패(잘못된 토큰, 이미 수락됨 등) -->
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// status: 'loading' | 'valid' | 'expired' | 'error'
const status = ref('loading')
const accepting = ref(false)
const errorMessage = ref('')

// ── mock 데이터: 백엔드 연동 시 토큰 검증 API 응답으로 교체 ──
const invitation = ref({
  inviterName: '홍길동',
  inviterAvatar: '',
  planTitle: '제주도 여행',
  startDate: '2026.07.16',
  endDate: '2026.08.27',
  participants: [
    { id: 1, name: '홍길동', color: '#1DA97C' },
    { id: 2, name: '김철수', color: '#D94530' },
  ],
})

async function verifyToken() {
  const token = route.query.token

  if (!token) {
    status.value = 'error'
    errorMessage.value = '잘못된 초대 링크예요'
    return
  }

  status.value = 'loading'

  // TODO: 백엔드 연동 시 아래를 실제 API 호출로 교체
  //   GET /api/invitations/verify?token={token}
  //   응답: { valid: true, planTitle, inviterName, inviterAvatar, startDate, endDate, participants }
  //   또는 만료/오류 시 { valid: false, reason: 'EXPIRED' | 'INVALID' | 'ALREADY_ACCEPTED' }
  try {
    await new Promise((resolve) => setTimeout(resolve, 600)) // 로딩 상태 확인용 지연, 실제 연동 시 제거

    // mock: 토큰 값에 "expired"가 포함되면 만료 상태를 재현할 수 있게 해둔다.
    // 예: /invite/accept?token=expired-token 으로 접속해보면 만료 화면을 확인할 수 있다.
    if (String(token).includes('expired')) {
      status.value = 'expired'
      return
    }

    status.value = 'valid'
  } catch (err) {
    // 실제 API 연동 시 네트워크 오류나 서버 오류 원인을 파악할 수 있도록 콘솔에 남긴다.
    console.error('초대 토큰 검증 실패:', err)
    status.value = 'error'
    errorMessage.value = '초대 정보를 불러오지 못했어요'
  }
}

async function acceptInvitation() {
  accepting.value = true
  errorMessage.value = ''

  // TODO: 백엔드 연동 시 아래를 실제 API 호출로 교체
  //   POST /api/invitations/accept  { token }
  //
  //   서버가 로그인 여부를 판단해서:
  //   - 로그인 안 되어 있으면 401 응답 → 아래 catch에서 로그인 페이지로 리다이렉트
  //   - 로그인 되어 있고 정상 처리되면 200 + { planId } 응답
  //     (이미 참여 중인 경우도 서버가 중복 추가 없이 그냥 planId를 내려준다)
  try {
    await new Promise((resolve) => setTimeout(resolve, 500))
    // mock: 실제 axios 사용 시 아래처럼 응답 상태에 따라 분기한다.
    //   const res = await api.post('/invitations/accept', { token: route.query.token })
    //   router.push({ name: 'plan-detail', params: { id: res.data.planId } })
    router.push({ name: 'plan-detail', params: { id: 1 } }) // TODO: 실제 planId로 교체
  } catch (err) {
    // 서버가 401(비로그인)을 내려주면, 로그인 페이지로 보내되
    // 로그인 성공 후 이 초대 수락 흐름으로 다시 돌아올 수 있도록
    // 현재 경로(토큰 포함)를 redirect 쿼리로 함께 넘긴다.
    if (err?.response?.status === 401) {
      router.push({
        name: 'login',
        query: { redirect: route.fullPath },
      })
      return
    }

    console.error('초대 수락 처리 실패:', err)
    errorMessage.value = '수락 처리에 실패했어요. 다시 시도해주세요.'
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
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
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
  color: #D94530;
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
  border-top-color: #D94530;
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
  background: #D94530;
  color: #fff;
  border: none;
  border-radius: 26px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.accept-btn:hover {
  background: #c23d2b;
}
.accept-btn:disabled {
  background: #e0b8b0;
  cursor: not-allowed;
}

.error-text {
  font-size: 12.5px;
  color: #D94530;
  margin-top: 10px;
}

/* 만료/오류 상태 */
.expired-icon {
  font-size: 40px;
  color: #D94530;
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
  background: #D94530;
  color: #fff;
  border: none;
  border-radius: 26px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}
.home-btn:hover {
  background: #c23d2b;
}
</style>