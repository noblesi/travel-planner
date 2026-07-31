import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PlanSetupForm from '@/components/plan/PlanSetupForm.vue'
import PlanSetupView from '@/views/PlanSetupView.vue'

const { createTravelPlanMock, getRegionsMock, routerPushMock } = vi.hoisted(() => ({
  createTravelPlanMock: vi.fn(),
  getRegionsMock: vi.fn(),
  routerPushMock: vi.fn(),
}))

vi.mock('@/api/plans', () => ({
  createTravelPlan: createTravelPlanMock,
}))

vi.mock('@/api/regions', () => ({
  getRegions: getRegionsMock,
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPushMock }),
}))

const regions = [{ regionCode: '1', regionName: '서울특별시' }]

function mountView() {
  return mount(PlanSetupView, {
    global: {
      stubs: {
        DefaultLayout: { template: '<div><slot /></div>' },
      },
    },
  })
}

beforeEach(() => {
  createTravelPlanMock.mockReset().mockResolvedValue({ planId: '101' })
  getRegionsMock.mockReset().mockResolvedValue(regions)
  routerPushMock.mockReset().mockResolvedValue(undefined)
})

describe('PlanSetupView', () => {
  it('지역을 조회하고 플랜 생성 후 제작 화면으로 이동한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('#regionCode').setValue('1')
    await wrapper.get('#startDate').setValue('2026-08-10')
    await wrapper.get('#endDate').setValue('2026-08-12')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(getRegionsMock).toHaveBeenCalledOnce()
    expect(createTravelPlanMock).toHaveBeenCalledWith({
      regionCode: '1',
      startDate: '2026-08-10',
      endDate: '2026-08-12',
      visibility: 'PUBLIC',
    })
    expect(routerPushMock).toHaveBeenCalledWith({
      name: 'plan-editor',
      params: { planId: '101' },
    })
  })

  it('14일을 초과한 여행 기간은 API를 호출하지 않는다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })

    await wrapper.get('#regionCode').setValue('1')
    await wrapper.get('#startDate').setValue('2026-08-01')
    await wrapper.get('#endDate').setValue('2026-08-15')
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('여행 기간은 최대 14일까지 설정할 수 있습니다.')
  })

  it('지역 조회 실패 시 오류와 재시도 동작을 제공한다', async () => {
    getRegionsMock.mockRejectedValueOnce({
      response: { data: { message: '지역 서비스에 연결할 수 없습니다.' } },
    })

    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('지역 서비스에 연결할 수 없습니다.')

    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(getRegionsMock).toHaveBeenCalledTimes(2)
    expect(wrapper.get('#regionCode').exists()).toBe(true)
  })
})
