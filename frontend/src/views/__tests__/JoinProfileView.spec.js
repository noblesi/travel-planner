import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { useUserStore } from '@/stores/useUserStore'
import JoinProfileView from '@/views/joinView/JoinProfileView.vue'

const { postMemberJoinMock, pushMock } = vi.hoisted(() => ({
  postMemberJoinMock: vi.fn(),
  pushMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('@/api/users', () => ({
  postMemberJoin: postMemberJoinMock,
}))

let alertMock

beforeEach(() => {
  setActivePinia(createPinia())
  pushMock.mockReset()
  postMemberJoinMock.mockReset()
  postMemberJoinMock.mockResolvedValue(true)
  alertMock = vi.spyOn(window, 'alert').mockImplementation(() => {})
})

afterEach(() => {
  alertMock.mockRestore()
})

function mountViewWithStep1Data() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useUserStore()
  store.setUserInfo('member@example.com', 'password-value')
  const wrapper = mount(JoinProfileView, { global: { plugins: [pinia] } })
  return { store, wrapper }
}

describe('JoinProfileView', () => {
  it('동의한 프로필 정보를 서버에 전송하고 완료 화면으로 이동한다', async () => {
    const { store, wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('input[type="radio"][value="F"]').setValue()
    await wrapper.get('input[type="checkbox"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(postMemberJoinMock).toHaveBeenCalledWith({
      email: 'member@example.com',
      password: 'password-value',
      birth: '20000101',
      privacy: 'Y',
      name: '홍길동',
      gender: 'F',
      phone: '010-1234-5678',
    })
    expect(store.userInfo).toEqual({ email: '', password: '' })
    expect(pushMock).toHaveBeenCalledWith('/joinComplete')

    wrapper.unmount()
  })

  it('8자리가 아닌 생년월일은 전송하지 않는다', async () => {
    const { wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('2000010')
    await wrapper.get('form').trigger('submit')

    expect(alertMock).toHaveBeenCalledWith(
      '생년월일을 8자리로 정확하게 입력하여 주세요. (예: 20001031)',
    )
    expect(postMemberJoinMock).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('개인정보 저장에 동의하지 않으면 전송하지 않는다', async () => {
    const { wrapper } = mountViewWithStep1Data()
    const inputs = wrapper.findAll('input[type="text"]')

    await inputs[0].setValue('홍길동')
    await inputs[1].setValue('20000101')
    await inputs[2].setValue('010-1234-5678')
    await wrapper.get('form').trigger('submit')

    expect(alertMock).toHaveBeenCalledWith('개인정보 저장에 동의하여 주세요.')
    expect(postMemberJoinMock).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()

    wrapper.unmount()
  })
})
