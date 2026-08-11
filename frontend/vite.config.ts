import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The backend serves everything under /api (server.servlet.context-path), so the
// dev server can forward the prefix untouched and CORS never comes up.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
