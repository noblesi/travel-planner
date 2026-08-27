import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PlaceSearchPanel from '@/components/plan/PlaceSearchPanel.vue'

const { searchPlacesMock } = vi.hoisted(() => ({
  searchPlacesMock: vi.fn(),
}))

vi.mock('@/api/places', () => ({
  searchPlaces: searchPlacesMock,
}))

const place = {
  placeProvider: 'TOUR_API',
  externalPlaceId: '1001',
  placeName: '여의도 한강공원',
  categoryName: '관광지',
  address: '서울 영등포구 여의동로 330',
  latitude: 37.5284,
  longitude: 126.934,
  imageUrl: null,
}

beforeEach(() => {
  searchPlacesMock.mockReset().mockResolvedValue({
    places: [place],
    page: 1,
    size: 10,
    totalCount: 11,
    hasNext: true,
  })
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('PlaceSearchPanel', () => {
  it('keeps successful search results when recent-keyword storage is unavailable', async () => {
    const wrapper = mount(PlaceSearchPanel, {
      props: { regionCode: '1', regionName: 'Seoul' },
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Storage is blocked', 'SecurityError')
    })

    await wrapper.get('[name="placeKeyword"]').setValue('palace')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.findAll('.place-search-panel__results > li')).toHaveLength(1)
    expect(wrapper.emitted('results-change').at(-1)).toEqual([[place]])
  })

  it('현재 여행지역으로 장소를 검색하고 결과 선택을 전달한다', async () => {
    const wrapper = mount(PlaceSearchPanel, {
      props: { regionCode: '1', regionName: '서울특별시' },
    })

    await wrapper.get('[name="placeKeyword"]').setValue(' 한강 ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(searchPlacesMock).toHaveBeenCalledWith({
      keyword: '한강',
      regionCode: '1',
      page: 1,
      size: 10,
    })
    expect(wrapper.text()).toContain('검색 결과 11곳')
    expect(wrapper.text()).toContain('여의도 한강공원')
    expect(wrapper.emitted('results-change').at(-1)).toEqual([[place]])

    await wrapper.get('.place-search-panel__results button').trigger('click')

    expect(wrapper.emitted('select').at(-1)).toEqual([place])
    await wrapper.setProps({ selectedPlaceId: 'TOUR_API:1001' })
    expect(wrapper.get('.place-search-panel__results button').attributes('aria-pressed')).toBe(
      'true',
    )
    expect(wrapper.find('.place-search-panel__selection').exists()).toBe(false)
  })

  it('빈 검색어는 API를 호출하지 않고 Validation 메시지를 표시한다', async () => {
    const wrapper = mount(PlaceSearchPanel)

    await wrapper.get('form').trigger('submit')

    expect(searchPlacesMock).not.toHaveBeenCalled()
    expect(wrapper.get('[role="alert"]').text()).toContain('검색어를 입력해 주세요.')
  })

  it('다음 페이지를 요청하고 외부 API 장애를 사용자 메시지로 변환한다', async () => {
    const wrapper = mount(PlaceSearchPanel, {
      props: { regionCode: '1', regionName: '서울특별시' },
    })
    await wrapper.get('[name="placeKeyword"]').setValue('한강')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    searchPlacesMock.mockRejectedValueOnce({
      response: { data: { code: 'TOUR_API_TIMEOUT' } },
    })
    const nextButton = wrapper
      .findAll('.place-search-panel__pagination button')
      .find((button) => button.text() === '다음')
    await nextButton.trigger('click')
    await flushPromises()

    expect(searchPlacesMock).toHaveBeenNthCalledWith(2, {
      keyword: '한강',
      regionCode: '1',
      page: 2,
      size: 10,
    })
    expect(wrapper.get('[role="alert"]').text()).toContain('연결이 원활하지 않습니다')
  })
})
