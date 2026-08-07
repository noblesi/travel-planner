<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

import ToastRegion from '@/components/ui/ToastRegion.vue'

const route = useRoute()
const toastPlacement = computed(() => (route.name === 'plan-editor' ? 'editor' : 'default'))
</script>

<template>
  <RouterView />
  <ToastRegion :placement="toastPlacement" />
</template>
