import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import BaseButton from '@/components/ui/BaseButton.vue'

describe('BaseButton', () => {
  it('variant, size, block 속성을 공통 class로 반영한다', () => {
    const wrapper = mount(BaseButton, {
      props: {
        variant: 'secondary',
        size: 'lg',
        block: true,
      },
      slots: { default: '저장' },
    })

    expect(wrapper.classes()).toEqual(
      expect.arrayContaining([
        'base-button--secondary',
        'base-button--lg',
        'base-button--block',
      ]),
    )
    expect(wrapper.text()).toBe('저장')
  })

  it('loading 중에는 중복 동작을 막고 상태를 보조기기에 전달한다', () => {
    const wrapper = mount(BaseButton, {
      props: { loading: true },
      slots: { default: '처리 중' },
    })

    expect(wrapper.attributes('disabled')).toBeDefined()
    expect(wrapper.attributes('aria-busy')).toBe('true')
    expect(wrapper.find('.base-button__spinner').exists()).toBe(true)
  })
})
