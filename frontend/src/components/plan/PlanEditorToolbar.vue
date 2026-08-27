<script setup>
import { RouterLink } from 'vue-router'

import PlanEditorSettings from '@/components/plan/PlanEditorSettings.vue'
import { formatKoreanTravelDate } from '@/utils/travelDate'

const props = defineProps({
  plan: { type: Object, default: null },
  daysCount: { type: Number, default: 0 },
  isReady: { type: Boolean, required: true },
  isSaving: { type: Boolean, required: true },
  saveStatus: { type: String, required: true },
  saveMessage: { type: String, required: true },
  pendingSaveCount: { type: Number, required: true },
  publicationBusy: { type: Boolean, default: false },
})

defineEmits(['busy-change', 'toggle-publication'])
</script>

<template>
  <header class="editor-toolbar">
    <div class="editor-toolbar__inner">
      <RouterLink class="back-button" :to="{ name: 'home' }" aria-label="홈으로 돌아가기">
        <span aria-hidden="true">←</span>
      </RouterLink>
      <div class="editor-toolbar__content">
        <div class="plan-heading">
          <span class="plan-heading__eyebrow">WITH TRIP PLANNER</span>
          <template v-if="plan">
            <div class="plan-heading__title-row">
              <h1>{{ plan.title }}</h1>
              <span class="visibility-badge">
                {{
                  plan.publishStatus === 'DRAFT'
                    ? '작성 중'
                    : plan.visibility === 'PUBLIC'
                      ? '공개'
                      : '비공개'
                }}
              </span>
            </div>
            <p>
              {{ formatKoreanTravelDate(plan.startDate) }} -
              {{ formatKoreanTravelDate(plan.endDate) }}
            </p>
          </template>
          <template v-else>
            <h1>여행 플랜 제작</h1>
            <p>여행 정보를 불러오는 중입니다.</p>
          </template>
        </div>

        <div v-if="plan" class="plan-management" aria-label="여행 플랜 관리">
          <dl class="plan-facts">
            <div>
              <dt>여행 기간</dt>
              <dd>{{ daysCount }}일</dd>
            </div>
            <div>
              <dt>여행 지역</dt>
              <dd>{{ plan.regionName }}</dd>
            </div>
          </dl>
          <template v-if="plan.canManagePlan !== false">
            <RouterLink
              class="invite-toolbar-link"
              :to="{ name: 'invite', params: { id: plan.planId } }"
            >
              동행자 초대
            </RouterLink>
            <PlanEditorSettings
              :can-manage-plan="true"
              @busy-change="$emit('busy-change', $event)"
            />
          </template>
          <p v-else class="collaborator-notice">동행자는 일정만 편집할 수 있습니다.</p>
        </div>
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
  min-height: 106px;
  gap: 18px;
  padding: 10px 24px;
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
.back-button {
  width: 42px;
  color: #334155;
  font-size: 20px;
}
.editor-toolbar__content {
  display: grid;
  min-width: 0;
  gap: 6px;
}
.plan-heading {
  min-width: 0;
}
.plan-heading__eyebrow {
  color: var(--color-brand);
  font-size: 9px;
  font-weight: 850;
  letter-spacing: 0.14em;
}
.plan-heading__title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 9px;
}
.plan-heading h1 {
  margin: 3px 0 0;
  overflow: hidden;
  color: #172033;
  font-size: clamp(18px, 1.7vw, 23px);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.plan-heading p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 11px;
}
.visibility-badge {
  flex: 0 0 auto;
  padding: 4px 8px;
  color: #475569;
  border-radius: 999px;
  background: #eef2f7;
  font-size: 9px;
  font-weight: 800;
}
.plan-management {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}
.plan-facts {
  display: flex;
  min-width: 0;
  margin: 0;
  gap: 6px;
}
.plan-facts > div {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border-radius: 8px;
  background: #f8fafc;
}
.plan-facts dt {
  color: #94a3b8;
  font-size: 9px;
  font-weight: 750;
}
.plan-facts dd {
  max-width: 120px;
  margin: 0;
  overflow: hidden;
  color: #334155;
  font-size: 10px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.invite-toolbar-link {
  display: inline-flex;
  min-height: 30px;
  flex: 0 0 auto;
  align-items: center;
  padding: 0 10px;
  color: var(--color-brand);
  border: 1px solid var(--color-brand-border);
  border-radius: 9px;
  background: var(--color-brand-soft);
  font-size: 10px;
  font-weight: 800;
  text-decoration: none;
}
.collaborator-notice {
  margin: 0;
  color: #64748b;
  font-size: 10px;
}
.plan-management :deep(.plan-editor-settings) {
  display: flex;
  position: relative;
  align-items: center;
  gap: 6px;
}
.plan-management :deep(.metadata-editor),
.plan-management :deep(.date-editor) {
  position: relative;
  margin-top: 0;
}
.plan-management :deep(.metadata-editor__open),
.plan-management :deep(.date-editor__open) {
  width: auto;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 9px;
  font-size: 10px;
}
.plan-management :deep(.metadata-editor__form),
.plan-management :deep(.date-editor__form) {
  position: absolute;
  z-index: 40;
  top: calc(100% + 12px);
  right: 0;
  width: 360px;
  box-shadow: 0 18px 50px rgb(15 23 42 / 20%);
}
.editor-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.save-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #64748b;
  font-size: 11px;
}
.save-state > span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
}
.save-state small {
  color: #475569;
  font-weight: 800;
}
.save-state--saving > span {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgb(245 158 11 / 14%);
}
.save-state--error,
.save-state--conflict {
  color: #b91c1c;
}
.save-state--error > span,
.save-state--conflict > span {
  background: #ef4444;
}
.exit-button {
  padding: 0 18px;
  color: #334155;
}
.complete-button {
  padding: 0 16px;
  color: var(--color-brand-on);
  border-color: var(--color-brand);
  background: var(--color-brand);
  font-weight: 800;
  cursor: pointer;
}
.complete-button:disabled {
  cursor: wait;
  opacity: 0.55;
}
@media (max-width: 1480px) {
  .plan-facts > div:first-child {
    display: none;
  }
  .plan-facts dd {
    max-width: 90px;
  }
  .invite-toolbar-link {
    font-size: 0;
  }
  .invite-toolbar-link::after {
    font-size: 10px;
    content: '초대';
  }
}
</style>
