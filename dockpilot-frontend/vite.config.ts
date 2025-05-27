import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 读取package.json中的版本号
const packageJson = require('./package.json')

export default defineConfig({
  plugins: [vue()],
  define: {
    // 注入版本信息到应用中
    'process.env.VUE_APP_VERSION': JSON.stringify(`v${packageJson.version}`),
    'process.env.VUE_APP_BUILD_TIME': JSON.stringify(new Date().toISOString()),
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3001,
    host: '0.0.0.0',
    open: true,
    cors: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/upload': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:8080',
        ws: true,
        changeOrigin: true
      }
    },
  },
  build: {
    target: 'es2015',
    outDir: 'dist',
    assetsDir: 'assets',
    minify: 'terser',
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          'naive-ui': ['naive-ui'],
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
        },
      },
    },
  },
}) 