export const PLAN_SEARCH_PAGE_SIZE = 8
export const MAX_RESTORED_PAGE = 25
export const MAX_PLAN_SEARCH_BATCH_SIZE = 100

export function parsePlanSearchRoute(query) {
  const keyword = typeof query.keyword === 'string' ? query.keyword : ''
  const legacyPage = Math.ceil((Number(query.count) || PLAN_SEARCH_PAGE_SIZE) / PLAN_SEARCH_PAGE_SIZE)
  const requestedPage = Number(query.page) || legacyPage

  return {
    keyword,
    page: clampPage(requestedPage),
  }
}

export function createPlanRestoreRequests(keyword, targetPage) {
  const page = clampPage(targetPage)
  const targetRecordCount = page * PLAN_SEARCH_PAGE_SIZE
  const requestCount = Math.ceil(targetRecordCount / MAX_PLAN_SEARCH_BATCH_SIZE)
  const requestSize = Math.ceil(targetRecordCount / requestCount)

  return Array.from({ length: requestCount }, (_, index) => ({
    keyword,
    page: index + 1,
    size: requestSize,
  }))
}

export function mapPublicPlan(plan) {
  const authorName = plan.authorName || '여행자'

  return {
    id: plan.planId,
    title: plan.title,
    region: plan.region || '지역 미정',
    days: plan.days,
    likeCount: plan.likeCount,
    viewCount: plan.viewCount,
    authorInitials: Array.from(authorName).slice(0, 2).join(''),
    authorName,
    authorAvatar: plan.authorImage,
    thumbnailImage: plan.thumbnailImage,
  }
}

export function formatCompactCount(value) {
  const count = Number(value) || 0
  if (count >= 1000) return `${(count / 1000).toFixed(1).replace(/\.0$/, '')}k`
  return String(count)
}

const REGION_COLOR_KEYS = ['sunset', 'berry', 'ocean', 'forest']

export function getRegionColorKey(region) {
  const normalizedRegion = typeof region === 'string' ? region : ''
  if (!normalizedRegion) return 'neutral'

  let hash = 0
  for (let index = 0; index < normalizedRegion.length; index += 1) {
    hash = (hash * 31 + normalizedRegion.charCodeAt(index)) % REGION_COLOR_KEYS.length
  }
  return REGION_COLOR_KEYS[hash]
}

function clampPage(value) {
  const integerPage = Math.trunc(Number(value)) || 1
  return Math.min(Math.max(integerPage, 1), MAX_RESTORED_PAGE)
}
