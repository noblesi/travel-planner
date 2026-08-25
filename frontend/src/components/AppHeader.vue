<script setup>
import { ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import alarmImg from '@/assets/headerImage/bell-icon.png'
import headerLogoUrl from '@/assets/branding/travel-planner-logo-symbol.webp'
import { useAuthStore } from '@/stores/auth'
import { getAlarmList } from '@/api/alarm'
import { getAlarmCheck } from '@/api/alarm'

const alarms = ref([])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isMenuOpen = ref(false)

const isAlarmOpen = ref(false)

const toggleAlarm = () => {
  isAlarmOpen.value = !isAlarmOpen.value
}

const closeMenu = () => {
  isMenuOpen.value = false
  isAlarmOpen.value = false
}

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

watch(() => route.fullPath, closeMenu)

const handlePlanSearchClick = () => {
  closeMenu()
  if (route.name === 'plan-search') {
    window.dispatchEvent(new Event('plan-search:reset'))
  }
}

const handleLogout = async () => {
  try {
    await authStore.logout()
    closeMenu()
    await router.push({ name: 'home' })
  } catch {
    // Store에 사용자에게 표시할 오류를 유지하고 현재 화면에 머뭅니다.
  }
}
const handleMypage = () => {
  console.log(authStore.currentUser)
 // router.push({name : 'myPage'})
}

const fetchAlarmList = () => {
  if (authStore.isAuthenticated) {
    getAlarmList().then((response) => {
        console.log('알림 목록 가져오기 성공:', response)
        if(response != null){
          alarms.value = response
        } else {
          alarms.value = "알람이 없습니다."
        }
        
      })
      .catch((error) => {
        console.error('알림 목록 가져오기 실패:', error)
      })
  }
}

watch(() => authStore.isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      fetchAlarmList()
    } else {
      alarms.value = [] 
    }
  },
  { immediate: true }
)
const selectedAlarm = ref(null)

// 알림 클릭 시 상세 모달 열기
const openAlarmDetail = (alarm) => {
  selectedAlarm.value = alarm
  
  const notificationId = alarm.notificationId
  if (notificationId) {
    getAlarmCheck(notificationId)
      .then(() => {
        alarm.isRead = true
      })
      .catch((err) => console.error('알림 읽음 처리 실패:', err))
  }
  
}

// 모달 닫기
const closeAlarmDetail = () => {
  selectedAlarm.value = null
}
</script>

<template>
  <header class="header" @keydown.esc="closeMenu">
    <div class="app-container header__inner">
      <RouterLink class="brand" :to="{ name: 'home' }" aria-label="WithTrip 홈" @click="closeMenu">
        <img class="brand__mark" :src="headerLogoUrl" alt="" width="40" height="40" />
        <span>WithTrip</span>
      </RouterLink>

      <nav
        id="primary-navigation"
        :class="['navigation', { 'navigation--open': isMenuOpen }]"
        aria-label="주요 메뉴"
      >
        <RouterLink class="navigation__link" :to="{ name: 'home' }" @click="closeMenu">홈</RouterLink>
        <RouterLink class="navigation__link" :to="{ name: 'plan-search' }" @click="handlePlanSearchClick">
          일정 탐색
        </RouterLink>
        <RouterLink
          v-if="authStore.isAuthenticated"
          class="navigation__link"
          :to="{ name: 'my-plans' }"
          @click="closeMenu"
        >
          내 플랜
        </RouterLink>
        <RouterLink
          class="navigation__link"
          :to="{ name: 'notice-list' }"
          @click="closeMenu"
        >
          공지사항
        </RouterLink>

        <div class="navigation__account">
          <template v-if="!authStore.isAuthenticated">
            <RouterLink class="navigation__account-link" :to="{ name: 'login' }" @click="closeMenu">
              로그인
            </RouterLink>
            <RouterLink
              class="navigation__account-link navigation__account-link--primary"
              :to="{ name: 'join' }"
              @click="closeMenu"
            >
              회원가입
            </RouterLink>
          </template>
          <template v-else>
            <!-- <button type="button" class="alarm-button" aria-label="알림 목록 보기">
              <img :src="alarmImg" alt="알림" class="alarm-icon" />
            </button> -->
            <div class="alarm-wrapper">
              <!-- 버튼 클릭 시 toggleAlarm 실행 -->
              <button type="button" class="alarm-button" aria-label="알림 목록 보기" @click="toggleAlarm">
                <img :src="alarmImg" alt="알림" class="alarm-icon" />
              </button>
              
              <!-- 드롭다운 알림창 (애니메이션 적용) -->
              <transition name="dropdown">
                <div v-if="isAlarmOpen" class="alarm-dropdown">
                  <div class="alarm-header">
                    <h4>새로운 알림</h4>
                  </div>
                  
                  <ul v-if="Array.isArray(alarms) && alarms.length > 0" class="alarm-list">
                  <li 
                    v-for="(alarm, index) in alarms" 
                    :key="index" 
                    class="alarm-item"
                    @click="openAlarmDetail(alarm)"
                  >
                    <div class="unread-dot" v-if="!alarm.isRead"></div>
                    <div class="alarm-content">
                      <!-- 목록에서는 제목이나 요약된 메시지를 표시 -->
                      <p class="alarm-text">{{ alarm.title }}</p>
                    </div>
                  </li>
                </ul>
                  
                  <!-- 알림이 없을 때 -->
                  <div v-else class="alarm-empty">
                    <div class="empty-icon">🔕</div>
                    <p>{{ alarms === '알람이 없습니다.' ? alarms : '새로운 알림이 없습니다.' }}</p>
                  </div>
                </div>
              </transition>
            </div>
            <span class="member-name">{{ authStore.currentUser.displayName }}</span>
            <button type="button" :disabled="authStore.pending" @click="handleLogout">로그아웃</button>
          </template>
        </div>
      </nav>

      <div class="header__actions">
        <div v-if="!authStore.isAuthenticated" class="desktop-actions">
          <RouterLink id="loginBtn" class="text-button" :to="{ name: 'login' }">로그인</RouterLink>
          <RouterLink class="primary-button" :to="{ name: 'join' }">회원가입</RouterLink>
        </div>
        <div v-else class="desktop-actions authenticated-actions" >
          <!-- <button type="button" class="alarm-button" aria-label="알림 목록 보기">
            <img :src="alarmImg" alt="알림" class="alarm-icon" />
          </button> -->
          <div class="alarm-wrapper">
              <!-- 버튼 클릭 시 toggleAlarm 실행 -->
              <button type="button" class="alarm-button" aria-label="알림 목록 보기" @click="toggleAlarm">
                <img :src="alarmImg" alt="알림" class="alarm-icon" />
              </button>
              
              <!-- 드롭다운 알림창 (애니메이션 적용) -->
              <transition name="dropdown">
                <div v-if="isAlarmOpen" class="alarm-dropdown">
                  <div class="alarm-header">
                    <h4>새로운 알림</h4>
                  </div>
                  
                  <ul v-if="Array.isArray(alarms) && alarms.length > 0" class="alarm-list">
                  <li 
                    v-for="(alarm, index) in alarms" 
                    :key="index" 
                    class="alarm-item"
                    @click="openAlarmDetail(alarm)"
                  >
                    <div class="unread-dot" v-if="!alarm.isRead"></div>
                    <div class="alarm-content">
                      <!-- 목록에서는 제목이나 요약된 메시지를 표시 -->
                      <p class="alarm-text">{{ alarm.title }}</p>
                    </div>
                  </li>
                </ul>
                  
                  <!-- 알림이 없을 때 -->
                  <div v-else class="alarm-empty">
                    <div class="empty-icon">🔕</div>
                    <p>{{ alarms === '알람이 없습니다.' ? alarms : '새로운 알림이 없습니다.' }}</p>
                  </div>
                </div>
              </transition>
            </div>

            <!-- 알림 상세 보기 모달 -->
            <Teleport to="body">
              <div 
                v-if="selectedAlarm" 
                class="alarm-detail-backdrop" 
                @click.self="closeAlarmDetail"
              >
                <div class="alarm-detail-modal">
                  <!-- 헤더 (제목 영역 & 닫기 버튼) -->
                  <div class="detail-header">
                    <h3>알림 상세 보기</h3>
                    <button 
                      type="button" 
                      class="detail-close-btn" 
                      aria-label="닫기" 
                      @click="closeAlarmDetail"
                    >
                      &times;
                    </button>
                  </div>

                  <!-- 본문 (제목/내용 분할 영역) -->
                  <div class="detail-body">
                    <!-- 1. 알림 제목 -->
                    <div class="detail-section">
                      <span class="detail-label">제목</span>
                      <div class="detail-title">
                        {{ selectedAlarm.title || '알림' }}
                      </div>
                    </div>

                    <!-- 2. 알림 내용 -->
                    <div class="detail-section">
                      <span class="detail-label">내용</span>
                      <div class="detail-content">
                        {{ selectedAlarm.content || selectedAlarm.message || selectedAlarm }}
                      </div>
                    </div>

                    <!-- 3. 시간 (데이터가 존재할 경우 표시) -->
                    <div v-if="selectedAlarm.createdAt || selectedAlarm.date" class="detail-time">
                      {{ selectedAlarm.createdAt || selectedAlarm.date }}
                    </div>
                  </div>
                </div>
              </div>
            </Teleport>

            
          <RouterLink class="member-name text-button" :to="{name : 'myPage'}">{{ authStore.currentUser.displayName }}</RouterLink>
          <button
            class="text-button"
            type="button"
            :disabled="authStore.pending"
            @click="handleLogout"
          >
            로그아웃
          </button>
        </div>

        <button
          :class="['menu-button', { 'menu-button--open': isMenuOpen }]"
          type="button"
          aria-controls="primary-navigation"
          :aria-expanded="isMenuOpen"
          :aria-label="isMenuOpen ? '주요 메뉴 닫기' : '주요 메뉴 열기'"
          @click="toggleMenu"
        >
          <span aria-hidden="true" />
          <span aria-hidden="true" />
          <span aria-hidden="true" />
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 11;
  border-bottom: 1px solid rgb(226 232 240 / 90%);
  background: rgb(255 255 255 / 92%);
  backdrop-filter: blur(12px);
}

.header__inner {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  min-height: var(--layout-header-height);
}

.brand {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  width: fit-content;
  color: var(--color-brand);
  font-size: 21px;
  font-weight: 800;
}

.brand__mark {
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  object-fit: contain;
}

.navigation {
  display: flex;
  gap: 36px;
  font-size: 15px;
  font-weight: 650;
}

.navigation__link {
  position: relative;
  display: inline-flex;
  min-height: var(--layout-header-height);
  align-items: center;
  transition: color 150ms ease;
}

.navigation__link::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--color-brand-accent);
  content: '';
  opacity: 0;
  transform: scaleX(0.6);
  transition: opacity 150ms ease, transform 150ms ease;
}

.navigation__link:hover,
.navigation__link.router-link-active {
  color: var(--color-brand);
}

.navigation__link.router-link-active::after {
  opacity: 1;
  transform: scaleX(1);
}

.navigation__account {
  display: none;
}

.header__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.desktop-actions,
.authenticated-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.authenticated-actions {
  justify-content: flex-end;
}

.member-name {
  color: #374151;
  font-size: 14px;
  font-weight: 650;
}

.text-button,
.primary-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  border-radius: 10px;
  cursor: pointer;
}

.text-button {
  border: 0;
  background: transparent;
}

.primary-button {
  color: var(--color-brand-on);
  border: 1px solid var(--color-brand);
  background: var(--color-brand);
  transition: background-color 150ms ease, border-color 150ms ease;
}

.primary-button:hover {
  border-color: var(--color-brand-hover);
  background: var(--color-brand-hover);
}

.menu-button {
  display: none;
  width: 42px;
  height: 42px;
  padding: 9px;
  border: 1px solid var(--color-brand-border);
  border-radius: 10px;
  background: var(--color-surface);
  cursor: pointer;
}

.menu-button span {
  display: block;
  width: 100%;
  height: 2px;
  border-radius: 2px;
  background: var(--color-text);
  transition: transform 150ms ease, opacity 150ms ease;
}

.menu-button--open span:nth-child(1) {
  transform: translateY(6px) rotate(45deg);
}

.menu-button--open span:nth-child(2) {
  opacity: 0;
}

.menu-button--open span:nth-child(3) {
  transform: translateY(-6px) rotate(-45deg);
}

@media (max-width: 900px) {
  .navigation {
    gap: 24px;
  }
}

@media (max-width: 760px) {
  .header__inner {
    grid-template-columns: 1fr auto;
    min-height: 64px;
  }

  .navigation {
    position: absolute;
    top: 100%;
    right: 0;
    left: 0;
    flex-direction: column;
    gap: 4px;
    display: none;
    padding: 12px var(--layout-gutter) 18px;
    border: 1px solid var(--color-brand-border);
    border-top: 0;
    border-radius: 0 0 16px 16px;
    background: rgb(255 255 255 / 98%);
    box-shadow: 0 18px 40px rgb(15 23 42 / 12%);
    backdrop-filter: blur(12px);
  }

  .navigation--open {
    display: flex;
  }

  .navigation__link {
    min-height: 44px;
    justify-content: flex-start;
    padding: 0 12px;
    border-radius: 9px;
  }

  .navigation__link::after {
    display: none;
  }

  .navigation__link.router-link-active {
    background: var(--color-brand-soft);
  }

  .navigation__account {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    align-items: center;
    margin-top: 8px;
    padding-top: 14px;
    border-top: 1px solid #e2e8f0;
  }

  .navigation__account-link,
  .navigation__account button {
    display: inline-flex;
    min-height: 42px;
    align-items: center;
    justify-content: center;
    border: 1px solid var(--color-brand-border);
    border-radius: 10px;
    background: var(--color-surface);
    color: var(--color-brand);
    font-weight: 700;
  }

  .navigation__account-link--primary {
    border-color: var(--color-brand);
    background: var(--color-brand);
    color: var(--color-brand-on);
  }

  .navigation__account .member-name {
    padding-left: 12px;
  }

  .desktop-actions {
    display: none;
  }

  .menu-button {
    display: grid;
    align-content: center;
    gap: 4px;
  }
}

.alarm-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  transition: background-color 150ms ease, transform 150ms ease;
}

.alarm-button:hover {
  background-color: rgba(0, 0, 0, 0.06);
  transform: scale(1.05);
}

.alarm-button:active {
  transform: scale(0.95);
}

.alarm-icon {
  width: 22px;
  height: 22px;
  object-fit: contain;
}

.mobile-member-info {
  display: flex;
  align-items: center;
  gap: 4px;
}
/* ==================================================== */
.alarm-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
}

/* 2. 드롭다운 애니메이션 (Vue Transition) */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

/* 3. 드롭다운 컨테이너 디자인 */
.alarm-dropdown {
  position: absolute;
  top: 100%;
  right: -10px;
  margin-top: 14px;
  width: 340px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 16px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.12);
  z-index: 100;
  overflow: hidden;
  text-align: left;
}

/* 4. 드롭다운 헤더 */
.alarm-header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
}

.alarm-header h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #111827;
}

/* 5. 알림 리스트 (커스텀 스크롤바 적용) */
.alarm-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 380px;
  overflow-y: auto;
}

/* 스크롤바 디자인 */
.alarm-list::-webkit-scrollbar {
  width: 6px;
}
.alarm-list::-webkit-scrollbar-track {
  background: transparent;
}
.alarm-list::-webkit-scrollbar-thumb {
  background-color: #cbd5e1;
  border-radius: 10px;
}
.alarm-list::-webkit-scrollbar-thumb:hover {
  background-color: #94a3b8;
}

/* 6. 개별 알림 아이템 디자인 */
.alarm-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f8fafc;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.alarm-item:last-child {
  border-bottom: none;
}

.alarm-item:hover {
  background-color: #f8fafc;
}

/* 안 읽음 표시 (파란 점) */
.unread-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  background-color: var(--color-brand, #3b82f6);
  border-radius: 50%;
  margin-top: 6px; /* 텍스트 첫 줄과 위치 맞춤 */
}

/* 알림 텍스트 컨텐츠 */
.alarm-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alarm-text {
  margin: 0;
  font-size: 14px;
  color: #334155;
  line-height: 1.5;
  word-break: keep-all; /* 한글 줄바꿈 예쁘게 처리 */
}

/* 알림 발생 시간 (있을 경우) */
.alarm-time {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
}

/* 7. 알림이 없을 때 (Empty State) */
.alarm-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 48px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 32px;
  opacity: 0.6;
}

.alarm-empty p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

/* ==================================================== */
/* 알림 상세 보기 모달 스타일 */
/* ==================================================== */

/* 어두운 배경 레이어 */
.alarm-detail-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  backdrop-filter: blur(3px);
}

/* 모달 대화상자 */
.alarm-detail-modal {
  width: 90%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  animation: modalPop 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(8px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 모달 헤더 */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.detail-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

/* 오른쪽 위 닫기(✕) 버튼 */
.detail-close-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  font-size: 22px;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.detail-close-btn:hover {
  background-color: #e2e8f0;
  color: #0f172a;
}

/* 모달 본문 */
.detail-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 분할 섹션 (제목/내용) */
.detail-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

/* 제목 라벨 */
.detail-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.4;
  word-break: keep-all;
}

/* 내용 박스 */
.detail-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  background-color: #f8fafc;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
  white-space: pre-wrap; /* 줄바꿈(\n) 유지 */
  word-break: break-word;
}

/* 시간 표시 */
.detail-time {
  font-size: 12px;
  color: #94a3b8;
  text-align: right;
  margin-top: -6px;
}
</style>
