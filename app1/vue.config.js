const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    proxy: {
      // 与聊天记录一致：Agent 与知识图谱 API 均在 agent2 的 run_api.py（默认 5001）
      '/api/agent': {
        target: 'http://127.0.0.1:5001',
        changeOrigin: true
      },
      '/api/kg': {
        target: 'http://127.0.0.1:5001',
        changeOrigin: true
      },
      // 其余 /api/*（如原 dashboard）仍走 8080，并去掉 /api 前缀
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''
        }
      }
    }
  }
})

