import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import GoogleAuthPlaceholder from '@/components/auth/GoogleAuthPlaceholder.vue'

describe('GoogleAuthPlaceholder', () => {
  it('준비 중인 인증 수단을 실행할 수 없는 button으로 명확히 표시한다', () => {
    const wrapper = mount(GoogleAuthPlaceholder)
    const button = wrapper.get('button')

    expect(button.attributes()).toMatchObject({
      disabled: '',
      title: 'Google 로그인은 준비 중입니다.',
      type: 'button',
    })
    expect(button.text()).toBe('Google 로그인 준비 중')
    expect(button.get('svg').attributes('aria-hidden')).toBe('true')
  })
})
