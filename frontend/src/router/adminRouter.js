const adminRouter = [
  // 관리자 로그인: 공통 관리자 레이아웃을 사용하지 않음
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/AdminLoginView.vue'),
  },

  // 관리자 내부 페이지
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      {
        path: '',
        redirect: {
          name: 'admin-dashboard',
        },
      },
      {
        path: 'dashboard',
        name: 'admin-dashboard',
        component: () => import('@/views/admin/AdminDashboardView.vue'),
        meta: {
          title: '대시보드',
        },
      },
      {
        path: 'members',
        name: 'admin-members',
        component: () => import('@/views/admin/AdminMemberListView.vue'),
        meta: {
          title: '회원 관리',
        },
      },
      {
        path: 'members/:memberId',
        name: 'admin-member-detail',
        component: () => import('@/views/admin/AdminMemberDetailView.vue'),
        meta: {
          title: '회원 상세',
        },
      },
      {
        path: 'trips',
        name: 'admin-trips',
        component: () => import('@/views/admin/AdminTripListView.vue'),
        meta: {
          title: '여행 플랜 관리',
        },
      },
      {
        path: 'trips/:tripId',
        name: 'admin-trip-detail',
        component: () => import('@/views/admin/AdminTripDetailView.vue'),
        meta: {
          title: '여행 플랜 상세',
        },
      },
    ],
  },
]

export default adminRouter
