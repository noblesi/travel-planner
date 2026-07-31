import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/plans/new',
      name: 'plan-setup',
      component: () => import('@/views/PlanSetupView.vue'),
    },
    {
      path: '/plans/:planId/edit',
      name: 'plan-editor',
      component: () => import('@/views/PlanEditorView.vue'),
      props: true,
    },
    {
      path: '/plans',
      name: 'plan-search',
      component: () => import('@/views/PlanSearch/PlanSearchView.vue'),
    },
    {
      path: '/plans/:id',
      name: 'plan-detail',
      component: () => import('@/views/PlanSearch/PlanDetailView.vue'),
      props: true,
    },
    {
      path: '/notice',
      name: 'notice-list',
      component: () => import('@/views/Notice/NoticeListView.vue'),
    },
    {
      path: '/notice/:id',
      name: 'notice-detail',
      component: () => import('@/views/Notice/NoticeDetailView.vue'),
      props: true,
    },
    {
      path: '/plans/:id/invite',
      name: 'invite',
      component: () => import('@/views/Invite/InviteView.vue'),
      props: true,
    },
    {
      path: '/invite/accept',
      name: 'invite-accept',
      component: () => import('@/views/Invite/InviteAcceptView.vue'),
    },
    {
      path: '/loginView',
      name: 'login',
      component: () => import('@/views/loginView/LoginView.vue'),
    },
    {
      path: '/emailFind',
      name: 'emailFind',
      component: () => import('@/views/loginView/EmailFindView.vue'),
    },
    {
      path: '/passwordFind',
      name: 'passwordFind',
      component: () => import('@/views/loginView/PasswordFindView.vue'),
    },
    //회원 가입
    {
      path: '/joinView',
      name: 'join',
      component: () => import('@/views/joinView/JoinView.vue'),
    },
    {
      path: '/joinProfileView',
      name: 'joinProfile',
      component: () => import('@/views/joinView/JoinProfileView.vue'),
    },
    {
      path: '/joinComplete',
      name: 'complete',
      component: () => import('@/views/joinView/JoinCompleteView.vue'),
    },
    //마이 페이지
    {
      path: '/myPage',
      name: 'myPage',
      component: () => import('@/views/myPage/MyPage.vue'),
    },
     {
      path: '/testView',
      name: 'testView',
      component: () => import('@/views/myPage/testView.vue'),
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.path === from.path) {
      return false  // 스크롤 유지,  아무 것도 안 함
    }
    return { top: 0 }
  },
})

export default router
