<template>
    <header class="header">
      <div class="nav left">
        <ul>
          <li :class="{ nav_active: $route.path === '/' }">
            <i class="nav_1"></i>
            <router-link to="/">数据概览</router-link>
          </li>
          <li :class="{ nav_active: $route.path === '/TaskManagementView' }">
            <i class="nav_7"></i>
            <router-link to="/TaskManagementView">查询统计</router-link>
          </li>
        </ul>
      </div>
      
      <div class="header_center">
        <h2><strong>四川省社科大脑服务平台</strong></h2>
        <p class="color_font">
          <small>Sichuan Provincial service platform for Social Science Brain</small>
        </p>
      </div>
  
      <div class="nav right">
        <ul style="text-align: right;">
          <li :class="{ nav_active: $route.path === '/AgentChat' }">
            <!-- 聊天记录：与「查询统计」的 nav_7 区分，使用 nav_6 -->
            <i class="nav_6"></i>
            <router-link to="/AgentChat">智能分析助手</router-link>
          </li>
          <li :class="{ nav_active: $route.path === '/knowledge-graph' }">
            <i class="nav_8"></i>
            <router-link to="/knowledge-graph">知识图谱</router-link>
          </li>
        </ul>
      </div>
    </header>
  </template>
  <script>
    export default {
        name:'HeaderW'
    }
</script>
  <style scoped>
    @import "@/assets/styles/base.css";
  </style>