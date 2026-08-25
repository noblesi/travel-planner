import { onMounted, ref } from 'vue'

import { getMyProfile } from '@/api/member'

const PROFILE_LOAD_ERROR = '회원 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'

export function useMemberProfile() {
  const profile = ref(null)
  const status = ref('loading')
  const errorMessage = ref('')

  async function loadProfile() {
    status.value = 'loading'
    errorMessage.value = ''

    try {
      const loadedProfile = await getMyProfile()
      if (!loadedProfile || typeof loadedProfile !== 'object') {
        throw new Error('Profile response is invalid')
      }
      profile.value = loadedProfile
      status.value = 'success'
    } catch (error) {
      profile.value = null
      status.value = 'error'
      errorMessage.value = error?.response?.data?.message || PROFILE_LOAD_ERROR
    }
  }

  function updateLoadedProfile(updatedProfile) {
    profile.value = updatedProfile
  }

  onMounted(loadProfile)

  return {
    profile,
    status,
    errorMessage,
    loadProfile,
    updateLoadedProfile,
  }
}
