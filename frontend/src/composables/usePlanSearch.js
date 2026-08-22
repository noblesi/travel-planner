import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getPlanList } from '@/api/planSearch'
import { usePlanSearchStore } from '@/stores/planSearch'
import {
  createPlanRestoreRequests,
  mapPublicPlan,
  parsePlanSearchRoute,
  PLAN_SEARCH_PAGE_SIZE,
} from '@/utils/planSearch'

const INITIAL_LOAD_ERROR = '공개 일정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'
const LOAD_MORE_ERROR = '다음 일정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'

export function usePlanSearch() {
  const router = useRouter()
  const route = useRoute()
  const planSearchStore = usePlanSearchStore()
  const initialRoute = parsePlanSearchRoute(route.query)
  const cachedSearch = planSearchStore.restoreSearch({
    searchedKeyword: initialRoute.keyword,
    currentPage: initialRoute.page,
  })

  const plans = ref(cachedSearch?.plans ?? [])
  const loading = ref(false)
  const loadingMore = ref(false)
  const errorMessage = ref('')
  const totalCount = ref(cachedSearch?.totalCount ?? 0)
  const hasNextPage = ref(cachedSearch?.hasNextPage ?? false)
  const keyword = ref(cachedSearch?.keyword ?? initialRoute.keyword)
  const searchedKeyword = ref(cachedSearch?.searchedKeyword ?? initialRoute.keyword)
  const hasSearched = ref(cachedSearch?.hasSearched ?? Boolean(initialRoute.keyword))
  const currentPage = ref(cachedSearch?.currentPage ?? initialRoute.page)
  const hasMore = computed(() => hasNextPage.value)
  let requestSequence = 0

  function cacheCurrentSearch() {
    planSearchStore.cacheSearch({
      keyword: keyword.value,
      searchedKeyword: searchedKeyword.value,
      hasSearched: hasSearched.value,
      currentPage: currentPage.value,
      plans: plans.value,
      totalCount: totalCount.value,
      hasNextPage: hasNextPage.value,
    })
  }

  function syncUrl() {
    const query = {}
    if (searchedKeyword.value) query.keyword = searchedKeyword.value
    if (currentPage.value > 1) query.page = currentPage.value
    router.replace({ query })
  }

  function openPlanDetail(planId) {
    cacheCurrentSearch()
    return router.push({ name: 'plan-detail', params: { id: planId } })
  }

  async function loadPlans(searchKeyword = '', targetPage = 1) {
    const sequence = ++requestSequence
    loading.value = true
    loadingMore.value = false
    errorMessage.value = ''
    plans.value = []
    totalCount.value = 0
    hasNextPage.value = false

    try {
      const requests = createPlanRestoreRequests(searchKeyword, targetPage)
      const results = await Promise.all(requests.map((request) => getPlanList(request)))
      if (sequence !== requestSequence) return

      const requestedRecordCount = requests.reduce((total, request) => total + request.size, 0)
      const restoredPlans = results
        .flatMap((result) => result.plans)
        .slice(0, requestedRecordCount)
        .map(mapPublicPlan)
      const resultTotalCount = results[0]?.totalCount ?? 0

      plans.value = restoredPlans
      currentPage.value = Math.max(Math.ceil(restoredPlans.length / PLAN_SEARCH_PAGE_SIZE), 1)
      totalCount.value = resultTotalCount
      hasNextPage.value = restoredPlans.length < resultTotalCount
      cacheCurrentSearch()
    } catch {
      if (sequence !== requestSequence) return
      plans.value = []
      totalCount.value = 0
      hasNextPage.value = false
      errorMessage.value = INITIAL_LOAD_ERROR
    } finally {
      if (sequence === requestSequence) loading.value = false
    }
  }

  function retryLoad() {
    return loadPlans(searchedKeyword.value, currentPage.value)
  }

  async function search() {
    const trimmed = keyword.value.trim()
    if (!trimmed) return

    searchedKeyword.value = trimmed
    hasSearched.value = true
    currentPage.value = 1
    syncUrl()
    await loadPlans(searchedKeyword.value, 1)
  }

  function searchSuggested(city) {
    keyword.value = city
    return search()
  }

  async function resetSearch() {
    keyword.value = ''
    searchedKeyword.value = ''
    hasSearched.value = false
    currentPage.value = 1
    syncUrl()
    await loadPlans('', 1)
  }

  async function loadMore() {
    if (loadingMore.value || !hasNextPage.value) return

    const sequence = ++requestSequence
    const requestKeyword = searchedKeyword.value
    loadingMore.value = true
    errorMessage.value = ''
    try {
      const nextPage = currentPage.value + 1
      const result = await getPlanList({
        keyword: requestKeyword,
        page: nextPage,
        size: PLAN_SEARCH_PAGE_SIZE,
      })
      if (sequence !== requestSequence) return

      plans.value.push(...result.plans.map(mapPublicPlan))
      currentPage.value = result.page
      totalCount.value = result.totalCount
      hasNextPage.value = result.hasNext
      syncUrl()
      cacheCurrentSearch()
    } catch {
      if (sequence !== requestSequence) return
      errorMessage.value = LOAD_MORE_ERROR
    } finally {
      if (sequence === requestSequence) loadingMore.value = false
    }
  }

  onMounted(() => {
    window.addEventListener('plan-search:reset', resetSearch)
    if (!cachedSearch) loadPlans(searchedKeyword.value, currentPage.value)
  })

  onUnmounted(() => {
    requestSequence += 1
    window.removeEventListener('plan-search:reset', resetSearch)
  })

  return {
    plans,
    loading,
    loadingMore,
    errorMessage,
    totalCount,
    keyword,
    searchedKeyword,
    hasSearched,
    currentPage,
    hasMore,
    openPlanDetail,
    retryLoad,
    search,
    searchSuggested,
    resetSearch,
    loadMore,
  }
}
