import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useUserStore } from '@/stores/useUserStore'
import JoinProfileView from '@/views/joinView/JoinProfileView.vue'

const { pushMock } = vi.hoisted(() => ({
  pushMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

let alertMock

beforeEach(() => {
  setActivePinia(createPinia())
  pushMock.mockReset()
  alertMock = vi.spyOn(window, 'alert').mockImplementation(() => {})
})

afterEach(() => {
  alertMock.mockRestore()
})

function mountView() {
  return mount(JoinProfileView, {
    global: {
      plugins: [createPinia()],
    },
  })
}

describe('JoinProfileView', () => {
  it('프로필 정보와 선택한 성별을 Store에 저장한다', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(JoinProfileView, { global: { plugins: [pinia] } })
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('  홍길동  ')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('input[type="radio"][value="F"]').setValue()
    await wrapper.get('form').trigger('submit')

    const store = useUserStore()
    expect(store.tempBirth).toBe('20000101')
    expect(store.tempName).toBe('홍길동')
    expect(store.tempGender).toBe('F')
    expect(store.tempPhone).toBe('010-1234-5678')
    expect(pushMock).toHaveBeenCalledWith({ name: 'complete' })

    wrapper.unmount()
  })

  it('8자리가 아닌 생년월일은 저장하지 않는다', async () => {
    const wrapper = mountView()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('2000010')
    await wrapper.get('form').trigger('submit')

    expect(alertMock).toHaveBeenCalledWith('생년월일을 정확하게 입력하여주세요.')
    expect(pushMock).not.toHaveBeenCalled()

    wrapper.unmount()
  })
})
