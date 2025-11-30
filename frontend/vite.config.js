import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backend = env.VITE_BACKEND_URL || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/auth': backend,
        '/queries': backend,
        '/admin': backend,
      },
    },
    build: {
      outDir: 'dist',
    },
  };
});
