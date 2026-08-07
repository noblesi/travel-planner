import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'

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
})
