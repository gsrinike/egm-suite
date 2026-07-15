import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: process.env.VITE_CSA_API_PROXY ?? 'http://localhost:8096',
        changeOrigin: true
      }
    }
  }
});
