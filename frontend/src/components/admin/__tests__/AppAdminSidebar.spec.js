import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import AppAdminSidebar from '@/components/admin/AppAdminSidebar.vue'
import { useToastStore } from '@/stores/toast'

const { logoutAdminMock, replaceMock } = vi.hoisted(() => ({
  logoutAdminMock: vi.fn(),
  replaceMock: vi.fn(),
}))

vi.mock('@/api/adminAuth', () => ({
  logoutAdmin: logoutAdminMock,
}))

vi.mock('vue-router', () => ({
  RouterLink: { template: '<a><slot /></a>' },
  useRouter: () => ({ replace: replaceMock }),
}))

function mountSidebar() {
  const pinia = createPinia()
  const wrapper = mount(AppAdminSidebar, {
    global: { plugins: [pinia] },
  })
  return { pinia, wrapper }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('AppAdminSidebar', () => {
  it('서버 session 로그아웃이 성공한 뒤 로그인 화면으로 이동한다', async () => {
    logoutAdminMock.mockResolvedValue(null)
    replaceMock.mockResolvedValue()
    const { wrapper } = mountSidebar()

    await wrapper.get('.logout-button').trigger('click')
    await flushPromises()

    expect(logoutAdminMock).toHaveBeenCalledTimes(1)
    expect(replaceMock).toHaveBeenCalledWith('/admin/login')
    wrapper.unmount()
  })

  it('로그아웃 실패 시 현재 화면을 유지하고 오류 Toast를 표시한다', async () => {
    logoutAdminMock.mockRejectedValue(new Error('network error'))
    const { pinia, wrapper } = mountSidebar()

    await wrapper.get('.logout-button').trigger('click')
    await flushPromises()

    expect(replaceMock).not.toHaveBeenCalled()
    expect(useToastStore(pinia).toasts.at(-1)).toMatchObject({
      type: 'error',
      message: '로그아웃하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    })

    useToastStore(pinia).clear()
    wrapper.unmount()
  })
})
