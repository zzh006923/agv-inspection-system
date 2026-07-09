import { createRouter, createWebHashHistory } from 'vue-router'
import InitView from '../views/InitView.vue'
import TaskView from '../views/TaskView.vue'
import TaskExecuteView from '../views/TaskExecuteView.vue'
import TaskDetailView from '../views/TaskDetailView.vue'
import SettingsView from '../views/SettingsView.vue'

const routes = [
  { path: '/', redirect: '/init' },
  { path: '/init', name: 'Init', component: InitView, meta: { title: '系统自检' } },
  { path: '/tasks', name: 'Tasks', component: TaskView, meta: { title: '任务列表' } },
  { path: '/task/:id/execute', name: 'TaskExecute', component: TaskExecuteView, meta: { title: '实时巡视' } },
  { path: '/task/:id/detail', name: 'TaskDetail', component: TaskDetailView, meta: { title: '任务复盘' } },
  { path: '/settings', name: 'Settings', component: SettingsView, meta: { title: '系统设置' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach(to => {
  document.title = `${to.meta.title || 'AGV'} - 智能巡检手持终端`
})

export default router
