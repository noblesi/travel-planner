<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AdminConfirmModal from '@/components/admin/AdminConfirmModal.vue'
import { useToastStore } from '@/stores/toast'

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

// /admin/members/:memberId의 동적 URL 값을 가져옵니다.
const memberId = computed(() => route.params.memberId)
const isEditingMemo = ref(false)
const adminMemo = ref('특이 사항 없음')
const showStatusConfirm = ref(false)

// memberId를 전달한 회원 상세 API 응답으로 교체합니다.
const member = reactive({
  name: '김민수',
  email: 'minsul2@test.com',
  status: 'active',
  role: 'member',
  joinedAt: '2026-07-07 14:21',
  lastLoginAt: '2026-07-19 13:20',
  phone: '010-1111-1111',
  birthDate: '2001-01-01',
})

const activities = [
  { label: '여행 플랜', value: 12 },
  { label: '신고받은 플랜 수', value: 3 },
  { label: '신고 누적', value: 0 },
]

// 상태 코드가 바뀌면 화면의 상태 문구도 자동으로 다시 계산됩니다.
const statusText = computed(() => {
  const statusMap = {
    active: '정상 회원',
    suspended: '정지 회원',
    withdrawn: '탈퇴 회원',
  }

  return statusMap[member.status] || member.status
})

const toggleMemberStatus = () => {
  // 회원 정지/해제 API 성공 결과에 따라 상태를 변경합니다.
  member.status = member.status === 'suspended' ? 'active' : 'suspended'
  toast.success(`회원 상태가 ${member.status === 'suspended' ? '정지' : '정상'}으로 변경되었습니다.`)
  showStatusConfirm.value = false
}

const saveMemo = () => {
  // 관리자 메모 저장 API 호출 후 편집 상태를 종료합니다.
  isEditingMemo.value = false
  toast.success('관리자 메모가 저장되었습니다.')
}
</script>

<template>
  <section class="member-detail-page">
    <header class="page-header">
      <div>
        <h1>회원 상세</h1>
        <p>회원 기본 정보, 활동 내역과 계정 상태를 확인합니다.</p>
      </div>

      <div class="header-actions">
        <button class="button button--dark" type="button" @click="router.push({ name: 'admin-members' })">
          목록으로
        </button>
        <button
          :class="['button', member.status === 'suspended' ? 'button--activate' : 'button--danger']"
          type="button"
          @click="showStatusConfirm = true"
        >
          {{ member.status === 'suspended' ? '정지 해제' : '회원 정지' }}
        </button>
      </div>
    </header>

    <article class="member-card">
      <div class="member-heading">
        <div>
          <div class="name-row">
            <h2>{{ member.name }}</h2>
            <span :class="['status-badge', `status-badge--${member.status}`]">
              {{ statusText }}
            </span>
          </div>
          <p>{{ member.email }}</p>
        </div>
        <span class="member-number">{{ memberId }}</span>
      </div>

      <dl class="information-grid">
        <div><dt>가입일</dt><dd>{{ member.joinedAt }}</dd></div>
        <div><dt>최근 로그인</dt><dd>{{ member.lastLoginAt }}</dd></div>
        <div><dt>휴대전화</dt><dd>{{ member.phone }}</dd></div>
        <div><dt>생년월일</dt><dd>{{ member.birthDate }}</dd></div>
      </dl>
    </article>

    <div class="detail-grid">
      <article class="management-card">
        <section class="account-section">
          <h2>계정 상태</h2>
          <div class="account-information">
            <div>
              <span>현재 상태</span>
              <strong :class="`account-status--${member.status}`">{{ statusText }}</strong>
            </div>
            <div><span>정지 이력</span><strong>없음</strong></div>
            <label class="role-field">
              <span>회원 권한</span>
              <select v-model="member.role">
                <option value="member">일반 회원</option>
                <option value="power-planner">파워 플래너</option>
              </select>
            </label>
          </div>
        </section>

        <section class="memo-section">
          <div class="section-heading">
            <h2>관리자 메모</h2>
            <button v-if="!isEditingMemo" type="button" @click="isEditingMemo = true">메모 수정</button>
            <button v-else class="memo-save" type="button" @click="saveMemo">저장</button>
          </div>
          <textarea v-model="adminMemo" :readonly="!isEditingMemo" />
        </section>
      </article>

      <aside class="activity-card">
        <h2>활동 요약</h2>
        <div class="activity-list">
          <div v-for="activity in activities" :key="activity.label" class="activity-item">
            <span>{{ activity.label }}</span>
            <strong>{{ activity.value }}</strong>
          </div>
        </div>
      </aside>
    </div>
    <AdminConfirmModal
      v-if="showStatusConfirm"
      :title="member.status === 'suspended' ? '회원 정지를 해제할까요?' : '회원을 정지할까요?'"
      :message="`${member.name} 회원의 계정 상태를 변경합니다.`"
      :confirm-label="member.status === 'suspended' ? '정지 해제' : '회원 정지'"
      :danger="member.status !== 'suspended'"
      @cancel="showStatusConfirm = false"
      @confirm="toggleMemberStatus"
    />
  </section>
</template>

<style scoped>
.member-detail-page { min-height: 100%; color: #272b31; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 26px; }
.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }
.page-header p { margin: 9px 0 0; color: #8c929c; font-size: 14px; }
.header-actions { display: flex; gap: 10px; }
.button { height: 38px; padding: 0 15px; border: 1px solid #ef9876; border-radius: 5px; background: #fff; color: #e8754d; font-size: 13px; font-weight: 700; cursor: pointer; }
.button:hover { background: #fff4ef; }
.button--dark { border-color: #9198a1; color: #565c65; }
.button--danger { border-color: #ff8a80; color: #ef625b; }
.button--activate { border-color: #58a8dd; color: #398bc4; }
.member-card, .management-card, .activity-card { border: 1px solid #dfe3e8; border-radius: 6px; background: #fff; box-shadow: 0 3px 12px rgb(31 41 55 / 4%); }
.member-card { min-height: 245px; padding: 26px 28px; }
.member-heading { display: flex; align-items: flex-start; justify-content: space-between; }
.name-row { display: flex; align-items: center; gap: 10px; }
.name-row h2 { margin: 0; font-size: 21px; }
.member-heading p { margin: 7px 0 0; color: #9298a1; font-size: 12px; }
.member-number { color: #8e949d; font-size: 12px; }
.status-badge { display: inline-flex; align-items: center; min-height: 24px; padding: 0 9px; border-radius: 20px; font-size: 11px; font-weight: 800; }
.status-badge--active { background: #ddf7e8; color: #2d9b64; }
.status-badge--suspended { background: #ffe5e4; color: #ee6962; }
.status-badge--withdrawn { background: #eff0f2; color: #777d86; }
.information-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); width: min(760px, 100%); margin: 28px 0 0; }
.information-grid > div { min-height: 66px; padding: 10px 0; border-bottom: 1px solid #ccd1d8; }
.information-grid > div:nth-child(odd) { padding-right: 26px; }
.information-grid > div:nth-child(even) { padding-left: 26px; }
.information-grid dt { margin-bottom: 7px; color: #989ea7; font-size: 12px; }
.information-grid dd { margin: 0; color: #363b42; font-size: 14px; }
.detail-grid { display: grid; grid-template-columns: minmax(0, 2fr) minmax(260px, .75fr); gap: 28px; margin-top: 28px; }
.management-card, .activity-card { min-height: 285px; padding: 24px 28px; }
.management-card h2, .activity-card h2 { margin: 0; font-size: 17px; }
.account-section { padding-bottom: 20px; border-bottom: 1px solid #d4d8de; }
.account-information { display: grid; grid-template-columns: repeat(3, minmax(130px, 1fr)); gap: 36px; margin-top: 18px; }
.account-information > div { display: grid; gap: 7px; }
.account-information span { color: #9399a2; font-size: 12px; }
.account-information strong { font-size: 13px; }
.role-field { display: grid; gap: 7px; }
.role-field select { width: 100%; max-width: 180px; height: 36px; padding: 0 10px; border: 1px solid #cfd4da; border-radius: 5px; outline: none; background: #fff; color: #3e444c; font-size: 13px; cursor: pointer; }
.role-field select:focus { border-color: #ef9472; box-shadow: 0 0 0 3px rgb(239 148 114 / 12%); }
.account-status--active { color: #3d9e6c; }
.account-status--suspended { color: #ee6962; }
.account-status--withdrawn { color: #777d86; }
.memo-section { margin-top: 20px; }
.section-heading { display: flex; align-items: center; justify-content: space-between; }
.section-heading button { height: 32px; padding: 0 13px; border: 1px solid #ef9472; border-radius: 4px; background: #fff; color: #e8734c; font-size: 12px; font-weight: 700; cursor: pointer; }
.section-heading .memo-save { background: #ed8c68; color: #fff; }
.memo-section textarea { width: 100%; min-height: 92px; margin-top: 13px; padding: 13px; resize: vertical; border: 1px solid #cfd4da; border-radius: 5px; outline: none; color: #444951; font-size: 13px; }
.memo-section textarea:read-only { background: #fafafa; color: #777d85; }
.activity-list { display: grid; gap: 12px; margin-top: 18px; }
.activity-item { display: flex; min-height: 72px; padding: 13px 16px; border: 1px solid #f19a78; border-radius: 9px; flex-direction: column; justify-content: center; background: #fff1eb; }
.activity-item span { color: #8d929a; font-size: 12px; }
.activity-item strong { margin-top: 5px; color: #2c3035; font-size: 21px; }
@media (max-width: 1000px) { .page-header { align-items: flex-start; flex-direction: column; } .detail-grid { grid-template-columns: 1fr; } .activity-list { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 650px) { .header-actions { width: 100%; flex-wrap: wrap; } .information-grid { grid-template-columns: 1fr; } .information-grid > div:nth-child(n) { padding-right: 0; padding-left: 0; } .account-information { grid-template-columns: 1fr; } .activity-list { grid-template-columns: 1fr; } }
</style>
