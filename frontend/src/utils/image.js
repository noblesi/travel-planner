import defaultProfileImg from '@/assets/myPageImage/default_profile.webp' // 기본 이미지 경로

// 💡 이미지 전용 Base URL 지정
//const IMAGE_BASE_URL = 'http://211.63.89.134:8080'
const IMAGE_BASE_URL = 'http://localhost:8080'

export function getImageUrl(path) {
  // 1. 경로가 없는 경우 기본 이미지 반환
  if (!path) return defaultProfileImg

  // 2. 이미 전체 URL(http://...)로 저장되어 있는 경우 그대로 반환
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }

  // 3. 상대 경로 앞의 '/' 제거 후 Base URL과 조합 (중복 슬래시 방지)
  const cleanPath = path.startsWith('/') ? path.slice(1) : path
  return `${IMAGE_BASE_URL}/${cleanPath}`
}