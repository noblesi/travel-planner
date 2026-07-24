<script setup>
import { computed, ref } from 'vue'

const selectedTab = ref('all')
const keyword = ref('')
const selectedRegion = ref('all')
const selectedStatus = ref('all')

const tabs = [
  { label: '전체 공개 플랜', value: 'all' },
  { label: '비공개 플랜', value: 'private' },
  { label: '신고 접수', value: 'reported' },
]

const trips = ref([
  { id: 'P-5412', title: '서울 궁궐 여행', region: '서울', author: '김민수', duration: '1박 2일', likes: 142, views: 233, status: 'public' },
  { id: 'P-1122', title: '제주 카페 투어', region: '제주', author: '이서연', duration: '2박 3일', likes: 52, views: 73, status: 'public' },
  { id: 'P-7898', title: '부산 서핑 드라이브', region: '부산', author: '최지호', duration: '당일 치기', likes: 0, views: 0, status: 'private' },
  { id: 'P-9041', title: '제주 숨은 명소 완전 정복', region: '제주', author: '박여행', duration: '3박 4일', likes: 31, views: 128, status: 'review-pending' },
  { id: 'P-9052', title: '서울 야경 명소 모음', region: '서울', author: '정하늘', duration: '1박 2일', likes: 18, views: 96, status: 'review-completed' },
])

const filteredTrips = computed(() => {
  const query = keyword.value.trim().toLowerCase()

  return trips.value.filter((trip) => {
    const matchesTab =
      selectedTab.value === 'all' ||
      (selectedTab.value === 'private' && trip.status === 'private') ||
      (selectedTab.value === 'reported' && ['review-pending', 'review-completed'].includes(trip.status))
    const matchesRegion = selectedRegion.value === 'all' || trip.region === selectedRegion.value
    const matchesStatus = selectedStatus.value === 'all' || trip.status === selectedStatus.value
    const matchesKeyword = !query || trip.title.toLowerCase().includes(query) || trip.author.toLowerCase().includes(query)
    return matchesTab && matchesRegion && matchesStatus && matchesKeyword
  })
})

const statusText = (status) => ({
  public: '공개',
  private: '비공개',
  'review-pending': '검토 대기',
  'review-completed': '검토 완료',
})[status]
</script>

<template>
  <section class="trip-page">
    <header class="page-header">
      <div>
        <h1>여행 플랜 관리</h1>
        <p>공개 여행 플랜의 노출 상태와 신고 콘텐츠를 관리합니다.</p>
      </div>
      <span class="result-count">검색 결과 <strong>{{ filteredTrips.length }}</strong>건</span>
    </header>

    <section class="trip-panel">
      <div class="status-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="['status-tab', { 'status-tab--active': selectedTab === tab.value }]"
          type="button"
          @click="selectedTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <div class="filters">
        <input v-model="keyword" type="search" placeholder="플랜 제목 또는 작성자 검색" />
        <select v-model="selectedRegion">
          <option value="all">지역 전체</option>
          <option value="서울">서울</option>
          <option value="부산">부산</option>
          <option value="제주">제주</option>
        </select>
        <select v-model="selectedStatus">
          <option value="all">상태 전체</option>
          <option value="public">공개</option>
          <option value="private">비공개</option>
          <option value="review-pending">검토 대기</option>
          <option value="review-completed">검토 완료</option>
        </select>
        <button class="search-button" type="button">검색</button>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>플랜 번호</th><th>여행 플랜</th><th>작성자</th><th>기간</th>
              <th>좋아요/조회</th><th>노출 상태</th><th>관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="trip in filteredTrips" :key="trip.id">
              <td>{{ trip.id }}</td>
              <td class="trip-title"><strong>{{ trip.title }}</strong><span>{{ trip.region }}</span></td>
              <td>{{ trip.author }}</td><td>{{ trip.duration }}</td><td>{{ trip.likes }}/{{ trip.views }}</td>
              <td><span :class="['status-badge', `status-badge--${trip.status}`]">{{ statusText(trip.status) }}</span></td>
              <td>
                <div class="actions">
                  <button
                    type="button"
                    @click="$router.push({ name: 'admin-trip-detail', params: { tripId: trip.id } })"
                  >상세</button><button type="button">수정</button><button class="danger" type="button">숨김</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredTrips.length === 0"><td class="empty" colspan="7">조회된 여행 플랜이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.trip-page { min-height: 100%; color: #272b31; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }
.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }
.page-header p { margin: 9px 0 0; color: #8c929c; font-size: 14px; }
.result-count { padding: 9px 14px; border: 1px solid #cfd4da; border-radius: 5px; background: #fff; color: #737a84; font-size: 12px; }
.result-count strong { color: #f06d3f; }
.trip-panel { min-height: 470px; padding: 22px 24px 30px; border: 1px solid #dfe3e8; border-radius: 6px; background: #fff; box-shadow: 0 3px 12px rgb(31 41 55 / 4%); }
.status-tabs { display: flex; gap: 10px; margin-bottom: 18px; }
.status-tab { height: 36px; padding: 0 16px; border: 1px solid #ff9a76; border-radius: 5px; background: #fff; color: #f2764d; font-size: 13px; font-weight: 700; cursor: pointer; }
.status-tab--active, .status-tab:hover { background: #fff2ec; border-color: #ff7a4b; }
.filters { display: grid; grid-template-columns: minmax(280px, 1fr) 170px 170px 100px; gap: 18px; margin-bottom: 28px; }
.filters input, .filters select { width: 100%; height: 40px; padding: 0 13px; border: 1px solid #cfd4da; border-radius: 5px; outline: none; background: #fff; color: #464b53; font-size: 13px; }
.filters input:focus, .filters select:focus { border-color: #f18460; box-shadow: 0 0 0 3px rgb(241 132 96 / 12%); }
.search-button { border: 0; border-radius: 5px; background: #ed8c68; color: #fff; font-size: 13px; font-weight: 800; cursor: pointer; }
.table-wrapper { min-height: 310px; overflow-x: auto; border: 1px solid #d8dce2; border-radius: 4px; }
table { width: 100%; min-width: 900px; border-collapse: collapse; table-layout: fixed; }
thead { background: #e2e5e9; }
th { height: 48px; color: #545a63; font-size: 13px; font-weight: 800; }
td { height: 55px; padding: 8px 12px; border-bottom: 1px solid #d8dce2; color: #464b52; font-size: 13px; text-align: center; }
th:nth-child(1) { width: 12%; } th:nth-child(2) { width: 24%; } th:nth-child(3) { width: 12%; } th:nth-child(4) { width: 12%; } th:nth-child(5) { width: 13%; } th:nth-child(6) { width: 12%; } th:nth-child(7) { width: 15%; }
.trip-title { text-align: left; }
.trip-title strong, .trip-title span { display: block; }
.trip-title span { margin-top: 4px; color: #9aa0a8; font-size: 11px; }
.status-badge { display: inline-flex; align-items: center; min-height: 24px; padding: 0 9px; border-radius: 20px; font-size: 11px; font-weight: 800; }
.status-badge--public { background: #dff3ff; color: #438dca; } .status-badge--private { background: #f0f1f3; color: #858b93; } .status-badge--review-pending { background: #fff0e7; color: #ed7449; } .status-badge--review-completed { background: #e5f6ec; color: #389765; }
.actions { display: flex; justify-content: center; gap: 5px; }
.actions button { height: 26px; padding: 0 8px; border: 1px solid #aeb5be; border-radius: 5px; background: #fff; color: #6c737c; font-size: 11px; cursor: pointer; }
.actions .danger { border-color: #ff8a80; color: #f06860; }
.empty { height: 180px; color: #949aa3; }
@media (max-width: 1050px) { .filters { grid-template-columns: 1fr 1fr; } }
@media (max-width: 650px) { .page-header { align-items: flex-start; flex-direction: column; } .filters { grid-template-columns: 1fr; gap: 10px; } .search-button { height: 40px; } .status-tabs { overflow-x: auto; } }
</style>
