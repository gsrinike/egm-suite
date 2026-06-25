import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: 'src/index.ts',
      name: 'GuiCommon',
      formats: ['es'],
      fileName: 'index'
    },
    rollupOptions: {
      external: ['vue']
    }
  }
});
