<script setup>
defineProps({
  plan: {
    type: Object,
    required: true,
  },
  likePending: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['back', 'report', 'toggle-like', 'import'])

function formatCount(value) {
  if (value >= 1000) return `${(value / 1000).toFixed(1).replace(/\.0$/, '')}k`
  return String(value)
}
</script>

<template>
  <header class="detail-head">
    <button class="back-link" title="여행플랜 상세페이지로 돌아가기" @click="$emit('back')">
      <i class="ti ti-arrow-left" aria-hidden="true" />
    </button>

    <h1 class="plan-title">{{ plan.title }}</h1>
    <div class="plan-author">
      <span class="author-dot" />{{ plan.authorName }}님의 여행 · {{ plan.periodLabel }}
    </div>

    <div class="head-actions">
      <button class="report-btn" title="신고하기" @click="$emit('report')">
        <i class="ti ti-flag" aria-hidden="true" />
      </button>
      <span class="view-stat">
        <i class="ti ti-eye" aria-hidden="true" /> {{ formatCount(plan.viewCount) }}
      </span>
      <button
        class="like-stat"
        :class="{ liked: plan.liked }"
        :disabled="likePending"
        :aria-busy="likePending"
        :aria-pressed="plan.liked"
        :aria-label="plan.liked ? '좋아요 취소' : '좋아요'"
        @click="$emit('toggle-like')"
      >
        <i class="ti ti-heart" aria-hidden="true" /> {{ plan.likeCount }}
      </button>
      <button class="import-btn" @click="$emit('import')">전체 일정 가져오기</button>
    </div>
  </header>
</template>

<style scoped>
.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 1.5rem;
  flex-shrink: 0;
  min-width: 0;
}
.back-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  color: #1a1a1a;
  border: 0;
  border-radius: 50%;
  background: #f0f0f0;
  font-size: 18px;
  cursor: pointer;
}
.back-link:hover { background: #e4e4e4; }
.plan-title {
  min-width: 0;
  color: #1a1a1a;
  font-size: 22px;
  font-weight: 700;
  white-space: nowrap;
}
.plan-author {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  overflow: hidden;
  color: var(--color-brand);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.author-dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--color-brand-accent);
}
.head-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  gap: 12px;
  margin-left: auto;
}
.like-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  color: #666;
  border: 1px solid #d8d8d8;
  border-radius: 20px;
  background: #f0f0f0;
  font-size: 16px;
  cursor: pointer;
}
.like-stat:hover,
.like-stat.liked {
  color: var(--color-brand);
  border-color: var(--color-brand-border);
  background: var(--color-brand-soft);
}
.like-stat:disabled {
  cursor: wait;
  opacity: 0.65;
}
.view-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #888;
  font-size: 16px;
}
.report-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  color: #666;
  border: 1px solid #d8d8d8;
  border-radius: 50%;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
}
.report-btn:hover { color: var(--color-brand); border-color: var(--color-brand-accent); }
.import-btn {
  padding: 9px 18px;
  color: var(--color-brand-on);
  border: 0;
  border-radius: 20px;
  background: var(--color-brand);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
}
.import-btn:hover { background: var(--color-brand-hover); }
@media (max-width: 760px) {
  .detail-head {
    display: grid;
    grid-template-columns: 36px minmax(0, 1fr);
    gap: 6px 12px;
    margin-bottom: 20px;
  }
  .back-link { grid-row: 1 / span 2; }
  .plan-title { overflow: hidden; text-overflow: ellipsis; }
  .plan-author { grid-column: 2; font-size: 12px; }
  .head-actions {
    grid-column: 1 / -1;
    width: 100%;
    gap: 8px;
    margin-left: 0;
    padding-top: 6px;
    flex-wrap: wrap;
  }
  .import-btn { margin-left: auto; padding: 8px 14px; }
}
@media (max-width: 360px) {
  .import-btn { width: 100%; margin-left: 0; }
}
</style>
