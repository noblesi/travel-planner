<script setup>
import { RouterLink } from 'vue-router'

import DefaultLayout from '@/layouts/DefaultLayout.vue'
</script>

<template>
  <DefaultLayout>
    <section class="not-found">
      <p>404</p>
      <h1>페이지를 찾을 수 없습니다.</h1>
      <RouterLink to="/">홈으로 돌아가기</RouterLink>
    </section>
  </DefaultLayout>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 520px;
  text-align: center;
  place-content: center;
}

.not-found p {
  margin: 0;
  color: var(--color-brand);
  font-size: 18px;
  font-weight: 800;
}

.not-found h1 {
  margin: 12px 0 26px;
}

.not-found a {
  color: var(--color-brand);
  font-weight: 750;
}
</style>
