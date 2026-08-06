import { ref } from 'vue'
import { defineStore } from 'pinia'

const SEARCH_CACHE_TTL_MS = 5 * 60 * 1000

export const usePlanSearchStore = defineStore('planSearch', () => {
  const cachedSearch = ref(null)

  function cacheSearch(state) {
    cachedSearch.value = {
      ...state,
      plans: state.plans.map((plan) => ({ ...plan })),
      cachedAt: Date.now(),
    }
  }

  function restoreSearch({ searchedKeyword, currentPage }) {
    const cached = cachedSearch.value
    if (!cached) return null

    const isExpired = Date.now() - cached.cachedAt > SEARCH_CACHE_TTL_MS
    const matchesRoute =
      cached.searchedKeyword === searchedKeyword && cached.currentPage === currentPage
    if (isExpired || !matchesRoute) {
      if (isExpired) cachedSearch.value = null
      return null
    }

    return {
      ...cached,
      plans: cached.plans.map((plan) => ({ ...plan })),
    }
  }

  return {
    cacheSearch,
    restoreSearch,
  }
})
