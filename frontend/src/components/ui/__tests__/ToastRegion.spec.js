import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'

import ToastRegion from '@/components/ui/ToastRegion.vue'
import { useToastStore } from '@/stores/toast'

describe('ToastRegion', () => {
  it('플랜 제작 화면에서는 중앙 상단 배치 클래스를 제공한다', () => {
    const wrapper = mount(ToastRegion, {
      props: { placement: 'editor' },
      global: { plugins: [createPinia()] },
    })

    expect(wrapper.get('.toast-region').classes()).toContain('toast-region--editor')
  })

  it('toast type에 맞는 live region을 제공하고 사용자가 닫을 수 있다', async () => {
    const pinia = createPinia()
    const wrapper = mount(ToastRegion, {
      global: { plugins: [pinia] },
    })
    const store = useToastStore(pinia)

    store.error('저장하지 못했습니다.', { duration: 0 })
    await wrapper.vm.$nextTick()

    const toast = wrapper.get('.toast')
    expect(toast.attributes('role')).toBe('alert')
    expect(toast.attributes('aria-live')).toBe('assertive')
    expect(toast.text()).toContain('저장하지 못했습니다.')

    await toast.get('button').trigger('click')
    expect(store.toasts).toEqual([])
  })

  it('action 실행 중 중복 클릭과 닫기를 막고 성공 후 toast를 제거한다', async () => {
    let resolveAction
    const action = vi.fn(
      () =>
        new Promise((resolve) => {
          resolveAction = resolve
        }),
    )
    const pinia = createPinia()
    const wrapper = mount(ToastRegion, {
      global: { plugins: [pinia] },
    })
    const store = useToastStore(pinia)
    store.info('삭제한 일정을 복구할 수 있습니다.', {
      duration: 0,
      actionLabel: '실행 취소',
      action,
    })
    await wrapper.vm.$nextTick()

    const actionButton = wrapper.get('.toast__action')
    actionButton.element.click()
    actionButton.element.click()
    await wrapper.vm.$nextTick()

    expect(action).toHaveBeenCalledOnce()
    expect(wrapper.get('.toast').attributes('aria-busy')).toBe('true')
    expect(actionButton.attributes()).toHaveProperty('disabled')
    expect(wrapper.get('[aria-label="알림 닫기"]').attributes()).toHaveProperty('disabled')

    resolveAction()
    await flushPromises()

    expect(store.toasts).toEqual([])
  })

  it('action 실패 후 원래 toast를 유지하고 재시도할 수 있게 한다', async () => {
    const action = vi.fn().mockRejectedValue(new Error('restore failed'))
    const pinia = createPinia()
    const wrapper = mount(ToastRegion, {
      global: { plugins: [pinia] },
    })
    const store = useToastStore(pinia)
    store.info('삭제한 일정을 복구할 수 있습니다.', {
      duration: 0,
      actionLabel: '실행 취소',
      action,
    })
    await wrapper.vm.$nextTick()

    await wrapper.get('.toast__action').trigger('click')
    await flushPromises()

    expect(store.toasts[0].message).toBe('삭제한 일정을 복구할 수 있습니다.')
    expect(store.toasts.at(-1)).toMatchObject({
      type: 'error',
      message: '요청한 작업을 완료하지 못했습니다.',
    })
    expect(wrapper.get('.toast__action').attributes()).not.toHaveProperty('disabled')
    store.clear()
  })
})
