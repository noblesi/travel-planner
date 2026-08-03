import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DefaultLayout from '@/layouts/DefaultLayout.vue'

describe('DefaultLayout', () => {
  it('공통 Header, main landmark, Footer와 본문 바로가기를 제공한다', () => {
    const wrapper = mount(DefaultLayout, {
      slots: {
        default: '<section data-testid="content">페이지 내용</section>',
      },
      global: {
        stubs: {
          AppHeader: { template: '<header data-testid="header" />' },
          AppFooter: { template: '<footer data-testid="footer" />' },
        },
      },
    })

    expect(wrapper.get('.skip-link').attributes('href')).toBe('#main-content')
    expect(wrapper.get('#main-content').attributes('tabindex')).toBe('-1')
    expect(wrapper.get('[data-testid="header"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="content"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="footer"]').exists()).toBe(true)
  })
})
