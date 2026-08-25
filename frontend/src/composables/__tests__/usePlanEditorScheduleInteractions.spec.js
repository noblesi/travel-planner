import { ref } from 'vue'
import { describe, expect, it, vi } from 'vitest'

import { usePlanEditorScheduleInteractions } from '@/composables/usePlanEditorScheduleInteractions'

function createSubject() {
  const editorStore = {
    addPlaceToSchedule: vi.fn().mockResolvedValue({}),
    moveScheduleItemTimeSlot: vi.fn().mockResolvedValue({}),
    moveScheduleItemToEnd: vi.fn().mockResolvedValue({}),
    moveScheduleItemBefore: vi.fn().mockResolvedValue({}),
    moveScheduleItemPosition: vi.fn().mockResolvedValue({}),
    removeScheduleItem: vi.fn().mockResolvedValue({}),
    retryLastSave: vi.fn().mockResolvedValue({}),
  }
  const toastStore = {
    show: vi.fn(),
    success: vi.fn(),
  }
  const subject = usePlanEditorScheduleInteractions({
    editorStore,
    toastStore,
    selectedDay: ref({ planDayId: 'day-1' }),
    selectedDayId: ref('day-1'),
  })

  return { editorStore, toastStore, subject }
}

describe('usePlanEditorScheduleInteractions', () => {
  it('같은 DAY와 시간대에 drop하면 해당 시간대 마지막으로 이동한다', () => {
    const { editorStore, subject } = createSubject()
    const item = { scheduleItemId: 'item-1', timeSlot: 'MORNING' }

    subject.startScheduleDrag(item)
    subject.dropSchedule({ targetPlanDayId: 'day-1', targetTimeSlot: 'MORNING' })

    expect(editorStore.moveScheduleItemToEnd).toHaveBeenCalledWith('item-1', 'day-1')
    expect(subject.selectedScheduleItemId.value).toBe('item-1')
  })

  it('다른 DAY에 drop하면 출발 DAY를 포함해 시간대를 이동한다', () => {
    const { editorStore, subject } = createSubject()
    const item = { scheduleItemId: 'item-1', timeSlot: 'MORNING' }

    subject.startScheduleDrag(item)
    subject.dropSchedule({ targetPlanDayId: 'day-2', targetTimeSlot: 'AFTERNOON' })

    expect(editorStore.moveScheduleItemTimeSlot).toHaveBeenCalledWith(
      'item-1',
      'AFTERNOON',
      'day-1',
      'day-2',
    )
  })

  it('선택한 일정을 삭제하면 선택을 해제하고 원래 DAY에 복구할 수 있다', async () => {
    const { editorStore, toastStore, subject } = createSubject()
    const item = {
      scheduleItemId: 'item-1',
      placeName: '경복궁',
      timeSlot: 'MORNING',
    }
    subject.selectScheduleItem(item)

    await expect(subject.removeScheduleItem(item)).resolves.toBe(true)

    expect(subject.selectedScheduleItemId.value).toBeNull()
    const undo = toastStore.show.mock.calls[0][0].action
    await undo()
    expect(editorStore.addPlaceToSchedule).toHaveBeenCalledWith(item, 'MORNING', 'day-1')
    expect(toastStore.success).toHaveBeenCalledWith('경복궁 일정을 복구했습니다.')
  })
})
