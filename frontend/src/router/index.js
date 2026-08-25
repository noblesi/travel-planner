import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'
import { useJoinDraftStore } from '@/stores/joinDraft'

const JOIN_FLOW_ROUTE_NAMES = new Set(['join', 'joinProfile'])

// 사용자 라우트와 기능별로 분리한 관리자 라우트를 하나의 Router에 등록합니다.
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    // 등록되지 않은 모든 URL은 마지막에 404 화면으로 보냅니다.
    {
      path: '/plans/new',
      name: 'plan-setup',
      component: () => import('@/views/PlanSetupView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/plans/:planId/edit',
      name: 'plan-editor',
      component: () => import('@/views/PlanEditorView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/my-plans',
      name: 'my-plans',
      component: () => import('@/views/MyPlansView.vue'),
      meta: { requiresAuth: true },
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
    //회원 가입
    {
      path: '/joinView',
      name: 'join',
      component: () => import('@/views/joinView/JoinView.vue'),
      beforeEnter: () => {
        useJoinDraftStore().clearRegistration()
        return true
      },
    },
    {
      path: '/joinProfileView',
      name: 'joinProfile',
      component: () => import('@/views/joinView/JoinProfileView.vue'),
      beforeEnter: () => {
        const store = useJoinDraftStore()
        if (!store.hasCredentials) {
          return { name: 'join', query: { reset: true } }
        }
        return true
      },
    },
    {
      path: '/joinComplete',
      name: 'complete',
      component: () => import('@/views/joinView/JoinCompleteView.vue'),
    },
    //이메일 찾기
    {
      path: '/findEmail',
      name: 'findEmail',
      component: () => import('@/views/loginView/EmailFindView.vue')
    },
    //비밀번호 찾기
    {
      path: '/findPassword',
      name: 'findPassword',
      component: () => import('@/views/loginView/PasswordFindView.vue')
    },
    //마이 페이지
    {
      path: '/myPage',
      name: 'myPage',
      component: () => import('@/views/myPage/MyPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.path === from.path) {
      return false // 스크롤 유지,  아무 것도 안 함
    }
    return { top: 0 }
  },
})

router.beforeEach(async (to, from) => {
  if (JOIN_FLOW_ROUTE_NAMES.has(from.name) && !JOIN_FLOW_ROUTE_NAMES.has(to.name)) {
    useJoinDraftStore().clearRegistration()
  }

  if (!to.meta.requiresAuth) return true

  const authStore = useAuthStore()
  if (!authStore.initialized) await authStore.restoreSession()
  if (authStore.isAuthenticated) return true

  return {
    name: 'login',
    query: { redirect: to.fullPath },
  }
})

export default router
