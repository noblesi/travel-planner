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
      path: '/announcements',
      name: 'announcements-list',
      component: () => import('@/views/Announcement/AnnouncementListView.vue'),
    },
    {
      path: '/announcements/:id',
      name: 'announcements-detail',
      component: () => import('@/views/Announcement/AnnouncementDetailView.vue'),
      props: true,
    },
    {
      path: '/invite',
      name: 'invite',
      component: () => import('@/views/Invite/InviteView.vue'),
    },
    {
      path: '/invite/accept',
      name: 'invite-accept',
      component: () => import('@/views/Invite/InviteAcceptView.vue'),
    },
    {
      path: '/loginView',
      name: 'login',
      component: () => import('@/views/LoginView/LoginView.vue'),
    },
    {
      path: '/emailFind',
      name: 'emailFind',
      component: () => import('@/views/LoginView/EmailFindView.vue'),
    },
    {
      path: '/passwordFind',
      name: 'passwordFind',
      component: () => import('@/views/LoginView/PasswordFindView.vue'),
    },
    //회원 가입
    {
      path: '/joinView',
      name: 'join',
      component: () => import('@/views/JoinView/JoinView.vue'),
    },
    {
      path: '/joinProfileView',
      name: 'joinProfile',
      component: () => import('@/views/JoinView/JoinProfileView.vue'),
    },
    {
      path: '/joinComplete',
      name: 'complete',
      component: () => import('@/views/JoinView/JoinCompleteView.vue'),
    },
    //마이 페이지
    {
      path: '/myPage',
      name: 'myPage',
      component: () => import('@/views/MyPage/MyPage.vue'),
    },
     {
      path: '/testView',
      name: 'testView',
      component: () => import('@/views/MyPage/testView.vue'),
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
