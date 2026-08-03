import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'

import HomeView from '@/views/HomeView.vue'

vi.mock('@/api/system', () => ({
  getHealth: vi.fn().mockResolvedValue({ status: 'UP', application: 'withtrip' }),
}))

describe('HomeView', () => {
  it('백엔드 연결 상태를 표시한다', async () => {
    const wrapper = mount(HomeView, {
      global: {
        stubs: {
          AppHeader: true,
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('여행의 모든 순간을')
    expect(wrapper.text()).toContain('Spring Boot API: 연결됨')
  })
})
