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
      path: "/plans",
      name: "plan-search",
      component: () => import('@/views/PlanSearch/PlanSearchView.vue')
    },
    {
      path: "/plans/:id",
      name: "plan-detail",
      component: () => import('@/views/PlanSearch/PlanDetailView.vue'),
      props: true,
    },
    {
      path: "/announcements",
      name: "announcements-list",
      component: () => import('@/views/Announcement/AnnouncementListView.vue')
    },
    {
      path: "/announcements/:id",
      name: "announcements-detail",
      component: () => import('@/views/Announcement/AnnouncementDetailView.vue'),
      props: true
    },
    {
      path: "/invite",
      name: "invite",
      component: () => import('@/views/Invite/InviteView.vue'),
    },
    {
      path: "/invite/accept",
      name: "invite-accept",
      component: () => import('@/views/Invite/InviteAcceptView.vue'),
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
      return false  // 스크롤 유지,  아무 것도 안 함
    }
    return { top: 0 }
  },
})

export default router
