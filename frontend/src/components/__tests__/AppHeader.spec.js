import { createPinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'

import AppHeader from '@/components/AppHeader.vue'

const routes = [
  { path: '/', name: 'home', component: { template: '<div />' } },
  { path: '/plans', name: 'plan-search', component: { template: '<div />' } },
  {
    path: '/notice',
    name: 'notice-list',
    component: { template: '<div />' },
  },
  { path: '/loginView', name: 'login', component: { template: '<div />' } },
  { path: '/joinView', name: 'join', component: { template: '<div />' } },
  { path: '/myPage', name: 'myPage', component: { template: '<div />' } },
]

async function mountHeader(initialPath = '/') {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(initialPath)
  await router.isReady()

  const wrapper = mount(AppHeader, {
    global: {
      plugins: [createPinia(), router],
    },
  })

  return { router, wrapper }
}

describe('AppHeader', () => {
  it('내부 메뉴를 RouterLink로 렌더링하고 현재 경로를 활성화한다', async () => {
    const { wrapper } = await mountHeader('/plans')

    expect(wrapper.get('a[href="/"]').exists()).toBe(true)
    expect(wrapper.get('a[href="/plans"]').classes()).toContain('router-link-active')
    expect(wrapper.get('a[href="/notice"]').exists()).toBe(true)
  })

  it('모바일 메뉴의 열림 상태를 접근성 속성과 함께 변경한다', async () => {
    const { wrapper } = await mountHeader()
    const menuButton = wrapper.get('button[aria-controls="primary-navigation"]')

    expect(menuButton.attributes('aria-expanded')).toBe('false')

    await menuButton.trigger('click')

    expect(menuButton.attributes('aria-expanded')).toBe('true')
    expect(wrapper.get('#primary-navigation').classes()).toContain('navigation--open')

    await wrapper.get('a[href="/plans"]').trigger('click')

    expect(menuButton.attributes('aria-expanded')).toBe('false')
  })
})
