import path from 'path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '');
  const isProd = mode === 'production';
  const backendTarget = (env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');
  const devHost = env.VITE_DEV_HOST || '127.0.0.1';
  const devPort = Number(env.VITE_DEV_PORT || '5173');
  const strictPort = String(env.VITE_DEV_STRICT_PORT || 'false').toLowerCase() === 'true';

  return {
    server: {
      port: devPort,
      host: devHost,
      strictPort,
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true,
        },
        '/client': {
          target: backendTarget,
          changeOrigin: true,
        }
      }
    },
    plugins: [react()],
    define: {
      'process.env.API_KEY': JSON.stringify(env.GEMINI_API_KEY),
      'process.env.GEMINI_API_KEY': JSON.stringify(env.GEMINI_API_KEY)
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      }
    },
    build: {
      sourcemap: !isProd,
      minify: isProd ? 'esbuild' : false,
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-react': ['react', 'react-dom', 'react-router-dom'],
            'vendor-antd': ['antd'],
            'vendor-xyflow': ['@xyflow/react'],
            'vendor-utils': ['axios', 'zustand', 'dayjs', 'zod']
          }
        }
      },
      chunkSizeWarningLimit: 1000
    }
  };
});
