<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

import PlanEditorSettings from '@/components/plan/PlanEditorSettings.vue'
import ScheduleList from '@/components/plan/ScheduleList.vue'
import { formatKoreanTravelDate } from '@/utils/travelDate'

defineProps({
  plan: { type: Object, required: true },
  days: { type: Array, required: true },
  selectedDayId: { type: [String, Number], default: null },
  selectedDay: { type: Object, default: null },
  isSelectedDayEmpty: { type: Boolean, required: true },
  morningItems: { type: Array, required: true },
  afternoonItems: { type: Array, required: true },
  hasSaveError: { type: Boolean, required: true },
  saveStatus: { type: String, required: true },
  saveErrorMessage: { type: String, default: '' },
  canRetrySave: { type: Boolean, required: true },
  selectedScheduleItemId: { type: [String, Number], default: null },
})

const emit = defineEmits([
  'busy-change',
  'select-day',
  'retry-save',
  'discard-save',
  'move-position',
  'move-time-slot',
  'remove',
  'select-item',
  'drag-start',
  'drag-end',
  'drop-schedule',
  'drop-before',
])
const settingsBusy = ref(false)

function updateBusy(value) {
  settingsBusy.value = value
  emit('busy-change', value)
}
</script>

<template>
  <aside class="schedule-panel" aria-label="여행 일정 편집 영역">
    <header class="schedule-panel__header">
      <div><span>TRAVEL SCHEDULE</span><h2>여행 일정</h2></div>
      <span class="visibility-badge">
        {{
          plan.publishStatus === 'DRAFT'
            ? '작성 중'
            : plan.visibility === 'PUBLIC'
              ? '공개'
              : '비공개'
        }}
      </span>
    </header>
    <div class="plan-summary">
      <div><span>여행 기간</span><strong>{{ days.length }}일</strong></div>
      <div><span>여행 지역</span><strong>{{ plan.regionName }}</strong></div>
    </div>
    <RouterLink
      v-if="plan.canManagePlan !== false"
      class="invite-panel-link"
      :to="{ name: 'invite', params: { id: plan.planId } }"
    >
      동행자 초대 링크 만들기
    </RouterLink>
    <p v-else class="collaborator-notice">동행자는 일정만 편집할 수 있습니다.</p>
    <PlanEditorSettings
      :can-manage-plan="plan.canManagePlan !== false"
      @busy-change="updateBusy"
    />

    <section v-if="hasSaveError" class="save-error" role="alert">
      <div>
        <strong>{{ saveStatus === 'conflict' ? '일정 충돌을 확인해 주세요.' : '자동 저장이 중단되었습니다.' }}</strong>
        <p>{{ saveErrorMessage }}</p>
      </div>
      <div class="save-error__actions">
        <button v-if="canRetrySave" type="button" @click="$emit('retry-save')">같은 작업 다시 시도</button>
        <button type="button" @click="$emit('discard-save')">닫기</button>
      </div>
    </section>

    <nav class="day-tabs" aria-label="여행 일차 선택">
      <button
        v-for="day in days"
        :key="day.planDayId"
        class="day-tab"
        :class="{ 'day-tab--active': day.planDayId === selectedDayId }"
        type="button"
        :aria-pressed="day.planDayId === selectedDayId"
        @click="$emit('select-day', day.planDayId)"
        @dragover.prevent
        @drop.prevent="$emit('drop-schedule', { targetPlanDayId: day.planDayId })"
      >
        <strong>DAY {{ day.dayNo }}</strong>
        <small>{{ formatKoreanTravelDate(day.travelDate) }}</small>
      </button>
    </nav>

    <div class="day-preview">
      <div class="day-preview__label">
        <span>선택된 일정</span>
        <strong v-if="selectedDay">DAY {{ selectedDay.dayNo }}</strong>
        <strong v-else>일정 없음</strong>
        <small v-if="selectedDay">{{ formatKoreanTravelDate(selectedDay.travelDate) }}</small>
      </div>
      <div v-if="!selectedDay" class="empty-schedule" role="status">
        <span class="empty-schedule__mark" aria-hidden="true">!</span>
        <strong>여행 일차를 불러오지 못했습니다.</strong>
        <p>여행 날짜를 다시 확인하거나 잠시 후 다시 시도해 주세요.</p>
      </div>
      <div v-else-if="isSelectedDayEmpty" class="empty-schedule" role="status">
        <span class="empty-schedule__mark" aria-hidden="true">+</span>
        <strong>DAY {{ selectedDay.dayNo }}에 등록된 장소가 없습니다.</strong>
        <p>장소를 검색한 뒤 오전 또는 오후 일정에 바로 추가할 수 있어요.</p>
      </div>
      <div v-else class="schedule-groups">
        <section
          class="schedule-group"
          aria-labelledby="morning-heading"
          @dragover.prevent
          @drop.prevent="$emit('drop-schedule', { targetPlanDayId: selectedDayId, targetTimeSlot: 'MORNING' })"
        >
          <header class="schedule-group__header"><h3 id="morning-heading">오전</h3><span>{{ morningItems.length }}곳</span></header>
          <ScheduleList
            v-if="morningItems.length"
            :items="morningItems"
            time-slot="MORNING"
            :disabled="settingsBusy"
            :selected-schedule-item-id="selectedScheduleItemId"
            @select="$emit('select-item', $event)"
            @drag-start="$emit('drag-start', $event)"
            @drag-end="$emit('drag-end')"
            @drop-before="$emit('drop-before', { targetItem: $event, targetPlanDayId: selectedDayId, targetTimeSlot: 'MORNING' })"
            @move-up="$emit('move-position', $event, -1)"
            @move-down="$emit('move-position', $event, 1)"
            @move-time-slot="$emit('move-time-slot', $event, 'AFTERNOON')"
            @remove="$emit('remove', $event)"
          />
          <p v-else class="schedule-group__empty">오전 일정이 없습니다.</p>
        </section>
        <section
          class="schedule-group"
          aria-labelledby="afternoon-heading"
          @dragover.prevent
          @drop.prevent="$emit('drop-schedule', { targetPlanDayId: selectedDayId, targetTimeSlot: 'AFTERNOON' })"
        >
          <header class="schedule-group__header"><h3 id="afternoon-heading">오후</h3><span>{{ afternoonItems.length }}곳</span></header>
          <ScheduleList
            v-if="afternoonItems.length"
            :items="afternoonItems"
            time-slot="AFTERNOON"
            :disabled="settingsBusy"
            :selected-schedule-item-id="selectedScheduleItemId"
            @select="$emit('select-item', $event)"
            @drag-start="$emit('drag-start', $event)"
            @drag-end="$emit('drag-end')"
            @drop-before="$emit('drop-before', { targetItem: $event, targetPlanDayId: selectedDayId, targetTimeSlot: 'AFTERNOON' })"
            @move-up="$emit('move-position', $event, -1)"
            @move-down="$emit('move-position', $event, 1)"
            @move-time-slot="$emit('move-time-slot', $event, 'MORNING')"
            @remove="$emit('remove', $event)"
          />
          <p v-else class="schedule-group__empty">오후 일정이 없습니다.</p>
        </section>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.schedule-panel { min-width: 0; padding: 24px; overflow-y: auto; border-right: 1px solid #dce3ec; background: #fff; }
.schedule-panel__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.schedule-panel__header span:first-child { color: #ff5a4e; font-size: 9px; font-weight: 850; letter-spacing: .13em; }
.schedule-panel__header h2 { margin: 4px 0 0; color: #172033; font-size: 22px; }
.visibility-badge { padding: 5px 10px; color: #475569; border-radius: 999px; background: #eef2f7; font-size: 10px; font-weight: 800; }
.plan-summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 18px; }
.plan-summary div { display: grid; gap: 5px; padding: 13px; border-radius: 12px; background: #f8fafc; }
.plan-summary span,
.day-preview__label > span { color: #94a3b8; font-size: 9px; font-weight: 800; letter-spacing: .08em; }
.plan-summary strong { overflow: hidden; color: #334155; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.invite-panel-link { display: flex; align-items: center; justify-content: center; min-height: 42px; margin-top: 14px; color: #e8443a; border: 1px solid #ffc2bd; border-radius: 12px; background: #fff8f7; font-size: 13px; font-weight: 800; text-decoration: none; }
.collaborator-notice { margin: 14px 0 0; padding: 11px 12px; color: #475569; border-radius: 10px; background: #f1f5f9; font-size: 11px; text-align: center; }
.save-error { display: grid; gap: 12px; margin-top: 14px; padding: 14px; color: #991b1b; border: 1px solid #fecaca; border-radius: 12px; background: #fef2f2; }
.save-error strong { font-size: 12px; }
.save-error p { margin: 4px 0 0; font-size: 11px; line-height: 1.5; }
.save-error__actions { display: flex; justify-content: flex-end; gap: 8px; }
.save-error__actions button { min-height: 34px; padding: 0 10px; color: #991b1b; border: 1px solid #fca5a5; border-radius: 9px; background: #fff; font-size: 10px; font-weight: 800; cursor: pointer; }
.day-tabs { display: flex; gap: 8px; margin-top: 18px; padding-bottom: 8px; overflow-x: auto; }
.day-tab { display: grid; min-width: 92px; gap: 3px; padding: 10px 12px; color: #64748b; border: 1px solid #dce3ec; border-radius: 11px; background: #fff; text-align: left; cursor: pointer; }
.day-tab small { font-size: 9px; }
.day-tab:hover,
.day-tab--active { color: #e8443a; border-color: #ffaaa3; background: #fff4f2; }
.day-preview { margin-top: 16px; }
.day-preview__label { display: grid; gap: 4px; }
.day-preview__label strong { color: #334155; font-size: 15px; }
.day-preview__label small { color: #64748b; font-size: 10px; }
.empty-schedule { display: grid; justify-items: center; margin-top: 18px; padding: 28px 14px; color: #64748b; border: 1px dashed #cbd5e1; border-radius: 14px; text-align: center; }
.empty-schedule__mark { display: grid; width: 38px; height: 38px; margin-bottom: 10px; place-items: center; color: #e8443a; border-radius: 50%; background: #fff1f0; font-size: 20px; }
.empty-schedule strong { color: #475569; font-size: 12px; }
.empty-schedule p { margin: 7px 0 0; font-size: 10px; line-height: 1.5; }
.schedule-groups { display: grid; gap: 16px; margin-top: 18px; }
.schedule-group { min-width: 0; }
.schedule-group__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 9px; }
.schedule-group__header h3 { margin: 0; color: #334155; font-size: 13px; }
.schedule-group__header span { color: #94a3b8; font-size: 10px; }
.schedule-group__empty { margin: 0; padding: 14px; color: #94a3b8; border-radius: 10px; background: #f8fafc; font-size: 10px; text-align: center; }
</style>
