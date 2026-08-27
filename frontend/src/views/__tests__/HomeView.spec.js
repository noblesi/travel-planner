import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import HomeView from '@/views/HomeView.vue'

describe('HomeView', () => {
  it('서비스 소개와 주요 이동 링크를 표시한다', () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          AppHeader: true,
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })

    expect(wrapper.text()).toContain('여행의 모든 순간을')
    expect(wrapper.text()).toContain('새 일정 만들기')
    expect(wrapper.text()).not.toContain('개발 환경 상태')
  })
})
