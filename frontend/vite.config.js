import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, fileURLToPath(new URL('.', import.meta.url)), 'VITE_')
  const apiProxy = {
    '/api': {
      target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
    },
    '/admin': {
      target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
    },
    '/assets/admin': {
      target: env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
      changeOrigin: true,
    },
  }

  return {
    plugins: [vue(), mode === 'development' && vueDevTools()].filter(Boolean),
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      port: 5173,
      strictPort: true,
      proxy: apiProxy,
    },
    preview: {
      port: 4173,
      strictPort: true,
      proxy: apiProxy,
    },
  }
})
