import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  {
    path: '/',
    component: () => import('@/components/Layout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue') },
      {
        path: 'divination',
        redirect: '/divination/type',
        children: [
          { path: 'type', name: 'TypeSelect', component: () => import('@/views/Divination/TypeSelect.vue') },
          { path: 'form', name: 'InfoForm', component: () => import('@/views/Divination/InfoForm.vue') },
          { path: 'result', name: 'Result', component: () => import('@/views/Divination/Result.vue') },
          { path: 'ziwei-form', name: 'ZiweiForm', component: () => import('@/views/Divination/ZiweiForm.vue') },
          { path: 'ziwei-result', name: 'ZiweiResult', component: () => import('@/views/Divination/ZiweiResult.vue') },
        ],
      },
      { path: 'personal', name: 'Personal', component: () => import('@/views/Personal/Index.vue') },
      { path: 'personal/record', name: 'RecordList', component: () => import('@/views/Personal/RecordList.vue') },
      { path: 'personal/exchange', name: 'CardExchange', component: () => import('@/views/Personal/CardExchange.vue') },
      { path: 'personal/signin', name: 'SignIn', component: () => import('@/views/Personal/SignIn.vue') },
      { path: 'personal/fortune', name: 'FortuneCalendar', component: () => import('@/views/Personal/FortuneCalendar.vue') },
      { path: 'personal/favorites', name: 'Favorites', component: () => import('@/views/Personal/Favorites.vue') },
      { path: 'personal/shop', name: 'Shop', component: () => import('@/views/Personal/Shop.vue') },
      { path: 'personal/trend', name: 'Trend', component: () => import('@/views/Personal/Trend.vue') },
      { path: 'personal/notifications', name: 'Notifications', component: () => import('@/views/Personal/Notifications.vue') },
      { path: 'personal/feedback', name: 'Feedback', component: () => import('@/views/Personal/Feedback.vue') },
      { path: 'personal/backpack', name: 'Backpack', component: () => import('@/views/Personal/Backpack.vue') },
      { path: 'personal/ai-chat', name: 'AiChat', component: () => import('@/views/Personal/AiChat.vue') },
      { path: 'personal/profile', name: 'UserProfile', component: () => import('@/views/Personal/Profile.vue') },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/components/Layout.vue'),
    redirect: '/admin/user',
    children: [
      { path: 'user', name: 'UserManage', component: () => import('@/views/Admin/UserManage.vue') },
      { path: 'card', name: 'CardManage', component: () => import('@/views/Admin/CardManage.vue') },
      { path: 'generate', name: 'CardGenerate', component: () => import('@/views/Admin/CardGenerate.vue') },
      { path: 'stat', name: 'DataStat', component: () => import('@/views/Admin/DataStat.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/login' || to.path === '/register') {
    next()
    return
  }

  if (!userStore.isLoggedIn()) {
    next('/login')
    return
  }

  if (to.path.startsWith('/admin') && !userStore.isAdmin()) {
    ElMessage.error('权限不足')
    next('/home')
    return
  }

  next()
})

export default router
