import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
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
      plugins: [createPinia()],
      stubs: {
        DefaultLayout: { template: '<div><slot /></div>' },
      },
    },
  })
}

async function selectRegion(wrapper, regionName = '서울특별시') {
  await wrapper.get('#regionCode').trigger('click')
  await wrapper.get('[role="option"]').trigger('click')
  expect(wrapper.get('#regionCode').text()).toContain(regionName)
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

    await selectRegion(wrapper)
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

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(1))
    await wrapper.get('#endDate').setValue(dateFromToday(15))
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('여행 기간은 최대 14일까지 설정할 수 있습니다.')
  })

  it('필수 입력을 안내하고 시작 날짜를 선택하기 전에는 종료 날짜를 비활성화한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })

    expect(wrapper.get('#endDate').attributes('disabled')).toBeDefined()

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('여행지역을 선택해 주세요.')
    expect(wrapper.text()).toContain('시작 날짜를 선택해 주세요.')
    expect(wrapper.text()).toContain('종료 날짜를 선택해 주세요.')
    expect(wrapper.get('#regionCode').attributes('aria-invalid')).toBe('true')
  })

  it('지역 목록을 커스텀 드롭다운으로 열고 키보드로 선택한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: {
        regions: [
          ...regions,
          { regionCode: '6', regionName: '부산광역시' },
        ],
      },
    })
    const trigger = wrapper.get('#regionCode')

    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)

    await trigger.trigger('click')

    expect(trigger.attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('[role="option"]')).toHaveLength(2)

    await trigger.trigger('keydown', { key: 'ArrowDown' })
    await trigger.trigger('keydown', { key: 'Enter' })

    expect(trigger.text()).toContain('부산광역시')
    expect(trigger.attributes('aria-expanded')).toBe('false')
  })

  it('선택한 여행 일수를 안내하고 비공개 설정을 생성 요청에 반영한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })
    const startDate = dateFromToday(4)
    const endDate = dateFromToday(6)

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(startDate)
    await wrapper.get('#endDate').setValue(endDate)
    await wrapper.get('#visibility').setValue(false)

    expect(wrapper.text()).toContain('3일 여행으로 계획을 시작합니다.')
    expect(wrapper.text()).toContain('비공개 여행')
    expect(wrapper.text()).toContain('초대한 동행자만 이 여행을 볼 수 있어요.')

    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toEqual([
      [
        {
          regionCode: '1',
          startDate,
          endDate,
          visibility: 'PRIVATE',
        },
      ],
    ])
  })

  it('오늘 이전 날짜를 거부하고 종료일 선택 범위를 14일 이내로 제한한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })
    const startDate = dateFromToday(3)

    expect(wrapper.get('#startDate').attributes('min')).toBe(dateFromToday(0))

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(-1))
    await wrapper.get('#endDate').setValue(dateFromToday(1))
    await wrapper.get('form').trigger('submit')

    expect(wrapper.emitted('submit')).toBeUndefined()
    expect(wrapper.text()).toContain('오늘 이후의 시작 날짜를 선택해 주세요.')

    await wrapper.get('#startDate').setValue(startDate)

    expect(wrapper.get('#endDate').attributes('min')).toBe(startDate)
    expect(wrapper.get('#endDate').attributes('max')).toBe(dateFromToday(16))
  })

  it('시작일이 기존 종료일보다 늦어지면 종료일을 초기화하고 이유를 안내한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })

    await wrapper.get('#startDate').setValue(dateFromToday(2))
    await wrapper.get('#endDate').setValue(dateFromToday(5))
    await wrapper.get('#startDate').setValue(dateFromToday(6))

    expect(wrapper.get('#endDate').element.value).toBe('')
    expect(wrapper.get('[role="status"]').text()).toContain(
      '시작 날짜가 기존 종료 날짜보다 늦어 종료 날짜를 초기화했습니다.',
    )
  })

  it('시작일 변경으로 14일을 초과하면 종료일을 초기화하고 이유를 안내한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: { regions },
    })

    await wrapper.get('#startDate').setValue(dateFromToday(2))
    await wrapper.get('#endDate').setValue(dateFromToday(15))
    await wrapper.get('#startDate').setValue(dateFromToday(1))

    expect(wrapper.get('#endDate').element.value).toBe('')
    expect(wrapper.get('[role="status"]').text()).toContain(
      '여행 기간이 14일을 초과해 종료 날짜를 초기화했습니다.',
    )
  })

  it('로컬 검증 오류를 서버 필드 오류보다 우선 표시한다', async () => {
    const wrapper = mount(PlanSetupForm, {
      props: {
        regions,
        serverFieldErrors: { endDate: '서버에서 전달한 종료 날짜 오류' },
      },
    })

    await wrapper.get('#startDate').setValue(dateFromToday(2))
    await wrapper.get('form').trigger('submit')

    expect(wrapper.get('#endDate-error').text()).toBe('종료 날짜를 선택해 주세요.')
    expect(wrapper.text()).not.toContain('서버에서 전달한 종료 날짜 오류')
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

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(7))
    await wrapper.get('#endDate').setValue(dateFromToday(9))
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('#regionCode').attributes('aria-invalid')).toBe('true')
    expect(wrapper.text()).toContain('지원하지 않는 여행지역입니다.')

    await selectRegion(wrapper)

    expect(wrapper.text()).not.toContain('지원하지 않는 여행지역입니다.')
  })

  it('Backend 비즈니스 오류를 관련 입력 항목에 표시한다', async () => {
    createTravelPlanMock.mockRejectedValueOnce({
      response: {
        status: 400,
        data: {
          code: 'TRAVEL_PLAN_DURATION_EXCEEDED',
          message: '여행 기간은 최대 14일까지 설정할 수 있습니다.',
          errors: [],
        },
      },
    })
    const wrapper = mountView()
    await flushPromises()

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(7))
    await wrapper.get('#endDate').setValue(dateFromToday(9))
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('#endDate').attributes('aria-invalid')).toBe('true')
    expect(wrapper.text()).toContain('여행 기간은 최대 14일까지 설정할 수 있습니다.')
  })

  it('생성 응답에 플랜 ID가 없으면 이동하지 않고 오류를 안내한다', async () => {
    createTravelPlanMock.mockResolvedValueOnce({})
    const wrapper = mountView()
    await flushPromises()

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(7))
    await wrapper.get('#endDate').setValue(dateFromToday(9))
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(routerPushMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('여행 계획을 만들지 못했습니다. 잠시 후 다시 시도해 주세요.')
  })

  it('플랜 생성 후 화면 이동만 실패하면 중복 생성 없이 다시 이동한다', async () => {
    routerPushMock.mockResolvedValueOnce({ type: 4 }).mockResolvedValueOnce(undefined)
    const wrapper = mountView()
    await flushPromises()

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(7))
    await wrapper.get('#endDate').setValue(dateFromToday(9))
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createTravelPlanMock).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('여행 계획은 만들어졌지만 제작 화면으로 이동하지 못했습니다.')
    expect(wrapper.find('form').exists()).toBe(false)

    await wrapper.get('.created-plan-recovery button').trigger('click')
    await flushPromises()

    expect(routerPushMock).toHaveBeenCalledTimes(2)
    expect(createTravelPlanMock).toHaveBeenCalledOnce()
  })

  it('생성 요청이 진행 중일 때 중복 제출을 차단한다', async () => {
    let resolveCreatePlan
    createTravelPlanMock.mockReturnValueOnce(
      new Promise((resolve) => {
        resolveCreatePlan = resolve
      }),
    )
    const wrapper = mountView()
    await flushPromises()

    await selectRegion(wrapper)
    await wrapper.get('#startDate').setValue(dateFromToday(7))
    await wrapper.get('#endDate').setValue(dateFromToday(9))
    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    expect(createTravelPlanMock).toHaveBeenCalledOnce()
    expect(wrapper.get('fieldset').attributes('disabled')).toBeDefined()

    resolveCreatePlan({ planId: '101' })
    await flushPromises()
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
