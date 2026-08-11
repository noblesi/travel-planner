<script setup>
import { RouterLink } from 'vue-router'

import { formatKoreanTravelDate } from '@/utils/travelDate'

const props = defineProps({
  plan: { type: Object, default: null },
  isReady: { type: Boolean, required: true },
  isSaving: { type: Boolean, required: true },
  saveStatus: { type: String, required: true },
  saveMessage: { type: String, required: true },
  pendingSaveCount: { type: Number, required: true },
  publicationBusy: { type: Boolean, default: false },
})

defineEmits(['toggle-publication'])
</script>

<template>
  <header class="editor-toolbar">
    <div class="editor-toolbar__inner">
      <RouterLink class="back-button" :to="{ name: 'home' }" aria-label="홈으로 돌아가기">
        <span aria-hidden="true">←</span>
      </RouterLink>
      <div class="plan-heading">
        <span class="plan-heading__eyebrow">WITH TRIP PLANNER</span>
        <template v-if="plan">
          <h1>{{ plan.title }}</h1>
          <p>
            {{ plan.regionName }} · {{ formatKoreanTravelDate(plan.startDate) }} -
            {{ formatKoreanTravelDate(plan.endDate) }}
          </p>
        </template>
        <template v-else>
          <h1>여행 플랜 제작</h1>
          <p>여행 정보를 불러오는 중입니다.</p>
        </template>
      </div>
      <div class="editor-toolbar__actions">
        <span
          v-if="isReady"
          class="save-state"
          :class="`save-state--${saveStatus}`"
          role="status"
          aria-live="polite"
        >
          <span aria-hidden="true" />
          {{ saveMessage }}
          <small v-if="pendingSaveCount > 1">{{ pendingSaveCount }}건</small>
        </span>
        <button
          v-if="plan && plan.canManagePlan !== false"
          class="complete-button"
          type="button"
          :disabled="!isReady || isSaving || publicationBusy"
          :aria-busy="publicationBusy"
          @click="$emit('toggle-publication')"
        >
          {{
            publicationBusy
              ? '처리 중...'
              : props.plan?.publishStatus === 'PUBLISHED'
                ? '작성 중으로 전환'
                : '제작 완료'
          }}
        </button>
        <RouterLink class="exit-button" :to="{ name: 'home' }">
          {{ isSaving ? '저장 후 나가기' : '나가기' }}
        </RouterLink>
      </div>
    </div>
  </header>
</template>

<style scoped>
.editor-toolbar {
  position: sticky;
  z-index: 20;
  top: 0;
  border-bottom: 1px solid #e1e7ef;
  background: rgb(255 255 255 / 96%);
  backdrop-filter: blur(18px);
}
.editor-toolbar__inner {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  min-height: 82px;
  gap: 18px;
  padding: 12px 24px;
}
.back-button,
.exit-button,
.complete-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border: 1px solid #dce3ec;
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
}
.back-button { width: 42px; color: #334155; font-size: 20px; }
.plan-heading { min-width: 0; }
.plan-heading__eyebrow { color: var(--color-brand); font-size: 9px; font-weight: 850; letter-spacing: .14em; }
.plan-heading h1 {
  margin: 3px 0 0;
  overflow: hidden;
  color: #172033;
  font-size: clamp(18px, 2vw, 25px);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plan-heading p { margin: 4px 0 0; color: #64748b; font-size: 12px; }
.editor-toolbar__actions { display: flex; align-items: center; gap: 12px; }
.save-state { display: inline-flex; align-items: center; gap: 7px; color: #64748b; font-size: 11px; }
.save-state > span { width: 8px; height: 8px; border-radius: 50%; background: #94a3b8; }
.save-state small { color: #475569; font-weight: 800; }
.save-state--saving > span { background: #f59e0b; box-shadow: 0 0 0 4px rgb(245 158 11 / 14%); }
.save-state--error,
.save-state--conflict { color: #b91c1c; }
.save-state--error > span,
.save-state--conflict > span { background: #ef4444; }
.exit-button { padding: 0 18px; color: #334155; }
.complete-button { padding: 0 16px; color: var(--color-brand-on); border-color: var(--color-brand); background: var(--color-brand); font-weight: 800; cursor: pointer; }
.complete-button:disabled { cursor: wait; opacity: .55; }
</style>
