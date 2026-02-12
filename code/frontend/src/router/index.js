import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  },
  {
    path: '/student',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      {
        path: 'dashboard',
        name: 'StudentDashboard',
        component: () => import('@/views/student/Dashboard.vue')
      },
      {
        path: 'calendar',
        name: 'ExerciseCalendar',
        component: () => import('@/views/student/Calendar.vue')
      },
      {
        path: 'checkin',
        name: 'ExerciseCheckin',
        component: () => import('@/views/student/Checkin.vue')
      },
      {
        path: 'progress',
        name: 'ProgressTracking',
        component: () => import('@/views/student/Progress.vue')
      },
      {
        path: 'plan',
        name: 'MyTrainingPlan',
        component: () => import('@/views/student/Plan.vue')
      },
      {
        path: 'achievements',
        name: 'Achievements',
        component: () => import('@/views/student/Achievements.vue')
      },
      {
        path: 'exercise-library',
        name: 'ExerciseLibrary',
        component: () => import('@/views/student/ExerciseLibrary.vue')
      },
      {
        path: 'settings',
        name: 'StudentSettings',
        component: () => import('@/views/student/Settings.vue')
      }
    ]
  },
  {
    path: '/coach',
    component: () => import('@/layouts/CoachLayout.vue'),
    meta: { requiresAuth: true, role: 'COACH' },
    children: [
      {
        path: 'dashboard',
        name: 'CoachDashboard',
        component: () => import('@/views/coach/Dashboard.vue')
      },
      {
        path: 'students',
        name: 'StudentList',
        component: () => import('@/views/coach/Students.vue')
      },
      {
        path: 'students/:id',
        name: 'StudentAnalytics',
        component: () => import('@/views/coach/StudentDetail.vue')
      },
      {
        path: 'plans',
        name: 'TrainingPlans',
        component: () => import('@/views/coach/Plans.vue')
      },
      {
        path: 'reports',
        name: 'EffectReports',
        component: () => import('@/views/coach/Reports.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      {
        path: 'coaches',
        name: 'CoachManagement',
        component: () => import('@/views/admin/Coaches.vue')
      },
      {
        path: 'students',
        name: 'StudentManagement',
        component: () => import('@/views/admin/Students.vue')
      },
      {
        path: 'monitoring',
        name: 'GymMonitoring',
        component: () => import('@/views/admin/Monitoring.vue')
      },
      {
        path: 'analytics',
        name: 'AdvancedAnalytics',
        component: () => import('@/views/admin/Analytics.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userRole = localStorage.getItem('role')
  
  // 检查是否需要认证（包括父路由的meta）
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  const requiredRole = to.matched.find(record => record.meta.role)?.meta.role
  
  if (requiresAuth && !token) {
    next('/login')
  } else if (requiredRole && requiredRole !== userRole) {
    next('/login')
  } else {
    next()
  }
})

export default router
