<!-- 定义 Vue 组件的模板部分，所有模板内容需要包裹在 template 标签中 -->
<template>
  <!-- 根容器 div，id 与挂载点对应，通常作为整个 Vue 应用的视觉容器 -->
  <div id="app">
    <!-- 头部组件：显示页面头部内容，需在 components 中注册后使用 -->
    <header-component />

    <!-- 页面主体内容区域，class 用于样式控制 -->
    <main class="app-main">
      <!-- Vue Router 的路由出口：根据当前路由路径动态渲染匹配的组件 -->
      <router-view />
    </main>
  </div>
</template>

<script>
import { defineComponent } from 'vue'// 从 Vue 中导入 defineComponent 方法，用于定义 Vue 组件（TypeScript 友好）
import HeaderComponent from '@/components/Header.vue'// 导入自定义头部组件，@ 符号通常指向项目 src 目录（需构建工具配置）
// 导出使用 Composition API 定义的组件
export default defineComponent({
  name: 'App',// 组件名称（用于调试和递归组件）
  components: {
    HeaderComponent// 注册子组件，使其可在模板中使用 <header-component> 标签
  }
})
</script>

<!-- 组件样式部分：使用 SCSS 预处理器编写样式 -->
<style lang="scss">
/* 导入基础全局样式文件（CSS 变量、重置样式等） */
@import '@/assets/styles/base.css'; // 保留原基础样式

/* 主内容区域样式 */
.app-main {
  padding: 20px;
  background: #081832;
  min-height: calc(100vh - 80px);  /* 最小高度计算：100% 视口高度减去头部高度（假设头部高80px） */
}
</style>