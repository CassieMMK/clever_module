import axios from 'axios';

// 创建 Axios 实例，并设置全局 baseURL
const instance = axios.create({
    baseURL: process.env.VUE_APP_API_URL,  // 使用环境变量
    timeout: 5000,  // 请求超时时间
});

// 可选：添加请求拦截器
instance.interceptors.request.use(
    config => {
        // 在发送请求前添加 token 等逻辑
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 导出实例
export default instance;