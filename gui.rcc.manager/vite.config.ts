import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api/csa': {
        target: process.env.VITE_CSA_API_PROXY ?? 'http://localhost:8096',
        changeOrigin: true
      },
      '/api/cnm': {
        target: process.env.VITE_CNM_API_PROXY ?? 'http://localhost:8084',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@egm/gui.common': fileURLToPath(new URL('../gui.common', import.meta.url)),
      '@egm/gui.cnm.manager': fileURLToPath(new URL('../gui.cnm.manager', import.meta.url))
    },
    dedupe: ['vue']
  }
});
