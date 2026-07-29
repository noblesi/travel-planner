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

function dateFromToday(dayOffset) {
  const date = new Date()
  date.setDate(date.getDate() + dayOffset)
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 10)
}

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

    const startDate = dateFromToday(7)
    const endDate = dateFromToday(9)

    await wrapper.get('#regionCode').setValue('1')
    await wrapper.get('#startDate').setValue(startDate)
    await wrapper.get('#endDate').setValue(endDate)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(getRegionsMock).toHaveBeenCalledOnce()
    expect(createTravelPlanMock).toHaveBeenCalledWith({
      regionCode: '1',
      startDate,
      endDate,
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
    await wrapper.get('#startDate').setValue(dateFromToday(1))
    await wrapper.get('#endDate').setValue(dateFromToday(15))
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('여행 기간은 최대 14일까지 설정할 수 있습니다.')
  })

  it('오늘 이전 날짜를 거부하고 종료일 선택 범위를 14일 이내로 제한한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })
    const startDate = dateFromToday(3)

    expect(wrapper.get('#startDate').attributes('min')).toBe(dateFromToday(0))

    await wrapper.get('#regionCode').setValue('1')
    await wrapper.get('#startDate').setValue(dateFromToday(-1))
    await wrapper.get('#endDate').setValue(dateFromToday(1))
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('오늘 이후의 시작 날짜를 선택해 주세요.')

    await wrapper.get('#startDate').setValue(startDate)

    expect(wrapper.get('#endDate').attributes('min')).toBe(startDate)
    expect(wrapper.get('#endDate').attributes('max')).toBe(dateFromToday(16))
  })

  it('플랜 생성 API의 필드 오류를 입력 항목에 표시하고 수정 시 제거한다', async () => {
    createTravelPlanMock.mockRejectedValueOnce({
      response: {
        status: 400,
        data: {
          code: 'VALIDATION_ERROR',
          message: '입력값 검증에 실패했습니다.',
          errors: [{ field: 'regionCode', message: '지원하지 않는 여행지역입니다.' }],
        },
      },
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('#regionCode').setValue('1')
    await wrapper.get('#startDate').setValue(dateFromToday(7))
    await wrapper.get('#endDate').setValue(dateFromToday(9))
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('#regionCode').attributes('aria-invalid')).toBe('true')
    expect(wrapper.text()).toContain('지원하지 않는 여행지역입니다.')

    await wrapper.get('#regionCode').setValue('')

    expect(wrapper.text()).not.toContain('지원하지 않는 여행지역입니다.')
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
