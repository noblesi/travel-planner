import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AsyncState from '@/components/ui/AsyncState.vue'

describe('AsyncState', () => {
  it('loading 상태를 status와 aria-busy로 알린다', () => {
    const wrapper = mount(AsyncState, {
      props: { variant: 'loading', title: '여행지를 불러오는 중입니다.' },
    })

    expect(wrapper.attributes('role')).toBe('status')
    expect(wrapper.attributes('aria-busy')).toBe('true')
    expect(wrapper.find('.async-state__spinner').exists()).toBe(true)
  })

  it('error 상태와 재시도 동작을 명확히 제공한다', async () => {
    const wrapper = mount(AsyncState, {
      props: {
        variant: 'error',
        title: '불러오지 못했습니다.',
        actionLabel: '다시 시도',
      },
    })

    expect(wrapper.attributes('role')).toBe('alert')
    await wrapper.get('button').trigger('click')
    expect(wrapper.emitted('action')).toHaveLength(1)
  })
})
