import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'

import {
  clampPlanEditorPanelWidth,
  usePlanEditorPanelResize,
} from '@/composables/usePlanEditorPanelResize'

const TestComponent = defineComponent({
  setup() {
    return usePlanEditorPanelResize()
  },
  render() {
    return h('button', {
      onKeydown: this.adjustPanelWidth,
      onPointerdown: this.startPanelResize,
    })
  },
})

afterEach(() => localStorage.clear())

describe('usePlanEditorPanelResize', () => {
  it('panel 너비를 허용 범위로 제한한다', () => {
    expect(clampPlanEditorPanelWidth(100)).toBe(340)
    expect(clampPlanEditorPanelWidth(450)).toBe(450)
    expect(clampPlanEditorPanelWidth(900)).toBe(560)
  })

  it('keyboard로 너비를 조절하고 localStorage에 저장한다', async () => {
    const wrapper = mount(TestComponent)

    await wrapper.get('button').trigger('keydown', { key: 'ArrowRight' })

    expect(wrapper.vm.schedulePanelWidth).toBe(450)
    expect(localStorage.getItem('planEditorPanelWidth')).toBe('450')
  })

  it('pointer 위치를 범위 안에서 적용하고 종료할 때 저장한다', async () => {
    const wrapper = mount(TestComponent)

    wrapper.get('button').element.dispatchEvent(
      new MouseEvent('pointerdown', { bubbles: true, clientX: 520 }),
    )
    document.dispatchEvent(new Event('pointerup'))

    expect(wrapper.vm.schedulePanelWidth).toBe(520)
    expect(localStorage.getItem('planEditorPanelWidth')).toBe('520')
  })
})
