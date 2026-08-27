import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import KakaoMap from '@/components/map/KakaoMap.vue'

const { loadKakaoMapSdkMock } = vi.hoisted(() => ({
  loadKakaoMapSdkMock: vi.fn(),
}))

vi.mock('@/utils/kakaoMapSdk', () => ({
  loadKakaoMapSdk: loadKakaoMapSdkMock,
}))

function installKakaoMock() {
  const listeners = []
  const map = {
    setCenter: vi.fn(),
    setBounds: vi.fn(),
    panTo: vi.fn(),
  }
  const infoWindow = {
    close: vi.fn(),
    setContent: vi.fn(),
    open: vi.fn(),
  }
  const createdMarkers = []

  window.kakao = {
    maps: {
      LatLng: vi.fn(function LatLng(latitude, longitude) {
        return { latitude, longitude }
      }),
      Map: vi.fn(function Map() {
        return map
      }),
      Marker: vi.fn(function Marker(options) {
        const marker = { options, setMap: vi.fn(), setZIndex: vi.fn() }
        createdMarkers.push(marker)
        return marker
      }),
      InfoWindow: vi.fn(function InfoWindow() {
        return infoWindow
      }),
      LatLngBounds: vi.fn(function LatLngBounds() {
        return { extend: vi.fn() }
      }),
      event: {
        addListener: vi.fn((marker, eventName, handler) => {
          listeners.push({ marker, eventName, handler })
        }),
      },
    },
  }

  return { listeners, map, infoWindow, createdMarkers }
}

beforeEach(() => {
  loadKakaoMapSdkMock.mockReset().mockResolvedValue()
  delete window.kakao
})

describe('KakaoMap', () => {
  it('좌표가 있는 장소만 Marker로 만들고 Marker 선택을 전달한다', async () => {
    const kakaoMock = installKakaoMock()
    const place = {
      mapPlaceId: 'search:TOUR_API:1001',
      placeName: '여의도 한강공원',
      latitude: 37.5284,
      longitude: 126.934,
    }
    const wrapper = mount(KakaoMap, {
      props: {
        places: [place, { mapPlaceId: 'invalid', placeName: '좌표 없음' }],
      },
    })
    await flushPromises()

    expect(kakaoMock.createdMarkers).toHaveLength(1)
    expect(kakaoMock.map.setCenter).toHaveBeenCalled()

    kakaoMock.listeners[0].handler()

    expect(wrapper.emitted('select')).toEqual([[place]])
    expect(kakaoMock.infoWindow.open).toHaveBeenCalled()
  })

  it('선택된 장소의 정보창을 열고 지도 중심을 이동한다', async () => {
    const kakaoMock = installKakaoMock()
    const place = {
      mapPlaceId: 'schedule:301',
      placeName: '경복궁',
      latitude: 37.5796,
      longitude: 126.977,
    }
    mount(KakaoMap, {
      props: { places: [place], selectedPlaceId: 'schedule:301' },
    })
    await flushPromises()

    expect(kakaoMock.infoWindow.setContent).toHaveBeenCalled()
    expect(kakaoMock.infoWindow.open).toHaveBeenCalled()
    expect(kakaoMock.map.panTo).toHaveBeenCalled()
  })

  it('선택한 검색 장소를 마커 위 상세카드로 표시하고 추가와 선택 해제를 전달한다', async () => {
    const kakaoMock = installKakaoMock()
    const place = {
      mapPlaceId: 'search:TOUR_API:1001',
      markerSource: 'SEARCH',
      placeProvider: 'TOUR_API',
      externalPlaceId: '1001',
      placeName: '여의도 한강공원',
      categoryName: '관광지',
      address: '서울 영등포구 여의동로 330',
      latitude: 37.5284,
      longitude: 126.934,
    }
    const wrapper = mount(KakaoMap, {
      props: {
        places: [place],
        selectedPlaceId: place.mapPlaceId,
        selectedPlaceDetail: place,
      },
    })
    await flushPromises()

    const detailContent = kakaoMock.infoWindow.setContent.mock.calls.at(-1)[0]
    expect(detailContent.className).toBe('kakao-map__detail-window')
    expect(detailContent.textContent).toContain('여의도 한강공원')

    detailContent
      .querySelector('.place-detail-card__actions button')
      .dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()
    expect(wrapper.emitted('add')).toEqual([[{ place, timeSlot: 'MORNING' }]])

    detailContent
      .querySelector('.place-detail-card__close')
      .dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()
    expect(wrapper.emitted('deselect')).toEqual([[place]])
    expect(kakaoMock.infoWindow.close).toHaveBeenCalled()
  })

  it('SDK 초기화 실패 시 오류와 재시도 버튼을 표시한다', async () => {
    loadKakaoMapSdkMock.mockRejectedValueOnce(new Error('API Key가 없습니다.'))
    const wrapper = mount(KakaoMap)
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('API Key가 없습니다.')

    installKakaoMock()
    loadKakaoMapSdkMock.mockResolvedValueOnce()
    await wrapper.get('[role="alert"] button').trigger('click')
    await flushPromises()

    expect(loadKakaoMapSdkMock).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })
})
