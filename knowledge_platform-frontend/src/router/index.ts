import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/home',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
      meta: { title: '首页' }
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: '登录', hideInMenu: true }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/Register.vue'),
      meta: { title: '注册', hideInMenu: true }
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: () => import('@/views/ForgotPassword.vue'),
      meta: { title: '忘记密码', hideInMenu: true }
    },
    {
      path: '/documents',
      name: 'Documents',
      component: () => import('@/views/Documents.vue'),
      meta: { title: '文档库' }
    },
    {
      path: '/documents/:id',
      name: 'DocumentDetail',
      component: () => import('@/views/DocumentDetail.vue'),
      meta: { title: '文档详情', hideInMenu: true }
    },
    {
      path: '/documents/:id/preview',
      name: 'DocumentPreview',
      component: () => import('@/views/DocumentPreview.vue'),
      meta: { title: '文档预览', hideInMenu: true }
    },
    {
      path: '/upload',
      name: 'Upload',
      component: () => import('@/views/Upload.vue'),
      meta: { title: '上传文档', requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { title: '个人中心', requiresAuth: true }
    },
    {
      path: '/notifications',
      name: 'Notifications',
      component: () => import('@/views/Notifications.vue'),
      meta: { title: '通知中心', requiresAuth: true }
    },
    {
      path: '/my/appeals',
      name: 'MyAppeals',
      component: () => import('@/views/user/AppealManagement.vue'),
      meta: { title: '我的申诉', requiresAuth: true }
    },
    {
      path: '/copyright/report',
      name: 'CopyrightReport',
      component: () => import('@/views/user/CopyrightReport.vue'),
      meta: { title: '侵权举报', requiresAuth: true }
    },
    {
      path: '/community',
      name: 'CommunityHome',
      component: () => import('@/views/community/CommunityHome.vue'),
      meta: { title: '社区交流' }
    },
    {
      path: '/community/topic/:id',
      name: 'TopicDetail',
      component: () => import('@/views/community/TopicDetail.vue'),
      meta: { title: '帖子详情', hideInMenu: true }
    },
    {
      path: '/community/create',
      name: 'CreateTopic',
      component: () => import('@/views/community/CreateTopic.vue'),
      meta: { title: '发布帖子', requiresAuth: true, hideInMenu: true }
    },
    {
      path: '/ai',
      name: 'AiHome',
      component: () => import('@/views/ai/AiHome.vue'),
      meta: { title: 'AI 智能助手', requiresAuth: true }
    },
    {
      path: '/dashboard',
      redirect: '/admin/dashboard'
    },
    {
      path: '/documents/dashboard',
      redirect: '/admin/dashboard'
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/Admin.vue'),
      meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: 'dashboard'
        },
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/Dashboard.vue'),
          meta: { title: '管理总览' }
        },
        {
          path: 'documents',
          name: 'AdminDocuments',
          component: () => import('@/views/admin/Documents.vue'),
          meta: { title: '文档管理' }
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('@/views/admin/Users.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'community/topics',
          name: 'AdminCommunityTopics',
          component: () => import('@/views/admin/community/Topics.vue'),
          meta: { title: '帖子管理' }
        },
        {
          path: 'community/replies',
          name: 'AdminCommunityReplies',
          component: () => import('@/views/admin/community/Replies.vue'),
          meta: { title: '评论管理' }
        },
        {
          path: 'community/categories',
          name: 'AdminCommunityCategories',
          component: () => import('@/views/admin/community/ForumCategories.vue'),
          meta: { title: '板块管理' }
        },
        {
          path: 'categories',
          name: 'AdminCategories',
          component: () => import('@/views/admin/Categories.vue'),
          meta: { title: '分类管理' }
        },
        {
          path: 'tags',
          name: 'AdminTags',
          component: () => import('@/views/admin/Tags.vue'),
          meta: { title: '标签管理' }
        },
        {
          path: 'notifications',
          name: 'AdminNotifications',
          component: () => import('@/views/admin/Notifications.vue'),
          meta: { title: '通知管理' }
        },
        {
          path: 'reviews',
          name: 'AdminReviews',
          component: () => import('@/views/admin/ReviewManagement.vue'),
          meta: { title: '文档审核' }
        },
        {
          path: 'appeals',
          name: 'AdminAppeals',
          component: () => import('@/views/admin/AppealHandling.vue'),
          meta: { title: '申诉处理' }
        },
        {
          path: 'reports',
          name: 'AdminReports',
          component: () => import('@/views/admin/ReportHandling.vue'),
          meta: { title: '举报处理' }
        },
      ]
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 知享校园知识库`
  }

  const isAuthPage = to.name === 'Login' || to.name === 'Register'

  if (isAuthPage && userStore.isLoggedIn) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/home'
    next(redirect)
    return
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
    return
  }

  // 检查是否需要管理员权限
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Home' })
    return
  }

  next()
})

export default router
