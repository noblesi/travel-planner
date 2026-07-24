const adminRouter = [
  // 관리자 로그인: 공통 관리자 레이아웃을 사용하지 않음
  {
    path: '/admin/login',
    name: 'admin-login',
    component: () => import('@/views/admin/AdminLoginView.vue'),
  },

  // 관리자 내부 페이지는 AdminLayout 아래에서 사이드바와 헤더를 공유합니다.
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    children: [
      {
        path: '',
        // /admin만 입력해도 기본 화면인 대시보드로 이동합니다.
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
        // :memberId는 상세 화면에서 route.params.memberId로 읽습니다.
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
        // :tripId는 목록에서 선택한 플랜 번호가 들어가는 동적 경로입니다.
        name: 'admin-trip-detail',
        component: () => import('@/views/admin/AdminTripDetailView.vue'),
        meta: {
          title: '여행 플랜 상세',
        },
      },
      {
        path: 'reports/:reportId',
        name: 'admin-report-detail',
        component: () => import('@/views/admin/AdminReportDetailView.vue'),
        meta: {
          title: '신고 상세',
        },
      },
      {
        path: 'notices',
        name: 'admin-notices',
        component: () => import('@/views/admin/AdminNoticeListView.vue'),
        meta: {
          title: '공지사항 관리',
        },
      },
      {
        path: 'notices/new',
        name: 'admin-notice-create',
        component: () => import('@/views/admin/AdminNoticeFormView.vue'),
        meta: {
          title: '공지사항 작성',
        },
      },
      {
        path: 'notices/:noticeId/edit',
        name: 'admin-notice-edit',
        component: () => import('@/views/admin/AdminNoticeFormView.vue'),
        meta: {
          title: '공지사항 수정',
        },
      },
      {
        path: 'notices/:noticeId',
        name: 'admin-notice-detail',
        component: () => import('@/views/admin/AdminNoticeDetailView.vue'),
        meta: {
          title: '공지사항 상세',
        },
      },
    ],
  },
]

export default adminRouter
