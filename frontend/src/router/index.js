import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'WorkItemList',
    component: () => import('../views/WorkItemList.vue')
  },
  // 兼容直接访问 #/work-items 的场景（用户可能从书签或历史记录进入）
  {
    path: '/work-items',
    redirect: '/'
  },
  {
    path: '/work-items/:id',
    name: 'WorkItemDetail',
    component: () => import('../views/WorkItemDetail.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router