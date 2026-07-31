let sdkPromise = null

function waitForKakaoMaps(resolve, reject) {
  if (!window.kakao?.maps) {
    reject(new Error('카카오맵 SDK를 초기화하지 못했습니다.'))
    return
  }

  try {
    window.kakao.maps.load(resolve)
  } catch {
    reject(new Error('카카오맵 SDK를 초기화하지 못했습니다.'))
  }
}

function rejectScriptLoad(script, reject) {
  script.remove()
  reject(new Error('카카오맵 SDK를 불러오지 못했습니다.'))
}

export function loadKakaoMapSdk() {
  if (window.kakao?.maps) {
    return new Promise((resolve) => window.kakao.maps.load(resolve))
  }

  if (sdkPromise) return sdkPromise

  const appKey = import.meta.env.VITE_KAKAO_MAP_KEY?.trim()
  if (!appKey) {
    return Promise.reject(new Error('카카오맵 API Key가 설정되지 않았습니다.'))
  }

  sdkPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById('kakao-map-sdk')
    if (existingScript) {
      existingScript.addEventListener('load', () => waitForKakaoMaps(resolve, reject), {
        once: true,
      })
      existingScript.addEventListener('error', () => rejectScriptLoad(existingScript, reject), {
        once: true,
      })
      return
    }

    const query = new URLSearchParams({
      appkey: appKey,
      libraries: 'services',
      autoload: 'false',
    })
    const script = document.createElement('script')
    script.id = 'kakao-map-sdk'
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?${query.toString()}`
    script.addEventListener('load', () => waitForKakaoMaps(resolve, reject), { once: true })
    script.addEventListener('error', () => rejectScriptLoad(script, reject), { once: true })
    document.head.appendChild(script)
  }).catch((error) => {
    sdkPromise = null
    throw error
  })

  return sdkPromise
}
