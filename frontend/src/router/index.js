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
      component: () => import('@/views/loginView/LoginView.vue'),
    },
    {
      path: '/joinView',
      name: 'join',
      component: () => import('@/views/loginView/JoinView.vue'),
    },
    {
      path: '/joinProfileView',
      name: 'joinProfile',
      component: () => import('@/views/loginView/JoinProfileView.vue'),
    },
    {
      path: '/passwordFind',
      name: 'find',
      component: () => import('@/views/loginView/PasswordFindView.vue'),
    },
    {
      path: '/joinComplete',
      name: 'complete',
      component: () => import('@/views/loginView/JoinCompleteView.vue'),
    },
  ],
})

export default router
