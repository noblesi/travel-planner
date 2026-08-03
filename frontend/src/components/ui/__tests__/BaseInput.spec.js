import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import BaseInput from '@/components/ui/BaseInput.vue'

describe('BaseInput', () => {
  it('label, hint, error를 input과 접근성 속성으로 연결한다', () => {
    const wrapper = mount(BaseInput, {
      props: {
        id: 'email',
        label: '이메일',
        hint: '로그인에 사용할 주소입니다.',
        error: '이메일을 입력해 주세요.',
        required: true,
      },
    })
    const input = wrapper.get('input')

    expect(wrapper.get('label').attributes('for')).toBe('email')
    expect(input.attributes('aria-invalid')).toBe('true')
    expect(input.attributes('aria-describedby')).toBe('email-hint email-error')
    expect(wrapper.get('#email-error').attributes('role')).toBe('alert')
  })

  it('입력 값을 v-model event로 전달한다', async () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: '' },
    })

    await wrapper.get('input').setValue('seoul@example.com')

    expect(wrapper.emitted('update:modelValue')).toEqual([['seoul@example.com']])
  })
})
