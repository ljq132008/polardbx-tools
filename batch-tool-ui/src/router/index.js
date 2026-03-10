import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Jobs',
    component: () => import('../views/Jobs.vue'),
    meta: { title: '任务列表' }
  },
  {
    path: '/batch',
    name: 'Batch',
    component: () => import('../views/Batch.vue'),
    meta: { title: '命令执行' }
  },
  {
    path: '/rule-groups',
    name: 'RuleGroups',
    component: () => import('../views/RuleGroups.vue'),
    meta: { title: '规则组管理' }
  },
  {
    path: '/migration-rules',
    name: 'MigrationRules',
    component: () => import('../views/MigrationRules.vue'),
    meta: { title: '迁移规则' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
