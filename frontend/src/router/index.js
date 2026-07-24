import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import adminRoutes from './adminRouter'

// 사용자 라우트와 기능별로 분리한 관리자 라우트를 하나의 Router에 등록합니다.
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    ...adminRoutes, // adminRouter.js의 라우트 배열을 이 위치에 펼쳐서 추가합니다.
    // 등록되지 않은 모든 URL은 마지막에 404 화면으로 보냅니다.
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

export default router
