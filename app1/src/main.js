import { createApp } from 'vue'// 从 Vue 框架中导入 createApp 方法，用于创建 Vue 应用实例
import App from './App.vue'// 导入根组件 App.vue，这将是整个应用的入口组件
import router from './router'// 导入路由配置对象，通常包含路由规则和路由相关配置

const app = createApp(App)// 使用 createApp 方法创建 Vue 应用实例，并传入根组件 App
app.use(router)// 使用应用实例注册路由插件，这将激活 Vue Router 的功能
app.mount('#app')// 将 Vue 应用实例挂载到 DOM 中 id 为 "app" 的 HTML 元素上，启动整个应用