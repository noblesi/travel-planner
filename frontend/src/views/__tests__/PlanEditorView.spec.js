import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PlanEditorView from '@/views/PlanEditorView.vue'

const { getTravelPlanEditorMock } = vi.hoisted(() => ({
  getTravelPlanEditorMock: vi.fn(),
}))

vi.mock('@/api/plans', () => ({
  getTravelPlanEditor: getTravelPlanEditorMock,
}))

const editor = {
  plan: {
    planId: '101',
    title: '서울특별시 여행',
    regionCode: '1',
    regionName: '서울특별시',
    startDate: '2026-08-10',
    endDate: '2026-08-11',
    visibility: 'PRIVATE',
    versionNo: 0,
  },
  days: [
    {
      planDayId: '201',
      dayNo: 1,
      travelDate: '2026-08-10',
      scheduleVersion: 0,
      items: [],
    },
    {
      planDayId: '202',
      dayNo: 2,
      travelDate: '2026-08-11',
      scheduleVersion: 0,
      items: [],
    },
  ],
}

function mountView(planId = '101') {
  return mount(PlanEditorView, {
    props: { planId },
    global: {
      plugins: [createPinia()],
      stubs: {
        RouterLink: { template: '<a><slot /></a>' },
      },
    },
  })
}

beforeEach(() => {
  getTravelPlanEditorMock.mockReset().mockResolvedValue(editor)
})

describe('PlanEditorView', () => {
  it('라우트로 전달된 플랜 ID를 조회하고 제작 화면 정보를 표시한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenCalledWith('101')
    expect(wrapper.text()).toContain('서울특별시 여행')
    expect(wrapper.text()).toContain('여행 일정')
    expect(wrapper.text()).toContain('2일')
    expect(wrapper.text()).toContain('아직 등록된 장소가 없습니다.')
    expect(wrapper.text()).toContain('서울특별시 지도')
  })

  it('플랜 ID prop이 변경되면 새로운 편집 데이터를 조회한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.setProps({ planId: '102' })
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenNthCalledWith(1, '101')
    expect(getTravelPlanEditorMock).toHaveBeenNthCalledWith(2, '102')
  })

  it('조회 실패 메시지를 표시하고 다시 시도한다', async () => {
    getTravelPlanEditorMock.mockRejectedValueOnce({
      response: { status: 404, data: { message: '여행 플랜을 찾을 수 없습니다.' } },
    })
    const wrapper = mountView('999')
    await flushPromises()

    expect(wrapper.text()).toContain('여행 플랜을 찾을 수 없습니다.')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('서울특별시 여행')
  })
})
