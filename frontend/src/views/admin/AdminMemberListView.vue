<script setup>
import { computed, ref } from 'vue'

const selectedTab = ref('all')
const keyword = ref('')
const joinPeriod = ref('all')
const memberStatus = ref('all')

const tabs = [
  { label: '전체 회원', value: 'all' },
  { label: '정상', value: 'active' },
  { label: '정지', value: 'suspended' },
  { label: '탈퇴', value: 'withdrawn' },
]

const members = ref([
  {
    memberNumber: 'M-111111',
    name: '김민수',
    email: 'TEST@TEST.com',
    joinDate: '2026.07.07',
    trips: 10,
    reports: 3,
    status: 'suspended',
  },
  {
    memberNumber: 'M-123456',
    name: '이서연',
    email: 'seo123@TEST.com',
    joinDate: '2026.07.11',
    trips: 12,
    reports: 2,
    status: 'withdrawn',
  },
  {
    memberNumber: 'M-717171',
    name: '최지호',
    email: 'choi111@TEST.com',
    joinDate: '2026.07.10',
    trips: 13,
    reports: 0,
    status: 'active',
  },
])

const filteredMembers = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()

  return members.value.filter((member) => {
    const matchesTab =
      selectedTab.value === 'all' || member.status === selectedTab.value

    const matchesStatus =
      memberStatus.value === 'all' ||
      member.status === memberStatus.value

    const matchesKeyword =
      !normalizedKeyword ||
      member.memberNumber.toLowerCase().includes(normalizedKeyword) ||
      member.name.toLowerCase().includes(normalizedKeyword) ||
      member.email.toLowerCase().includes(normalizedKeyword)

    return matchesTab && matchesStatus && matchesKeyword
  })
})

const statusText = (status) => {
  const statusMap = {
    active: '정상',
    suspended: '정지',
    withdrawn: '탈퇴',
  }

  return statusMap[status] || status
}

const suspendMember = (member) => {
  member.status = 'suspended'
}

const activateMember = (member) => {
  member.status = 'active'
}
</script>

<template>
  <section class="member-page">
    <header class="page-header">
      <h1>회원 관리</h1>
      <p>회원 상태와 서비스 활동 내역을 조회하고 관리합니다.</p>
    </header>

    <section class="member-panel">
      <div class="status-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="[
            'status-tab',
            { 'status-tab--active': selectedTab === tab.value },
          ]"
          @click="selectedTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="filter-area">
        <div class="search-field">
          <span class="search-icon" aria-hidden="true">⌕</span>

          <input
            v-model="keyword"
            type="search"
            placeholder="이름, 아이디, 이메일 검색"
          />
        </div>

        <select v-model="joinPeriod" aria-label="가입일 선택">
          <option value="all">가입일 전체</option>
          <option value="today">오늘</option>
          <option value="week">최근 7일</option>
          <option value="month">최근 30일</option>
        </select>

        <select v-model="memberStatus" aria-label="회원 상태 선택">
          <option value="all">상태 전체</option>
          <option value="active">정상</option>
          <option value="suspended">정지</option>
          <option value="withdrawn">탈퇴</option>
        </select>

        <button class="search-button" type="button">
          검색
        </button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>회원 번호</th>
              <th>회원 정보</th>
              <th>가입일</th>
              <th>여행 플랜</th>
              <th>신고 누적</th>
              <th>상태</th>
              <th>관리</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="member in filteredMembers"
              :key="member.memberNumber"
            >
              <td>{{ member.memberNumber }}</td>

              <td>
                <div class="member-information">
                  <strong>{{ member.name }}</strong>
                  <span>{{ member.email }}</span>
                </div>
              </td>

              <td>{{ member.joinDate }}</td>
              <td>{{ member.trips }}건</td>
              <td>{{ member.reports }}회</td>

              <td>
                <span
                  :class="[
                    'status-badge',
                    `status-badge--${member.status}`,
                  ]"
                >
                  {{ statusText(member.status) }}
                </span>
              </td>

              <td>
                <div class="management-buttons">
                  <button
                    class="detail-button"
                    type="button"
                    @click="
                      $router.push({
                        name: 'admin-member-detail',
                        params: { memberId: member.memberNumber },
                      })
                    "
                  >
                    상세
                  </button>

                  <button
                    v-if="member.status === 'active'"
                    class="suspend-button"
                    type="button"
                    @click="suspendMember(member)"
                  >
                    정지
                  </button>

                  <button
                    v-if="member.status === 'suspended'"
                    class="activate-button"
                    type="button"
                    @click="activateMember(member)"
                  >
                    해제
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="filteredMembers.length === 0">
              <td class="empty-message" colspan="7">
                조회된 회원이 없습니다.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.member-page {
  min-height: 100%;
  color: #272b31;
}

.page-header {
  margin-bottom: 26px;
}

.page-header h1 {
  margin: 0;
  font-size: 34px;
  letter-spacing: -1.2px;
}

.page-header p {
  margin: 9px 0 0;
  color: #8c929c;
  font-size: 14px;
}

.member-panel {
  padding: 22px 24px 30px;
  border: 1px solid #dfe3e8;
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 3px 12px rgb(31 41 55 / 4%);
}

.status-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.status-tab {
  min-width: 72px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #ff9a76;
  border-radius: 5px;
  background: #ffffff;
  color: #f2764d;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.status-tab:hover,
.status-tab--active {
  background: #fff2ec;
  border-color: #ff7a4b;
  color: #f05f2d;
}

.filter-area {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 160px 160px 100px;
  gap: 18px;
  margin-bottom: 28px;
}

.search-field {
  position: relative;
}

.search-icon {
  position: absolute;
  top: 50%;
  left: 13px;
  color: #a1a6ae;
  transform: translateY(-50%);
}

.search-field input,
.filter-area select {
  width: 100%;
  height: 40px;
  border: 1px solid #cfd4da;
  border-radius: 5px;
  outline: none;
  background: #ffffff;
  color: #464b53;
  font-size: 13px;
}

.search-field input {
  padding: 0 14px 0 38px;
}

.filter-area select {
  padding: 0 12px;
}

.search-field input:focus,
.filter-area select:focus {
  border-color: #f18460;
  box-shadow: 0 0 0 3px rgb(241 132 96 / 12%);
}

.search-button {
  height: 40px;
  border: 0;
  border-radius: 5px;
  background: #ed8c68;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.search-button:hover {
  background: #e87a52;
}

.table-wrapper {
  min-height: 340px;
  overflow-x: auto;
  border: 1px solid #d8dce2;
  border-radius: 4px;
}

table {
  width: 100%;
  min-width: 820px;
  border-collapse: collapse;
  table-layout: fixed;
}

thead {
  background: #e2e5e9;
}

th {
  height: 48px;
  color: #545a63;
  font-size: 13px;
  font-weight: 800;
}

td {
  height: 52px;
  padding: 8px 14px;
  border-bottom: 1px solid #d8dce2;
  color: #464b52;
  font-size: 13px;
  text-align: center;
}

th:nth-child(1) {
  width: 14%;
}

th:nth-child(2) {
  width: 24%;
}

th:nth-child(3) {
  width: 14%;
}

th:nth-child(4),
th:nth-child(5) {
  width: 11%;
}

th:nth-child(6) {
  width: 10%;
}

th:nth-child(7) {
  width: 16%;
}

.member-information {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 3px;
  text-align: left;
}

.member-information strong {
  color: #30343a;
  font-size: 13px;
}

.member-information span {
  color: #989ea7;
  font-size: 11px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  height: 24px;
  padding: 0 9px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 800;
}

.status-badge--active {
  background: #dff3ff;
  color: #438dca;
}

.status-badge--suspended {
  background: #ffe5e4;
  color: #f06b65;
}

.status-badge--withdrawn {
  background: #f0f1f3;
  color: #858b93;
}

.management-buttons {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.management-buttons button {
  min-width: 42px;
  height: 25px;
  padding: 0 8px;
  border-radius: 5px;
  background: #ffffff;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.detail-button,
.activate-button {
  border: 1px solid #aeb5be;
  color: #6c737c;
}

.suspend-button {
  border: 1px solid #ff8a80;
  color: #f06860;
}

.empty-message {
  height: 180px;
  color: #949aa3;
}

@media (max-width: 1050px) {
  .filter-area {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 650px) {
  .filter-area {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .status-tabs {
    overflow-x: auto;
  }

  .member-panel {
    padding: 18px;
  }
}
</style>
