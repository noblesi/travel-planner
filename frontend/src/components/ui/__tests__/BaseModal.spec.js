import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it } from 'vitest'
import { nextTick } from 'vue'

import BaseModal from '@/components/ui/BaseModal.vue'

afterEach(() => {
  document.body.innerHTML = ''
  document.body.style.overflow = ''
})

describe('BaseModal', () => {
  it('dialog 이름과 설명을 연결하고 열린 동안 body scroll을 잠근다', async () => {
    const wrapper = mount(BaseModal, {
      props: {
        title: '일정 가져오기',
        description: '가져올 일정 정보를 입력해 주세요.',
      },
      slots: { default: '<input aria-label="일정 이름" />' },
      attachTo: document.body,
    })
    await nextTick()

    const dialog = document.body.querySelector('[role="dialog"]')
    const title = document.getElementById(dialog.getAttribute('aria-labelledby'))
    const description = document.getElementById(dialog.getAttribute('aria-describedby'))

    expect(title?.textContent).toBe('일정 가져오기')
    expect(description?.textContent).toBe('가져올 일정 정보를 입력해 주세요.')
    expect(dialog.getAttribute('aria-modal')).toBe('true')
    expect(document.body.style.overflow).toBe('hidden')
    expect(document.activeElement).toBe(document.body.querySelector('.base-modal__close'))

    wrapper.unmount()
    expect(document.body.style.overflow).toBe('')
  })

  it('Escape와 닫기 버튼 동작을 close event로 전달한다', async () => {
    const wrapper = mount(BaseModal, {
      props: { title: '신고하기' },
      attachTo: document.body,
    })
    await nextTick()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    document.body.querySelector('.base-modal__close').click()

    expect(wrapper.emitted('close')).toHaveLength(2)
    wrapper.unmount()
  })
})
