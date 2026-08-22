<script setup>
import defaultPlanThumbnail from '@/assets/plan/default-plan-thumbnail.svg'
import { formatCompactCount, getRegionColorKey } from '@/utils/planSearch'

defineProps({
  plan: {
    type: Object,
    required: true,
  },
})

defineEmits(['select'])

function useDefaultThumbnail(event) {
  const image = event.currentTarget
  if (image.dataset.fallbackApplied === 'true') return

  image.dataset.fallbackApplied = 'true'
  image.src = defaultPlanThumbnail
}
</script>

<template>
  <button type="button" class="card" @click="$emit('select', plan.id)">
    <div class="card-img-wrap">
      <img
        class="card-img"
        :src="plan.thumbnailImage || defaultPlanThumbnail"
        alt=""
        @error="useDefaultThumbnail"
      />
      <div class="badge-days">{{ plan.days }}일</div>
    </div>
    <div class="card-body">
      <div class="card-top">
        <span class="region-badge" :class="`region-${getRegionColorKey(plan.region)}`">
          {{ plan.region }}
        </span>
      </div>
      <div class="card-title">{{ plan.title }}</div>
      <div class="card-foot">
        <div class="author">
          <div
            class="avatar"
            :style="plan.authorAvatar ? { backgroundImage: `url(${plan.authorAvatar})` } : {}"
          >
            <span v-if="!plan.authorAvatar">{{ plan.authorInitials }}</span>
          </div>
          <span class="author-name">{{ plan.authorName }}</span>
        </div>
        <div class="stats">
          <span class="stat stat-like">
            <i class="ti ti-heart" aria-hidden="true"></i>
            <span class="sr-only">좋아요 </span>{{ plan.likeCount }}
          </span>
          <span class="stat">
            <i class="ti ti-eye" aria-hidden="true"></i>
            <span class="sr-only">조회수 </span>{{ formatCompactCount(plan.viewCount) }}
          </span>
        </div>
      </div>
    </div>
  </button>
</template>

<style scoped>
.card {
  width: 100%;
  height: 100%;
  overflow: hidden;
  padding: 0;
  border: 0;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 2px 8px rgb(0 0 0 / 6%);
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 32px rgb(0 0 0 / 10%);
}

.card:focus-visible {
  outline: 3px solid var(--color-brand-focus);
  outline-offset: 3px;
}

.card-img-wrap {
  position: relative;
  overflow: hidden;
  height: 220px;
}

.card-img {
  display: block;
  width: 100%;
  height: 100%;
  background-color: #f0f0f0;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.card:hover .card-img {
  transform: scale(1.08);
}

.badge-days {
  position: absolute;
  z-index: 1;
  top: 14px;
  left: 14px;
  padding: 5px 12px;
  border-radius: 14px;
  background: rgb(0 0 0 / 50%);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.card-body {
  padding: 20px 20px 22px;
}

.card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.region-badge {
  padding: 4px 11px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 700;
}

.region-sunset {
  background: #fde8e5;
  color: #b23a24;
}

.region-berry {
  background: #f3e6f8;
  color: #8e3aa8;
}

.region-ocean {
  background: #e2eefc;
  color: #1f5fae;
}

.region-forest {
  background: #e3f2e7;
  color: #24815a;
}

.region-neutral {
  background: #f0f0f0;
  color: #777;
}

.card-title {
  display: -webkit-box;
  overflow: hidden;
  min-height: 50.4px;
  margin-bottom: 16px;
  -webkit-box-orient: vertical;
  color: #1a1a1a;
  font-size: 18px;
  font-weight: 700;
  -webkit-line-clamp: 2;
  letter-spacing: -0.2px;
  line-height: 1.4;
}

.card-foot {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.avatar {
  display: flex;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: var(--color-brand-soft);
  background-position: center;
  background-size: cover;
  color: var(--color-brand);
  font-size: 11px;
  font-weight: 600;
}

.author-name {
  overflow: hidden;
  max-width: 74px;
  color: #666;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stats {
  display: flex;
  flex-shrink: 0;
  gap: 14px;
  margin-left: auto;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #bbb;
  font-size: 13px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
