<script setup>
import { computed, ref, watch } from 'vue'

import DefaultProfileImage from '@/assets/myPageImage/default_profile.webp'
import { displayProfileValue } from '@/utils/memberProfile'

const props = defineProps({
  profile: {
    type: Object,
    required: true,
  },
})

const imageLoadFailed = ref(false)
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
    <small>이메일과 프로필 이미지는 현재 읽기 전용입니다.</small>
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

.profile-summary small {
  width: 100%;
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
  color: var(--color-text-muted);
  line-height: 1.6;
}

@media (max-width: 760px) {
  .profile-summary {
    min-height: 360px;
  }
}
</style>
