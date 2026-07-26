<script setup>
import { useRoute } from 'vue-router'

import DefaultLayout from '@/layouts/DefaultLayout.vue'

const route = useRoute()
</script>

<template>
  <DefaultLayout>
    <section class="editor-entry">
      <p>PLAN {{ route.params.planId }}</p>
      <h1>여행 계획이 만들어졌습니다.</h1>
      <span>이제 일차별 장소와 이동 순서를 계획할 수 있어요.</span>
    </section>
  </DefaultLayout>
</template>

<style scoped>
.editor-entry {
  display: grid;
  min-height: 560px;
  padding: 80px 20px;
  place-content: center;
  text-align: center;
}

.editor-entry p {
  margin: 0 0 12px;
  color: #ff5a4e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.editor-entry h1 {
  margin: 0;
  color: #111827;
  font-size: clamp(30px, 5vw, 48px);
}

.editor-entry span {
  margin-top: 16px;
  color: #64748b;
}
</style>
