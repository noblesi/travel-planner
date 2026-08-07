import { createPinia } from 'pinia'
import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import MyPlansView from '@/views/MyPlansView.vue'

const { deleteTravelPlanMock, getMyTravelPlansMock, restoreTravelPlanMock } = vi.hoisted(() => ({
  deleteTravelPlanMock: vi.fn(),
  getMyTravelPlansMock: vi.fn(),
  restoreTravelPlanMock: vi.fn(),
}))

vi.mock('@/api/plans', () => ({
  deleteTravelPlan: deleteTravelPlanMock,
  getMyTravelPlans: getMyTravelPlansMock,
  restoreTravelPlan: restoreTravelPlanMock,
}))

const activePlan = {
  planId: '101',
  title: '서울 여행',
  regionName: '서울특별시',
  startDate: '2026-08-10',
  endDate: '2026-08-11',
  visibility: 'PUBLIC',
  publishStatus: 'DRAFT',
  planStatus: 'ACTIVE',
  versionNo: 2,
  currentMemberRole: 'CREATOR',
}

const deletedPlan = {
  ...activePlan,
  planId: '102',
  title: '삭제한 부산 여행',
  planStatus: 'DELETED',
  versionNo: 4,
}

function mountView() {
  return mount(MyPlansView, {
    global: {
      plugins: [createPinia()],
      stubs: {
        DefaultLayout: { template: '<div><slot /></div>' },
        RouterLink: RouterLinkStub,
      },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  getMyTravelPlansMock.mockResolvedValue({ plans: [activePlan, deletedPlan] })
  deleteTravelPlanMock.mockResolvedValue({ planStatus: 'DELETED', versionNo: 3 })
  restoreTravelPlanMock.mockResolvedValue({ planStatus: 'ACTIVE', versionNo: 5 })
})

describe('MyPlansView', () => {
  it('진행 중 플랜을 조회하고 제작 화면 링크를 제공한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getMyTravelPlansMock).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('서울 여행')
    expect(wrapper.text()).not.toContain('삭제한 부산 여행')
    expect(wrapper.getComponent(RouterLinkStub).exists()).toBe(true)
  })

  it('삭제된 플랜을 필터링하고 복구한다', async () => {
    const wrapper = mountView()
    await flushPromises()
    const deletedFilter = wrapper.findAll('.plan-filters button').find((button) => button.text() === '삭제된 플랜')

    await deletedFilter.trigger('click')
    expect(wrapper.text()).toContain('삭제한 부산 여행')

    await wrapper.get('.restore-button').trigger('click')
    await flushPromises()

    expect(restoreTravelPlanMock).toHaveBeenCalledWith('102', 4)
    expect(getMyTravelPlansMock).toHaveBeenCalledTimes(2)
  })

  it('확인 후 소유 플랜을 삭제한다', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const wrapper = mountView()
    await flushPromises()

    const deleteButton = wrapper.findAll('.plan-card__actions button').find((button) => button.text() === '삭제')
    await deleteButton.trigger('click')
    await flushPromises()

    expect(deleteTravelPlanMock).toHaveBeenCalledWith('101', 2)
  })
})
