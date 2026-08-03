import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useToastStore } from '@/stores/toast'

describe('toast store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('toast를 등록하고 지정 시간 후 제거한다', () => {
    const store = useToastStore()

    const id = store.success('저장되었습니다.', { duration: 1000 })

    expect(id).toBe(1)
    expect(store.toasts).toEqual([
      { id: 1, message: '저장되었습니다.', type: 'success' },
    ])

    vi.advanceTimersByTime(1000)
    expect(store.toasts).toEqual([])
  })

  it('빈 메시지는 무시하고 지원하지 않는 type은 info로 보정한다', () => {
    const store = useToastStore()

    expect(store.show({ message: '   ' })).toBeNull()
    store.show({ message: '안내', type: 'warning', duration: 0 })

    expect(store.toasts).toEqual([{ id: 1, message: '안내', type: 'info' }])
  })

  it('clear가 toast와 예약된 timer를 함께 제거한다', () => {
    const store = useToastStore()
    store.error('실패했습니다.')

    store.clear()
    vi.runAllTimers()

    expect(store.toasts).toEqual([])
  })
})
