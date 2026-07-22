import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/loginView/LoginView.vue' 
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
    {
      path: '/loginView',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/joinView',
      name: 'join',
      component: () => import('@/views/loginView/JoinView.vue'),
    },
  ],
})

export default router
