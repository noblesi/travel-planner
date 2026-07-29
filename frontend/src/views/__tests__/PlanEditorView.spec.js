import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PlanEditorView from '@/views/PlanEditorView.vue'

const { getTravelPlanEditorMock, updateTravelPlanDatesMock } = vi.hoisted(() => ({
  getTravelPlanEditorMock: vi.fn(),
  updateTravelPlanDatesMock: vi.fn(),
}))

vi.mock('@/api/plans', () => ({
  getTravelPlanEditor: getTravelPlanEditorMock,
  updateTravelPlanDates: updateTravelPlanDatesMock,
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
  updateTravelPlanDatesMock.mockReset()
})

describe('PlanEditorView', () => {
  it('라우트로 전달된 플랜 ID를 조회하고 제작 화면 정보를 표시한다', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(getTravelPlanEditorMock).toHaveBeenCalledWith('101')
    expect(wrapper.text()).toContain('서울특별시 여행')
    expect(wrapper.text()).toContain('여행 일정')
    expect(wrapper.text()).toContain('2일')
    expect(wrapper.findAll('.day-tab')).toHaveLength(2)
    expect(wrapper.text()).toContain('DAY 1에 등록된 장소가 없습니다.')
    expect(wrapper.text()).toContain('서울특별시 지도')
  })

  it('선택한 DAY의 오전·오후 일정 카드와 해당 DAY의 빈 상태를 표시한다', async () => {
    getTravelPlanEditorMock.mockResolvedValueOnce({
      ...editor,
      days: [
        {
          ...editor.days[0],
          items: [
            {
              scheduleItemId: '301',
              timeSlot: 'MORNING',
              positionNo: 1,
              placeName: '경복궁',
              categoryName: '관광지',
              address: '서울 종로구 사직로 161',
              description: '조선 시대의 법궁',
            },
            {
              scheduleItemId: '302',
              timeSlot: 'AFTERNOON',
              positionNo: 1,
              placeName: '북촌한옥마을',
              categoryName: '문화마을',
              address: '서울 종로구 계동길 37',
              description: '한옥 골목 산책',
            },
          ],
        },
        editor.days[1],
      ],
    })
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.text()).toContain('오전')
    expect(wrapper.text()).toContain('경복궁')
    expect(wrapper.text()).toContain('오후')
    expect(wrapper.text()).toContain('북촌한옥마을')
    expect(wrapper.findAll('.schedule-card')).toHaveLength(2)

    await wrapper.findAll('.day-tab')[1].trigger('click')

    expect(wrapper.findAll('.day-tab')[1].attributes('aria-pressed')).toBe('true')
    expect(wrapper.text()).toContain('DAY 2에 등록된 장소가 없습니다.')
    expect(wrapper.text()).not.toContain('경복궁')
    expect(wrapper.findAll('.schedule-card')).toHaveLength(0)
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

  it('제작 페이지에서 여행 날짜를 변경하고 응답 DAY를 반영한다', async () => {
    const updatedEditor = {
      plan: {
        ...editor.plan,
        startDate: '2026-08-09',
        endDate: '2026-08-11',
        versionNo: 1,
      },
      days: [
        {
          planDayId: '200',
          dayNo: 1,
          travelDate: '2026-08-09',
          scheduleVersion: 0,
          items: [],
        },
        { ...editor.days[0], dayNo: 2 },
        { ...editor.days[1], dayNo: 3 },
      ],
    }
    updateTravelPlanDatesMock.mockResolvedValueOnce(updatedEditor)
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-09')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-11')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()

    expect(updateTravelPlanDatesMock).toHaveBeenCalledWith('101', {
      startDate: '2026-08-09',
      endDate: '2026-08-11',
      versionNo: 0,
      force: false,
    })
    expect(wrapper.text()).toContain('3일')
    expect(wrapper.findAll('.day-tab')).toHaveLength(3)
    expect(wrapper.find('.date-editor__form').exists()).toBe(false)
  })

  it('일정이 있는 DAY가 제외되면 확인 후 강제로 날짜를 변경한다', async () => {
    updateTravelPlanDatesMock
      .mockRejectedValueOnce({
        response: {
          status: 409,
          data: {
            code: 'PLAN_DAYS_WITH_SCHEDULES_WOULD_BE_REMOVED',
            message: '변경 범위에서 제외되는 날짜에 일정이 있습니다.',
          },
        },
      })
      .mockResolvedValueOnce({
        plan: { ...editor.plan, startDate: '2026-08-11', versionNo: 1 },
        days: [{ ...editor.days[1], dayNo: 1 }],
      })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-11')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-11')
    await wrapper.get('.date-editor__form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alertdialog"]').exists()).toBe(true)

    const confirmButton = wrapper
      .findAll('.confirmation-dialog__actions button')
      .find((button) => button.text().includes('일정 삭제 후 변경'))
    await confirmButton.trigger('click')
    await flushPromises()

    expect(updateTravelPlanDatesMock).toHaveBeenNthCalledWith(2, '101', {
      startDate: '2026-08-11',
      endDate: '2026-08-11',
      versionNo: 0,
      force: true,
    })
    expect(wrapper.find('[role="alertdialog"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('1일')
  })

  it('14일을 초과한 날짜 변경은 API를 호출하지 않는다', async () => {
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.date-editor__open').trigger('click')
    await wrapper.get('[name="editStartDate"]').setValue('2026-08-01')
    await wrapper.get('[name="editEndDate"]').setValue('2026-08-15')
    await wrapper.get('.date-editor__form').trigger('submit')

    expect(updateTravelPlanDatesMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('여행 기간은 최대 14일까지 설정할 수 있습니다.')
  })
})
