<script setup>
import { computed, ref, watch } from 'vue'

import DefaultProfileImage from '@/assets/myPageImage/default_profile.webp'
import { updateProfileImage } from '@/api/member'
import { useToastStore } from '@/stores/toast'
import { displayProfileValue } from '@/utils/memberProfile'

const props = defineProps({
  profile: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['updated'])
const toastStore = useToastStore()

const imageLoadFailed = ref(false)
const fileInput = ref(null)
const isUploading = ref(false)
const uploadError = ref('')
const profileImageUrl = computed(() => {
  if (props.profile.profileImageUrl && !imageLoadFailed.value) {
    return props.profile.profileImageUrl
  }
  return DefaultProfileImage
})

watch(
  () => props.profile.profileImageUrl,
  () => {
    imageLoadFailed.value = false
  },
)

function openFilePicker() {
  if (!isUploading.value) fileInput.value?.click()
}

async function handleFileChange(event) {
  const input = event.target
  const file = input.files?.[0]
  if (!file || isUploading.value) return

  uploadError.value = ''
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    uploadError.value = 'JPEG, PNG 또는 WebP 이미지만 선택해 주세요.'
    input.value = ''
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    uploadError.value = '프로필 이미지는 5MB 이하만 업로드할 수 있습니다.'
    input.value = ''
    return
  }

  isUploading.value = true
  try {
    const updatedProfile = await updateProfileImage(file)
    imageLoadFailed.value = false
    emit('updated', updatedProfile)
    toastStore.success('프로필 이미지를 변경했습니다.')
  } catch (error) {
    uploadError.value =
      error?.response?.data?.message || '프로필 이미지를 변경하지 못했습니다. 다시 시도해 주세요.'
  } finally {
    isUploading.value = false
    input.value = ''
  }
}
</script>

<template>
  <aside class="profile-summary">
    <img
      class="profile-image"
      :src="profileImageUrl"
      alt=""
      width="148"
      height="148"
      @error="imageLoadFailed = true"
    />
    <strong>{{ displayProfileValue(profile.nickname) }}</strong>
    <span>{{ profile.email }}</span>
    <input
      ref="fileInput"
      class="profile-file-input"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      @change="handleFileChange"
    />
    <button type="button" :disabled="isUploading" @click="openFilePicker">
      {{ isUploading ? '업로드 중...' : '프로필 이미지 변경' }}
    </button>
    <small>JPEG, PNG, WebP · 최대 5MB</small>
    <p v-if="uploadError" class="profile-upload-error" role="alert">{{ uploadError }}</p>
  </aside>
</template>

<style scoped>
.profile-summary {
  display: flex;
  min-height: 410px;
  flex-direction: column;
  align-items: center;
  padding: 42px 24px 28px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: 0 16px 45px rgb(15 23 42 / 6%);
  text-align: center;
}

.profile-image {
  width: 148px;
  height: 148px;
  border: 6px solid var(--color-brand-soft);
  border-radius: 50%;
  object-fit: cover;
}

.profile-summary strong {
  margin-top: 22px;
  color: var(--color-text);
  font-size: 21px;
}

.profile-summary > span {
  margin-top: 6px;
  overflow-wrap: anywhere;
  color: var(--color-text-muted);
  font-size: 13px;
}

.profile-file-input {
  display: none;
}

.profile-summary button {
  min-height: 38px;
  margin-top: 20px;
  padding: 0 14px;
  border: 1px solid var(--color-brand-border);
  border-radius: 10px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.profile-summary button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.profile-summary small {
  width: 100%;
  margin-top: 20px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
  color: var(--color-text-muted);
  line-height: 1.6;
}

.profile-upload-error {
  margin: 12px 0 0;
  color: var(--color-danger);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 760px) {
  .profile-summary {
    min-height: 360px;
  }
}
</style>
