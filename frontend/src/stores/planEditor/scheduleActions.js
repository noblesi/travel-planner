import {
  addScheduleItem,
  deleteScheduleItem,
  reorderScheduleItems,
  updateScheduleItem,
} from '@/api/plans'
import { createLocalScheduleError } from '@/stores/planEditor/errors'
import { createPlanEditorOperationId } from '@/stores/planEditor/operationId'

export function createPlanEditorScheduleActions({
  plan,
  days,
  selectedDayId,
  enqueueOperation,
}) {
  function currentDay(planDayId) {
    const day = days.value.find((candidate) => candidate.planDayId === planDayId)
    if (!day) throw createLocalScheduleError('선택한 여행 일차를 찾을 수 없습니다.')
    return day
  }

  function currentItem(day, scheduleItemId) {
    const item = day.items?.find((candidate) => candidate.scheduleItemId === scheduleItemId)
    if (!item) {
      throw createLocalScheduleError('일정 항목이 최신 목록에 없어 작업을 건너뛰었습니다.')
    }
    return item
  }

  function orderedTimeSlotItems(day, timeSlot) {
    return [...(day.items ?? [])]
      .filter((candidate) => candidate.timeSlot === timeSlot)
      .sort((left, right) => left.positionNo - right.positionNo)
  }

  function addPlaceToSchedule(place, timeSlot, planDayId = selectedDayId.value) {
    const operationId = createPlanEditorOperationId()
    const planId = plan.value.planId
    return enqueueOperation({
      label: `${place.placeName} 추가`,
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        return addScheduleItem(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot,
          placeProvider: place.placeProvider,
          externalPlaceId: place.externalPlaceId,
          placeName: place.placeName,
          categoryName: place.categoryName ?? null,
          address: place.address ?? null,
          latitude: place.latitude ?? null,
          longitude: place.longitude ?? null,
          imageUrl: place.imageUrl ?? null,
          description: place.description ?? null,
        })
      },
    })
  }

  function moveScheduleItemTimeSlot(
    scheduleItemId,
    timeSlot,
    planDayId = selectedDayId.value,
    targetPlanDayId = planDayId,
  ) {
    const operationId = createPlanEditorOperationId()
    const planId = plan.value.planId
    return enqueueOperation({
      label: timeSlot === 'MORNING' ? '오전으로 이동' : '오후로 이동',
      preferredDayId: targetPlanDayId,
      async run() {
        const day = currentDay(planDayId)
        const targetDay = currentDay(targetPlanDayId)
        const item = currentItem(day, scheduleItemId)
        return updateScheduleItem(planId, planDayId, scheduleItemId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          itemVersion: item.itemVersion,
          timeSlot,
          targetPlanDayId: targetPlanDayId === planDayId ? null : String(targetPlanDayId),
          targetScheduleVersion:
            targetPlanDayId === planDayId ? null : targetDay.scheduleVersion,
        })
      },
    })
  }

  function moveScheduleItemToEnd(scheduleItemId, planDayId = selectedDayId.value) {
    const operationId = createPlanEditorOperationId()
    const planId = plan.value.planId
    return enqueueOperation({
      label: '일정 순서 변경',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        const orderedItems = orderedTimeSlotItems(day, item.timeSlot)
        const currentIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === scheduleItemId,
        )
        if (currentIndex < 0 || currentIndex === orderedItems.length - 1) return null

        const [movedItem] = orderedItems.splice(currentIndex, 1)
        orderedItems.push(movedItem)
        return reorderScheduleItems(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot: item.timeSlot,
          scheduleItemIds: orderedItems.map((candidate) => candidate.scheduleItemId),
        })
      },
    })
  }

  function moveScheduleItemBefore(
    scheduleItemId,
    targetScheduleItemId,
    planDayId = selectedDayId.value,
  ) {
    const operationId = createPlanEditorOperationId()
    const planId = plan.value.planId
    return enqueueOperation({
      label: '일정 순서 변경',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        const targetItem = currentItem(day, targetScheduleItemId)
        if (item.timeSlot !== targetItem.timeSlot || scheduleItemId === targetScheduleItemId) {
          return null
        }

        const orderedItems = orderedTimeSlotItems(day, item.timeSlot)
        const sourceIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === scheduleItemId,
        )
        if (sourceIndex < 0) return null

        const [movedItem] = orderedItems.splice(sourceIndex, 1)
        const targetIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === targetScheduleItemId,
        )
        if (targetIndex < 0) return null

        orderedItems.splice(targetIndex, 0, movedItem)
        return reorderScheduleItems(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot: item.timeSlot,
          scheduleItemIds: orderedItems.map((candidate) => candidate.scheduleItemId),
        })
      },
    })
  }

  function removeScheduleItem(scheduleItemId, planDayId = selectedDayId.value) {
    const operationId = createPlanEditorOperationId()
    const planId = plan.value.planId
    return enqueueOperation({
      label: '일정 삭제',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        return deleteScheduleItem(planId, planDayId, scheduleItemId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          itemVersion: item.itemVersion,
        })
      },
    })
  }

  function moveScheduleItemPosition(
    scheduleItemId,
    direction,
    planDayId = selectedDayId.value,
  ) {
    const operationId = createPlanEditorOperationId()
    const planId = plan.value.planId
    return enqueueOperation({
      label: direction < 0 ? '일정 순서 올리기' : '일정 순서 내리기',
      preferredDayId: planDayId,
      async run() {
        const day = currentDay(planDayId)
        const item = currentItem(day, scheduleItemId)
        const orderedItems = orderedTimeSlotItems(day, item.timeSlot)
        const currentIndex = orderedItems.findIndex(
          (candidate) => candidate.scheduleItemId === scheduleItemId,
        )
        const targetIndex = currentIndex + direction
        if (currentIndex < 0 || targetIndex < 0 || targetIndex >= orderedItems.length) return null

        const reordered = [...orderedItems]
        const targetItem = reordered[targetIndex]
        reordered[targetIndex] = reordered[currentIndex]
        reordered[currentIndex] = targetItem
        return reorderScheduleItems(planId, planDayId, {
          operationId,
          scheduleVersion: day.scheduleVersion,
          timeSlot: item.timeSlot,
          scheduleItemIds: reordered.map((candidate) => candidate.scheduleItemId),
        })
      },
    })
  }

  return {
    addPlaceToSchedule,
    moveScheduleItemTimeSlot,
    moveScheduleItemToEnd,
    moveScheduleItemBefore,
    removeScheduleItem,
    moveScheduleItemPosition,
  }
}
