import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

async function importLoader() {
  vi.resetModules()
  return import('@/utils/kakaoMapSdk')
}

beforeEach(() => {
  vi.stubEnv('VITE_KAKAO_MAP_KEY', 'test-javascript-key')
  delete window.kakao
  document.getElementById('kakao-map-sdk')?.remove()
})

afterEach(() => {
  vi.unstubAllEnvs()
  delete window.kakao
  document.getElementById('kakao-map-sdk')?.remove()
})

describe('loadKakaoMapSdk', () => {
  it('SDK 로드 실패 후 기존 script를 제거하고 재시도한다', async () => {
    const { loadKakaoMapSdk } = await importLoader()
    const firstLoad = loadKakaoMapSdk()
    const firstScript = document.getElementById('kakao-map-sdk')

    firstScript.dispatchEvent(new Event('error'))
    await expect(firstLoad).rejects.toThrow('불러오지 못했습니다')
    expect(document.getElementById('kakao-map-sdk')).toBeNull()

    const retryLoad = loadKakaoMapSdk()
    const retryScript = document.getElementById('kakao-map-sdk')

    expect(retryScript).not.toBe(firstScript)
    expect(retryScript.src).toContain('appkey=test-javascript-key')

    window.kakao = { maps: { load: vi.fn((callback) => callback()) } }
    retryScript.dispatchEvent(new Event('load'))

    await expect(retryLoad).resolves.toBeUndefined()
    expect(window.kakao.maps.load).toHaveBeenCalledOnce()
  })

  it('SDK 초기화 실패 메시지에 현재 origin을 포함한다', async () => {
    const { loadKakaoMapSdk } = await importLoader()
    const load = loadKakaoMapSdk()
    const script = document.getElementById('kakao-map-sdk')

    script.dispatchEvent(new Event('load'))

    await expect(load).rejects.toThrow(window.location.origin)
    expect(document.getElementById('kakao-map-sdk')).toBeNull()
  })
})
