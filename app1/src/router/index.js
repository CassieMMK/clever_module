import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
// import StaticView from '@/components/StatisticsPage.vue'
// import MessageView from '@/views/MessageView.vue'
// import Table1View from '@/views/Table1View.vue'
import TaskManagementView from '@/views/TaskManagementView.vue'
import AgentChatView from '@/views/AgentChatView.vue'
import KnowledgeGraphPage from '@/views/KnowledgeGraphPage.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/TaskManagementView',
    name: 'TaskManagementView',
    component: TaskManagementView
  },
  {
    path: '/AgentChat',
    name: 'AgentChat',
    component: AgentChatView
  },
  {
    path: '/knowledge-graph',
    name: 'KnowledgeGraph',
    component: KnowledgeGraphPage
  }

//   {
//     path: '/message',
//     name: 'message',
//     component: MessageView
//   },
//   {
//     path: '/table1',
//     name: 'table1',
//     component: Table1View
//   }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router

