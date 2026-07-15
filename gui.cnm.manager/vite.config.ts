import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_CNM_API_PROXY ?? 'http://localhost:8084',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@egm/gui.common': fileURLToPath(new URL('../gui.common', import.meta.url))
    },
    dedupe: ['vue']
  }
});
