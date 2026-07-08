<template>
  <div class="task-management-container">
    <!-- Sidebar Placeholder -->
    <div class="sidebar-placeholder">
      <!-- Content for sidebar based on image -->
      <div class="logo">查询</div>
      <nav class="navigation">
        <ul>
          <li :class="{active: paperType==='CSCI'}" @click="selectType('CSCI')"><span>CSCI论文</span></li>
          <li :class="{active: paperType==='SSCI&HCI'}" @click="selectType('SSCI&HCI')"><span>SSCI/HCI论文</span></li>
        </ul>
      </nav>
    </div>

    <!-- Main Content Area -->
    <div class="main-content">
      <!-- Header/Filter Area -->
      <div class="header-filter-area">
        <!--<div class="breadcrumb">任务清理</div>-->
        <div class="filter-controls">
          <div class="filter-item">
            <span>论文名称:</span>
            <input v-model="paperName" type="text" placeholder="请输入论文名称" class="search-input">
          </div>
          <div class="filter-item">
            <span>作者名称:</span>
            <input v-model="authorName" type="text" placeholder="请输入第一作者" class="search-input">
          </div>
          <div class="filter-item">
            <span>组织机构:</span>
            <input v-model="organization" type="text" placeholder="请输入组织机构" class="search-input">
          </div>
          <div class="filter-item">
            <span>发表年份:</span>
            <input v-model="publishYear" type="text" placeholder="请输入发表年份" class="search-input">
          </div>
          <div class="filter-item">
            <span>研究方向:</span>
            <input v-model="researchField" type="text" placeholder="请输入研究方向" class="search-input">
          </div>
          <button class="action-button search" @click="handleSearch">搜索</button>
          <button class="action-button reset" @click="resetForm">重置</button>
        </div>
        <div class="action-buttons-top">
          <button class="action-button stop">一键停止</button>
          <button class="action-button delete">一键删除</button>
          <button class="action-button expand">展开 v</button>
        </div>
      </div>

      <!-- loading/error/无数据 -->
      <div v-if="loading" class="loading-indicator">加载中...</div>
      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
      <div v-if="!loading && !errorMessage && results.length === 0" class="no-results">暂无数据</div>

      <!-- 动态表格区域 -->
      <div class="table-area" v-if="results.length > 0">
        <table>
          <colgroup>
            <col style="width: 60px;" /> <!-- 序号 -->
            <col /> <!-- 论文名称 -->
            <col style="width: 120px;" /> <!-- 第一作者 -->
            <col /> <!-- 组织机构 -->
            <col style="width: 200px;" /> <!-- 全部作者 -->
            <col style="width: 100px;" /> <!-- 发表年份 -->
            <col /> <!-- 研究方向 -->
          </colgroup>
          <thead>
            <tr>
              <th>序号</th>
              <th>论文名称</th>
              <th>第一作者</th>
              <th>组织机构</th>
              <th>全部作者</th>
              <th>发表年份</th>
              <th>研究方向</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in results" :key="idx">
              <td>{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td>{{ item.objName || '--' }}</td>
              <td>{{ item.firstAuthor || '--' }}</td>
              <td>{{ item.institution || '--' }}</td>
              <td>{{ item.authors || '--' }}</td>
              <td>{{ item.publishYear || '--' }}</td>
              <td>{{ item.researchDirection || '--' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页区域 -->
      <div class="pagination-area" v-if="totalPages > 1">
        <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1">&lt;</button>
        <button v-for="page in visiblePages" :key="page" :class="{active: page === currentPage}" @click="changePage(page)">{{ page }}</button>
        <button @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages">&gt;</button>
        <span>共 {{ totalResults }} 条</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'

// 查询参数
const paperName = ref('')
const authorName = ref('')
const organization = ref('')
const publishYear = ref('')
const researchField = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const paperType = ref('CSCI') // 默认CSCI

// 结果与状态
const results = ref([])
const totalResults = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const totalPages = ref(0)

function selectType(type) {
  if (paperType.value !== type) {
    paperType.value = type
    currentPage.value = 1
    handleSearch()
  }
}

const handleSearch = async (isPageChange = false) => {
  if (!isPageChange) currentPage.value = 1
  loading.value = true
  errorMessage.value = ''
  results.value = []
  try {
    const payload = {
      authorName: authorName.value,
      organization: organization.value,
      page: currentPage.value - 1,
      paperName: paperName.value,
      paperType: paperType.value,
      publishYear: publishYear.value,
      researchField: researchField.value,
      size: pageSize.value
    }
    const response = await axios.post('/api/sheke/dataview/search', payload)
    console.log('111111111111111111')
    if (response.data && response.data.success && response.data.data) {
      const data = response.data.data
      // 兼容CSCI和SSCI类型
      if (paperType.value === 'CSCI') {
        results.value = Array.isArray(data.csciList) ? data.csciList : []
      } else if (paperType.value === 'SSCI') {
        results.value = Array.isArray(data.ssciList) ? data.ssciList : []
      } else if (paperType.value === 'SSCI&HCI') {
        results.value = Array.isArray(data.ssciList) ? data.ssciList : []
      } else {
        results.value = []
      }
      totalResults.value = data.pagination?.totalCount || results.value.length
      totalPages.value = Math.ceil(totalResults.value / pageSize.value)
    } else {
      errorMessage.value = response.data?.msg || '查询失败'
    }
  } catch (e) {
    errorMessage.value = e.message || '请求失败'
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  paperName.value = ''
  authorName.value = ''
  organization.value = ''
  publishYear.value = ''
  researchField.value = ''
}

const changePage = (page) => {
  if (page >= 1 && page <= totalPages.value && page !== currentPage.value) {
    currentPage.value = page
    handleSearch(true)
  }
}

// 分页按钮显示逻辑（最多显示5页，当前页居中）
const visiblePages = computed(() => {
  const pages = []
  if (totalPages.value <= 5) {
    for (let i = 1; i <= totalPages.value; i++) pages.push(i)
  } else {
    let start = Math.max(1, currentPage.value - 2)
    let end = Math.min(totalPages.value, start + 4)
    if (end - start < 4) start = Math.max(1, end - 4)
    for (let i = start; i <= end; i++) pages.push(i)
  }
  return pages
})
</script>

<style scoped>
.task-management-container {
  display: flex;
  height: 100vh;
  background-color: #f0f2f5; /* Light background for main content area */
}

.sidebar-placeholder {
  width: 200px;
  background-color: #081832; /* Dark blue from image */
  color: #fff;
  padding: 20px 0;
  box-sizing: border-box;
}

.logo {
  text-align: center;
  margin-bottom: 30px;
  font-size: 18px;
  font-weight: bold;
}

.navigation ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.navigation li {
  padding: 10px 20px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navigation li:hover {
  background-color: #1a2e4a;
}

.navigation li.active {
  background-color: #1a2e4a;
  border-left: 4px solid #409eff; /* Highlight color */
  padding-left: 16px;
}

.navigation .has-children .arrow {
  font-size: 12px;
}

.navigation .sub-menu {
  list-style: none;
  padding: 0;
  margin: 5px 0 0 15px;
  border-left: 1px solid #3a4c63;
}

.navigation .sub-menu li {
  padding: 8px 10px;
  font-size: 14px;
}

.navigation .sub-menu li.active {
    background-color: #409eff; /* Highlight color */
    color: #fff;
    border-left: none;
     padding-left: 10px;
}

.main-content {
  flex-grow: 1;
  padding: 20px;
  box-sizing: border-box;
}

.header-filter-area {
  background-color: #fff;
  padding: 20px;
  margin-bottom: 20px;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.breadcrumb {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
}

.filter-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 15px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.search-input,
.filter-select {
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.search-icon-button {
  padding: 8px 12px;
  background-color: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.action-buttons-top {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.action-button {
  padding: 8px 15px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: #fff;
}

.action-button.search {
  background-color: #409eff;
}

.action-button.reset {
  background-color: #909399;
}
.action-button.stop {
    background-color: #e6a23c;
}
.action-button.delete {
    background-color: #f56c6c;
}
.action-button.expand {
     background-color: #409eff;
}

.table-area {
  background-color: #fff;
  padding: 20px;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border: 1px solid #ebeef5;
  padding: 12px;
  text-align: left;
}

th {
  background-color: #f5f7fa;
  font-weight: bold;
}

.table-area tbody tr:nth-child(even) {
  background-color: #fafafa;
}

.table-area tbody tr:hover {
  background-color: #f0f9eb;
}

.link {
  color: #409eff;
  cursor: pointer;
  margin-right: 5px;
}

.pagination-area {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 20px;
  gap: 5px;
}

.pagination-area button {
  padding: 5px 10px;
  border: 1px solid #ccc;
  background-color: #fff;
  cursor: pointer;
  border-radius: 4px;
}

.pagination-area button.active {
  background-color: #409eff;
  color: #fff;
  border-color: #409eff;
}

.pagination-area span {
  margin: 0 10px;
}

.table-area .status-running {
    color: #67c23a; /* Green color for running status */
}
</style> 