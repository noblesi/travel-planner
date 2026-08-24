import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import NoticeDetailView from '@/views/Notice/NoticeDetailView.vue'

const { getNoticeDetailMock, pushMock } = vi.hoisted(() => ({
  getNoticeDetailMock: vi.fn(),
  pushMock: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: '1' } }),
  useRouter: () => ({ push: pushMock }),
}))

vi.mock('@/api/notices', () => ({
  getNoticeDetail: getNoticeDetailMock,
}))

function noticeDetail(noticeId, title, category = 'GUIDE') {
  return {
    noticeId,
    category,
    title,
    createdAt: '2026-08-22',
    viewCount: 12,
    content: `${title} 내용`,
  }
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

function mountView(id = '1') {
  return mount(NoticeDetailView, {
    props: { id },
    global: {
      stubs: {
        DefaultLayout: { template: '<main><slot /></main>' },
      },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  getNoticeDetailMock.mockResolvedValue(noticeDetail('1', '서비스 이용 안내'))
})

describe('NoticeDetailView', () => {
  it('공지 상세를 표시하고 목록 route로 이동한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getNoticeDetailMock).toHaveBeenCalledWith('1')
    expect(wrapper.get('h1').text()).toBe('서비스 이용 안내')
    expect(wrapper.text()).toContain('안내')
    expect(wrapper.text()).toContain('2026.08.22')

    await wrapper.get('.back-btn').trigger('click')
    expect(pushMock).toHaveBeenCalledWith({ name: 'notice-list' })

    wrapper.unmount()
  })

  it('route ID가 바뀌면 이전 공지를 지우고 새 공지를 조회한다', async () => {
    const secondRequest = deferred()
    getNoticeDetailMock
      .mockResolvedValueOnce(noticeDetail('1', '첫 번째 공지'))
      .mockReturnValueOnce(secondRequest.promise)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.setProps({ id: '2' })

    expect(getNoticeDetailMock).toHaveBeenLastCalledWith('2')
    expect(wrapper.get('[role="status"]').text()).toContain('불러오는 중')
    expect(wrapper.find('h1').exists()).toBe(false)

    secondRequest.resolve(noticeDetail('2', '두 번째 공지', 'MAINTENANCE'))
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe('두 번째 공지')
    expect(wrapper.text()).toContain('점검')
    wrapper.unmount()
  })

  it('이전 route의 늦은 성공 응답이 최신 공지를 덮지 않는다', async () => {
    const firstRequest = deferred()
    const secondRequest = deferred()
    getNoticeDetailMock
      .mockReset()
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    const wrapper = mountView()

    await wrapper.setProps({ id: '2' })
    secondRequest.resolve(noticeDetail('2', '최신 공지'))
    await flushPromises()
    firstRequest.resolve(noticeDetail('1', '늦게 도착한 이전 공지'))
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe('최신 공지')
    expect(wrapper.text()).not.toContain('늦게 도착한 이전 공지')
    wrapper.unmount()
  })

  it('최신 공지 성공 후 도착한 이전 route 실패를 무시한다', async () => {
    const firstRequest = deferred()
    const secondRequest = deferred()
    getNoticeDetailMock
      .mockReset()
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    const wrapper = mountView()

    await wrapper.setProps({ id: '2' })
    secondRequest.resolve(noticeDetail('2', '최신 공지'))
    await flushPromises()
    firstRequest.reject(new Error('old request failed'))
    await flushPromises()

    expect(wrapper.get('h1').text()).toBe('최신 공지')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
