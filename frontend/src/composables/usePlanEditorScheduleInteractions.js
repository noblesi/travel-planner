import { ref } from 'vue'

export function usePlanEditorScheduleInteractions({
  editorStore,
  toastStore,
  selectedDay,
  selectedDayId,
}) {
  const selectedScheduleItemId = ref(null)
  const draggedSchedule = ref(null)

  async function runScheduleOperation(operation) {
    try {
      return await operation
    } catch {
      return null
    }
  }

  function addSearchPlace({ place, timeSlot }) {
    if (!selectedDay.value) return null
    return runScheduleOperation(editorStore.addPlaceToSchedule(place, timeSlot))
  }

  function moveScheduleItem(item, targetTimeSlot) {
    return runScheduleOperation(
      editorStore.moveScheduleItemTimeSlot(item.scheduleItemId, targetTimeSlot),
    )
  }

  function selectScheduleItem(item) {
    selectedScheduleItemId.value = item?.scheduleItemId ?? null
  }

  function startScheduleDrag(item) {
    draggedSchedule.value = {
      item,
      sourcePlanDayId: selectedDayId.value,
      sourceTimeSlot: item.timeSlot,
    }
  }

  function endScheduleDrag() {
    draggedSchedule.value = null
  }

  function dropSchedule({ targetPlanDayId, targetTimeSlot }) {
    const dragged = draggedSchedule.value
    if (!dragged || !targetPlanDayId) return

    const nextTimeSlot = targetTimeSlot || dragged.sourceTimeSlot
    const sameDay = String(targetPlanDayId) === String(dragged.sourcePlanDayId)
    const sameSlot = nextTimeSlot === dragged.sourceTimeSlot

    if (sameDay && sameSlot) {
      runScheduleOperation(
        editorStore.moveScheduleItemToEnd(
          dragged.item.scheduleItemId,
          dragged.sourcePlanDayId,
        ),
      )
    } else {
      runScheduleOperation(
        editorStore.moveScheduleItemTimeSlot(
          dragged.item.scheduleItemId,
          nextTimeSlot,
          dragged.sourcePlanDayId,
          targetPlanDayId,
        ),
      )
    }

    selectedScheduleItemId.value = dragged.item.scheduleItemId
    draggedSchedule.value = null
  }

  function dropScheduleBefore({ targetItem, targetPlanDayId, targetTimeSlot }) {
    const dragged = draggedSchedule.value
    if (!dragged || !targetItem) return

    const sameDay = String(targetPlanDayId) === String(dragged.sourcePlanDayId)
    const sameSlot = targetTimeSlot === dragged.sourceTimeSlot
    if (sameDay && sameSlot) {
      runScheduleOperation(
        editorStore.moveScheduleItemBefore(
          dragged.item.scheduleItemId,
          targetItem.scheduleItemId,
          targetPlanDayId,
        ),
      )
      draggedSchedule.value = null
      return
    }

    dropSchedule({ targetPlanDayId, targetTimeSlot })
  }

  function moveScheduleItemPosition(item, direction) {
    return runScheduleOperation(
      editorStore.moveScheduleItemPosition(item.scheduleItemId, direction),
    )
  }

  async function removeScheduleItem(item) {
    const sourcePlanDayId = selectedDayId.value
    try {
      const result = await editorStore.removeScheduleItem(item.scheduleItemId)
      if (!result) return null
      if (String(selectedScheduleItemId.value) === String(item.scheduleItemId)) {
        selectedScheduleItemId.value = null
      }
      toastStore.show({
        message: `${item.placeName} 일정을 삭제했습니다.`,
        type: 'info',
        duration: 8000,
        actionLabel: '실행 취소',
        action: async () => {
          await editorStore.addPlaceToSchedule(item, item.timeSlot, sourcePlanDayId)
          toastStore.success(`${item.placeName} 일정을 복구했습니다.`)
        },
      })
    } catch {
      return null
    }
    return true
  }

  function retryScheduleSave() {
    return runScheduleOperation(editorStore.retryLastSave())
  }

  return {
    selectedScheduleItemId,
    addSearchPlace,
    moveScheduleItem,
    selectScheduleItem,
    startScheduleDrag,
    endScheduleDrag,
    dropSchedule,
    dropScheduleBefore,
    moveScheduleItemPosition,
    removeScheduleItem,
    retryScheduleSave,
  }
}
