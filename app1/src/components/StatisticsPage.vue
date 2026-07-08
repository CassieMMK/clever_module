<script setup>
import { ref } from 'vue';
import axios from 'axios';

// 搜索参数
const searchQuery = ref('');
const searchType = ref('个人'); // '个人' 或 '组织'
const paperType = ref('CSCI'); // 'CSCI' 或 'SSCI_HCI'

// 结果和状态
const results = ref([]);
const loading = ref(false);
const errorMessage = ref('');

// 从后端获取的查询元数据
const searchTitle = ref(''); // 用于显示 request.getName() 返回的标题
const isCSCIType = ref(false); // 用于显示后端返回的布尔标记

// 分页参数
const currentPage = ref(1);
const pageSize = ref(10); // 每页显示数量
const totalPages = ref(0);
const totalResults = ref(0);

const API_ENDPOINT = '/api/sheke/dataview/select'; // 假设 /api 会被代理到后端 /select

const handleSearch = async (isPageChange = false) => {
  if (!isPageChange) {
    currentPage.value = 1; // 如果是新的搜索，重置到第一页
  }
  loading.value = true;
  errorMessage.value = '';
  results.value = []; // 清空旧结果

  // 修正 paperType 以匹配后端期望
  let backendPaperType = paperType.value;
  if (paperType.value === 'CSCI') {
    backendPaperType = 'CSCI';
  } else if (paperType.value === 'SSCI_HCI') {
    backendPaperType = 'SSCI&HCI';
  }
  const requestPayload = {
    name: searchQuery.value,
    page: currentPage.value - 1, // page从0开始
    pageSize: pageSize.value,
    paperType: backendPaperType,
    selectType: searchType.value
  };
  console.log('请求参数:', requestPayload);
  try {
    const response = await axios.post(API_ENDPOINT, requestPayload);
    console.log('后端返回:', response);

    if (
        response &&
        response.data &&
        (response.data.code === 200 || response.data.code === 0 || response.data.code === "0")
    ) {
      const data = response.data.data;
      console.log('后端data:', data);

      // 兼容 CSCI/SSCI 返回
      if (data) {
        searchTitle.value = data.name || searchQuery.value;
        isCSCIType.value = !!data.csciList;

        if (data.csciList) {
          results.value = data.csciList;
          totalResults.value = data.pagination?.totalCount || data.csciList.length;
          totalPages.value = Math.ceil(totalResults.value / pageSize.value);
        } else if (data.ssciList) {
          results.value = data.ssciList;
          totalResults.value = data.pagination?.totalCount || data.ssciList.length;
          totalPages.value = Math.ceil(totalResults.value / pageSize.value);
        } else {
          results.value = [];
          totalResults.value = 0;
          totalPages.value = 0;
        }
      } else {
        errorMessage.value = '未获取到有效数据结构。';
        results.value = [];
        totalResults.value = 0;
        totalPages.value = 0;
      }
    } else {
      console.error('查询失败，后端返回:', response && response.data);
      errorMessage.value =
          (response && response.data && response.data.message)
              ? response.data.message
              : '查询失败，请检查网络或联系管理员。';
      results.value = [];
      totalResults.value = 0;
      totalPages.value = 0;
    }
  } catch (error) {
    console.error("请求失败:", error);
    if (error.response && error.response.data && error.response.data.message) {
      errorMessage.value = `请求错误: ${error.response.data.message}`;
    } else if (error.request) {
      errorMessage.value = '请求已发出，但未收到响应。请检查后端服务是否运行。';
    } else {
      errorMessage.value = `请求设置时发生错误: ${error.message}`;
    }
    results.value = [];
    totalResults.value = 0;
    totalPages.value = 0;
  } finally {
    loading.value = false;
  }
};

const changePage = (newPage) => {
  if (newPage >= 1 && newPage <= totalPages.value && newPage !== currentPage.value) {
    currentPage.value = newPage;
    handleSearch(true); // 传递 true 表示是翻页操作
  }
};

</script>

<template>
  <div class="search-container">
    <div class="search-bar">
      <select v-model="searchType" class="search-select">
        <option value="个人">个人</option>
        <option value="组织">组织</option>
      </select>
      <select v-model="paperType" class="search-select">
        <option value="CSCI">CSCI</option>
        <option value="SSCI_HCI">SSCI & HCI</option>
      </select>
      <input v-model="searchQuery" class="search-input" type="text" placeholder="请输入搜索内容" @keyup.enter="handleSearch(false)" />
      <button @click="handleSearch(false)" class="search-btn" :disabled="loading">
        <img v-if="!loading" src="@/assets/img/find.png" alt="搜索" class="search-icon" />
        <span v-else>搜索中...</span>
      </button>
    </div>
    <div v-if="loading" class="loading-indicator">加载中...</div>
    <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>

    <div v-if="!loading && results.length > 0" class="results-container">
      <h2>搜索 "{{ searchTitle }}" 的结果 <span v-if="totalResults > 0">({{ totalResults }} 条)</span></h2>
      <p class="result-type-indicator">
        <span v-if="isCSCIType">当前显示: CSCI & HCI 类论文</span>
        <span v-else>当前显示: SSCI 类论文</span>
      </p>
      <ul class="search-results-list">
        <li v-for="(paper, idx) in results" :key="paper.id || idx" class="result-item">
          <h3 class="paper-title">{{ paper.title || paper.firstAuthor || '无标题' }}</h3>
          <p><strong>作者:</strong> {{ paper.authors || paper.firstAuthor || '未知作者' }}</p>
          <p><strong>单位:</strong> {{ paper.institution || '未知单位' }}</p>
          <p v-if="paper.publishYear || paper.year"><strong>年份:</strong> {{ paper.publishYear || paper.year }}</p>
          <p v-if="paper.journal"><strong>期刊:</strong> {{ paper.journal }}</p>
          <p v-if="paper.conference"><strong>会议:</strong> {{ paper.conference }}</p>
          <p v-if="paper.doi">
            <strong>DOI:</strong>
            <a :href="`https://doi.org/${paper.doi}`" target="_blank" rel="noopener noreferrer">{{ paper.doi }}</a>
          </p>
          <p v-if="paper.abstract"><strong>摘要:</strong> {{ paper.abstract.substring(0, 200) }}...</p>
        </li>
      </ul>
      <div v-if="totalPages > 1" class="pagination-controls">
        <button @click="changePage(1)" :disabled="currentPage === 1">首页</button>
        <button @click="changePage(currentPage - 1)" :disabled="currentPage <= 1">上一页</button>
        <span>第 {{ currentPage }} 页 / 共 {{ totalPages }} 页</span>
        <button @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages">下一页</button>
        <button @click="changePage(totalPages)" :disabled="currentPage === totalPages">末页</button>
      </div>
    </div>
    <div v-if="!loading && !errorMessage && results.length === 0 && totalResults === 0" class="no-results">
      没有找到相关内容，请输入查内容。
    </div>
  </div>
</template>

<style scoped lang="scss">
.results-container {
  margin-top: 20px;
}
.search-results-list {
  list-style: none;
  padding: 0;
}
.result-item {
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}
.paper-title {
  font-weight: bold;
  color: #0056b3;
}
.search-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 30px;
  padding: 0 20px;
  font-family: 'Arial', sans-serif;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 8px;
  background-color: #fff;
  padding: 10px 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  flex-wrap: wrap; /* 允许换行 */
}

.search-select,
.search-input {
  padding: 10px;
  font-size: 16px;
  height: 48px;
  border-radius: 6px;
  border: 1px solid #ddd;
  box-sizing: border-box;
}

.search-select {
  min-width: 120px;
}

.search-input {
  flex-grow: 1;
  min-width: 300px; /* 保证输入框有足够宽度 */
}

.search-btn {
  padding: 0 15px; /* 调整内边距以适应内容 */
  font-size: 16px;
  cursor: pointer;
  background-color: #007bff;
  height: 48px;
  color: white;
  border: none;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 100px; /* 按钮最小宽度 */
}

.search-btn:hover {
  background-color: #0056b3;
}
.search-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.search-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.loading-indicator,
.error-message,
.no-results {
  margin-top: 20px;
  padding: 15px;
  border-radius: 6px;
  width: 100%;
  max-width: 800px;
  text-align: center;
}

.loading-indicator {
  color: #007bff;
}

.error-message {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.no-results {
  color: #666;
}

.results-container {
  background-color: #fff;
  width: 100%;
  max-width: 1000px; /* 根据内容调整 */
  padding: 25px;
  margin-top: 10px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  border-radius: 8px;

  h2 {
    margin-top: 0;
    margin-bottom: 10px;
    color: #333;
    font-size: 1.8em;
  }
  .result-type-indicator {
    font-size: 0.9em;
    color: #555;
    margin-bottom: 20px;
  }
}

.search-results-list {
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.result-item {
  border-bottom: 1px solid #eee;
  padding: 20px 0;
  &:last-child {
    border-bottom: none;
  }

  .paper-title {
    font-size: 1.4em;
    color: #0056b3;
    margin-top: 0;
    margin-bottom: 8px;
  }

  p {
    font-size: 0.95em;
    color: #444;
    margin-bottom: 6px;
    line-height: 1.6;
    strong {
      color: #111;
    }
  }
  .paper-doi a {
    color: #007bff;
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }
}

.pagination-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 30px;
  gap: 8px;
  flex-wrap: wrap; /* 允许换行 */

  button {
    padding: 8px 15px;
    font-size: 14px;
    border: 1px solid #ddd;
    background-color: #f7f7f7;
    color: #333;
    cursor: pointer;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover:not(:disabled) {
      background-color: #e9e9e9;
    }

    &:disabled {
      color: #aaa;
      cursor: not-allowed;
      background-color: #f0f0f0;
    }
  }

  span {
    font-size: 14px;
    color: #555;
    margin: 0 5px;
  }
}
</style>
