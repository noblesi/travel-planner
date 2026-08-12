import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import { useAuthStore } from '@/stores/auth'
import { useUserStore } from '@/stores/useUserStore'

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
      beforeEnter: (to, from, next) => {
      const store = useUserStore()
      // Step 1에서 반드시 넘겨야 하는 데이터(예: userId)가 있는지 확인
      if (!store.step1Data.email || !store.step1Data.password) {
        // 데이터가 없으면 1단계로 돌려보냅니다.
        next({ name: 'Step1', query: { reset: true } })
      } else {
        next()
      }
    }
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

router.beforeEach(async (to) => {
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
