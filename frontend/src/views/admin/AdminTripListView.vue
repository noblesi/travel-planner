<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

// 여행 플랜 목록의 탭, 검색어, 지역, 처리 상태 필터입니다.
const router = useRouter()
const selectedTab = ref('all')
const keyword = ref('')
const selectedRegion = ref('all')
const selectedStatus = ref('all')
const isRecommendationModalOpen = ref(false)

const recommendationWeights = reactive({
  likes: 40,
  views: 20,
  saves: 30,
  freshness: 10,
})

const resetRecommendationWeights = () => {
  recommendationWeights.likes = 40
  recommendationWeights.views = 20
  recommendationWeights.saves = 30
  recommendationWeights.freshness = 10
}

const saveRecommendationWeights = () => {
  isRecommendationModalOpen.value = false
}

const tabs = [
  { label: '전체 공개 플랜', value: 'all' },
  { label: '비공개 플랜', value: 'private' },
  { label: '신고 접수', value: 'reported' },
]

// 여행 플랜 목록 API 응답으로 교체할 화면 확인용 데이터입니다.
const trips = ref([
  { id: 'P-5412', title: '서울 궁궐 여행', region: '서울', author: '김민수', duration: '1박 2일', likes: 142, views: 233, status: 'public' },
  { id: 'P-1122', title: '제주 카페 투어', region: '제주', author: '이서연', duration: '2박 3일', likes: 52, views: 73, status: 'public' },
  { id: 'P-7898', title: '부산 서핑 드라이브', region: '부산', author: '최지호', duration: '당일 치기', likes: 0, views: 0, status: 'private' },
  { id: 'P-9041', reportId: 'R-221133', title: '제주 숨은 명소 완전 정복', region: '제주', author: '박여행', duration: '3박 4일', likes: 31, views: 128, status: 'review-pending' },
  { id: 'P-9052', reportId: 'R-221144', title: '서울 야경 명소 모음', region: '서울', author: '정하늘', duration: '1박 2일', likes: 18, views: 96, status: 'review-completed' },
])

// 신고 검토 행은 신고 상세로, 일반 플랜 행은 여행 플랜 상세로 이동합니다.
const openDetail = (trip) => {
  if (trip.reportId) {
    router.push({
      name: 'admin-report-detail',
      params: { reportId: trip.reportId },
    })
    return
  }

  router.push({
    name: 'admin-trip-detail',
    params: { tripId: trip.id },
  })
}

// 신고 접수 탭은 검토 대기와 검토 완료 상태를 모두 포함합니다.
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
  // 백엔드 상태 코드를 사용자에게 보여줄 한글 문구로 변환합니다.
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
      <div class="header-actions">
        <span class="result-count">검색 결과 <strong>{{ filteredTrips.length }}</strong>건</span>
        <button
          class="rule-button"
          type="button"
          @click="isRecommendationModalOpen = true"
        >
          추천 점수 규칙
        </button>
      </div>
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
                    @click="openDetail(trip)"
                  >상세</button><button type="button">수정</button><button class="danger" type="button">숨김</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredTrips.length === 0"><td class="empty" colspan="7">조회된 여행 플랜이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <Teleport to="body">
      <div
        v-if="isRecommendationModalOpen"
        class="modal-overlay"
        role="presentation"
        @click.self="isRecommendationModalOpen = false"
      >
        <section
          class="recommendation-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="recommendation-modal-title"
        >
          <header class="modal-header">
            <div>
              <h2 id="recommendation-modal-title">추천 점수 규칙 설정</h2>
              <p>메인 화면의 추천 콘텐츠 노출 순위를 계산하는 기준입니다.</p>
            </div>
            <button
              class="modal-close"
              type="button"
              aria-label="추천 점수 규칙 닫기"
              @click="isRecommendationModalOpen = false"
            >
              ×
            </button>
          </header>

          <div class="rule-guide">
            항목별 비중을 조정해 여행 플랜 추천 기준을 설정합니다.
          </div>

          <form class="weight-form" @submit.prevent="saveRecommendationWeights">
            <label>
              <span>좋아요 수</span>
              <span class="weight-input"><input v-model.number="recommendationWeights.likes" type="number" /><small>%</small></span>
            </label>
            <label>
              <span>조회 수</span>
              <span class="weight-input"><input v-model.number="recommendationWeights.views" type="number" /><small>%</small></span>
            </label>
            <label>
              <span>일정 저장 수</span>
              <span class="weight-input"><input v-model.number="recommendationWeights.saves" type="number" /><small>%</small></span>
            </label>
            <label>
              <span>최신성 점수</span>
              <span class="weight-input"><input v-model.number="recommendationWeights.freshness" type="number" /><small>%</small></span>
            </label>

            <div class="modal-actions">
              <button class="modal-reset" type="button" @click="resetRecommendationWeights">초기화</button>
              <button class="modal-save" type="submit">규칙 저장</button>
            </div>
          </form>
        </section>
      </div>
    </Teleport>
  </section>
</template>

<style scoped>
.trip-page { min-height: 100%; color: #272b31; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 26px; }
.page-header h1 { margin: 0; font-size: 34px; letter-spacing: -1.2px; }
.page-header p { margin: 9px 0 0; color: #8c929c; font-size: 14px; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.result-count { padding: 9px 14px; border: 1px solid #cfd4da; border-radius: 5px; background: #fff; color: #737a84; font-size: 12px; }
.result-count strong { color: #f06d3f; }
.rule-button { height: 38px; padding: 0 16px; border: 1px solid var(--admin-orange); border-radius: 5px; background: var(--admin-orange); color: #fff; font-size: 13px; font-weight: 800; cursor: pointer; }
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

<style>
.modal-overlay { position: fixed; z-index: 1000; inset: 0; display: grid; padding: 24px; place-items: center; background: rgb(37 42 49 / 45%); }
.recommendation-modal { width: min(500px, 100%); padding: 26px; border: 1px solid #eee3db; border-radius: 16px; background: #fff; color: #252a31; box-shadow: 0 24px 70px rgb(37 42 49 / 18%); }
.modal-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.modal-header h2 { margin: 0; font-size: 22px; }
.modal-header p { margin: 8px 0 0; color: #858c96; font-size: 13px; }
.modal-close { width: 34px; height: 34px; border: 0; border-radius: 50%; background: #fff0e5; color: #f3883b; font-size: 22px; cursor: pointer; }
.rule-guide { margin: 22px 0; padding: 14px 16px; border-radius: 8px; background: #fff0e5; color: #765f51; font-size: 13px; }
.weight-form { display: grid; gap: 16px; }
.weight-form label { display: flex; align-items: center; justify-content: space-between; font-size: 14px; font-weight: 700; }
.weight-input { display: flex; align-items: center; gap: 8px; }
.weight-input input { width: 76px; height: 38px; padding: 0 10px; border: 1px solid #ddd3cc; border-radius: 6px; outline: none; }
.weight-input input:focus { border-color: #f3883b; box-shadow: 0 0 0 3px rgb(243 136 59 / 14%); }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 14px; }
.modal-actions button { height: 40px; padding: 0 18px; border-radius: 6px; font-weight: 800; cursor: pointer; }
.modal-reset { border: 1px solid #f3883b; background: #fff; color: #f3883b; }
.modal-save { border: 1px solid #f3883b; background: #f3883b; color: #fff; }
</style>
