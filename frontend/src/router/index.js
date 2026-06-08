import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'WorkItemList',
    component: () => import('../views/WorkItemList.vue')
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