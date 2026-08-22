import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import MyPage from '@/views/myPage/MyPage.vue'

const {
  changeMyPasswordMock,
  getMyProfileMock,
  updateMyProfileMock,
  withdrawMyAccountMock,
} = vi.hoisted(() => ({
  changeMyPasswordMock: vi.fn(),
  getMyProfileMock: vi.fn(),
  updateMyProfileMock: vi.fn(),
  withdrawMyAccountMock: vi.fn(),
}))

vi.mock('@/api/member', () => ({
  changeMyPassword: changeMyPasswordMock,
  getMyProfile: getMyProfileMock,
  updateMyProfile: updateMyProfileMock,
  withdrawMyAccount: withdrawMyAccountMock,
}))

const profile = {
  memberId: '1',
  name: '김여행',
  email: 'traveler@example.com',
  nickname: '주말여행자',
  birthDate: '1998-04-02',
  genderCode: 'F',
  phoneNumber: '010-1234-5678',
  profileImageUrl: null,
}

async function mountPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/myPage', name: 'myPage', component: { template: '<div />' } },
      { path: '/passwordFind', name: 'passwordFind', component: { template: '<div />' } },
    ],
  })
  await router.push('/myPage')
  await router.isReady()
  const wrapper = mount(MyPage, {
    global: {
      plugins: [pinia, router],
      stubs: {
        DefaultLayout: { template: '<div><slot /></div>' },
        RouterLink: { template: '<a><slot /></a>' },
        BaseModal: {
          props: ['open'],
          emits: ['close'],
          template: '<div v-if="open" data-testid="withdrawal-modal"><slot /><slot name="footer" /></div>',
        },
      },
    },
  })
  return { pinia, router, wrapper }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('MyPage', () => {
  it('API에서 조회한 현재 회원정보를 표시한다', async () => {
    getMyProfileMock.mockResolvedValue(profile)

    const { wrapper } = await mountPage()
    await flushPromises()

    expect(getMyProfileMock).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('김여행')
    expect(wrapper.text()).toContain('주말여행자')
    expect(wrapper.text()).toContain('traveler@example.com')
    expect(wrapper.text()).toContain('여성')
    expect(wrapper.text()).toContain('1998년 4월 2일')
  })

  it('조회 실패 메시지를 표시하고 다시 시도할 수 있다', async () => {
    getMyProfileMock
      .mockRejectedValueOnce({ response: { data: { message: '세션이 만료되었습니다.' } } })
      .mockResolvedValueOnce(profile)

    const { wrapper } = await mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('세션이 만료되었습니다.')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(getMyProfileMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('주말여행자')
  })

  it('profile 응답이 비어 있으면 안전한 오류 상태를 표시한다', async () => {
    getMyProfileMock.mockResolvedValue(null)

    const { wrapper } = await mountPage()
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('회원 정보를 불러오지 못했습니다.')
    expect(wrapper.find('.profile-content').exists()).toBe(false)
  })

  it('수정한 회원정보를 저장하고 인증 스토어의 닉네임을 동기화한다', async () => {
    const updatedProfile = {
      ...profile,
      name: '김새이름',
      nickname: '새닉네임',
      genderCode: 'N',
    }
    getMyProfileMock.mockResolvedValue(profile)
    updateMyProfileMock.mockResolvedValue(updatedProfile)

    const { wrapper } = await mountPage()
    const authStore = useAuthStore()
    authStore.setCurrentUser({
      memberId: '1',
      email: profile.email,
      displayName: profile.nickname,
    })
    await flushPromises()

    await wrapper.get('.primary-action').trigger('click')
    await wrapper.get('input[name="name"]').setValue('김새이름')
    await wrapper.get('input[name="nickname"]').setValue('새닉네임')
    await wrapper.get('select[name="genderCode"]').setValue('N')
    await wrapper.get('form.profile-details').trigger('submit')
    await flushPromises()

    expect(updateMyProfileMock).toHaveBeenCalledWith({
      name: '김새이름',
      nickname: '새닉네임',
      genderCode: 'N',
      birthDate: '1998-04-02',
      phoneNumber: '010-1234-5678',
    })
    expect(authStore.currentUser.displayName).toBe('새닉네임')
    expect(wrapper.text()).toContain('김새이름')
    expect(wrapper.text()).toContain('새닉네임')
  })

  it('수정을 취소하면 입력값을 저장하지 않고 기존 정보를 유지한다', async () => {
    getMyProfileMock.mockResolvedValue(profile)
    const { wrapper } = await mountPage()
    await flushPromises()

    await wrapper.get('.primary-action').trigger('click')
    await wrapper.get('input[name="name"]').setValue('취소할 이름')
    const cancelButton = wrapper.findAll('button').find((button) => button.text() === '취소')
    await cancelButton.trigger('click')

    expect(updateMyProfileMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('김여행')
    expect(wrapper.find('input[name="name"]').exists()).toBe(false)
  })

  it('저장 실패 시 입력값과 서버 검증 메시지를 유지한다', async () => {
    getMyProfileMock.mockResolvedValue(profile)
    updateMyProfileMock.mockRejectedValue({
      response: {
        data: {
          message: '요청 값이 올바르지 않습니다.',
          errors: [{ field: 'nickname', message: '이미 사용할 수 없는 닉네임입니다.' }],
        },
      },
    })

    const { wrapper } = await mountPage()
    await flushPromises()
    await wrapper.get('.primary-action').trigger('click')
    await wrapper.get('input[name="nickname"]').setValue('중복닉네임')
    await wrapper.get('form.profile-details').trigger('submit')
    await flushPromises()

    expect(wrapper.get('input[name="nickname"]').element.value).toBe('중복닉네임')
    expect(wrapper.text()).toContain('이미 사용할 수 없는 닉네임입니다.')
    expect(wrapper.text()).toContain('요청 값이 올바르지 않습니다.')
  })

  it('현재 비밀번호 확인 후 회원탈퇴하고 세션을 정리한다', async () => {
    getMyProfileMock.mockResolvedValue(profile)
    withdrawMyAccountMock.mockResolvedValue(null)

    const { router, wrapper } = await mountPage()
    const authStore = useAuthStore()
    authStore.setCurrentUser({
      memberId: '1',
      email: profile.email,
      displayName: profile.nickname,
    })
    await flushPromises()

    await wrapper.get('.withdraw-open-button').trigger('click')
    await wrapper.get('#withdrawal-password').setValue('WithTrip-E2E-2026!')
    await wrapper.get('#withdraw-account-form').trigger('submit')
    await flushPromises()

    expect(withdrawMyAccountMock).toHaveBeenCalledWith('WithTrip-E2E-2026!')
    expect(authStore.isAuthenticated).toBe(false)
    expect(router.currentRoute.value.name).toBe('home')
  })

  it('회원탈퇴 실패 시 비밀번호와 오류 메시지를 유지한다', async () => {
    getMyProfileMock.mockResolvedValue(profile)
    withdrawMyAccountMock.mockRejectedValue({
      response: { data: { message: '현재 비밀번호가 올바르지 않습니다.' } },
    })

    const { wrapper } = await mountPage()
    await flushPromises()
    await wrapper.get('.withdraw-open-button').trigger('click')
    await wrapper.get('#withdrawal-password').setValue('wrong-password')
    await wrapper.get('#withdraw-account-form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('#withdrawal-password').element.value).toBe('wrong-password')
    expect(wrapper.text()).toContain('현재 비밀번호가 올바르지 않습니다.')
  })

  it('현재 비밀번호 확인 후 새 비밀번호로 변경한다', async () => {
    getMyProfileMock.mockResolvedValue(profile)
    changeMyPasswordMock.mockResolvedValue(null)

    const { wrapper } = await mountPage()
    await flushPromises()
    const passwordButton = wrapper.findAll('button').find((button) => button.text() === '비밀번호 변경')
    await passwordButton.trigger('click')
    await wrapper.get('#current-password').setValue('WithTrip-E2E-2026!')
    await wrapper.get('#new-password').setValue('New-WithTrip-2026!')
    await wrapper.get('#new-password-confirm').setValue('New-WithTrip-2026!')
    await wrapper.get('#change-password-form').trigger('submit')
    await flushPromises()

    expect(changeMyPasswordMock).toHaveBeenCalledWith({
      currentPassword: 'WithTrip-E2E-2026!',
      newPassword: 'New-WithTrip-2026!',
    })
    expect(wrapper.find('#change-password-form').exists()).toBe(false)
  })

  it('새 비밀번호 확인이 다르면 API를 호출하지 않는다', async () => {
    getMyProfileMock.mockResolvedValue(profile)

    const { wrapper } = await mountPage()
    await flushPromises()
    const passwordButton = wrapper.findAll('button').find((button) => button.text() === '비밀번호 변경')
    await passwordButton.trigger('click')
    await wrapper.get('#current-password').setValue('WithTrip-E2E-2026!')
    await wrapper.get('#new-password').setValue('New-WithTrip-2026!')
    await wrapper.get('#new-password-confirm').setValue('Different-Password!')
    await wrapper.get('#change-password-form').trigger('submit')

    expect(changeMyPasswordMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('새 비밀번호 확인이 일치하지 않습니다.')
  })
})
