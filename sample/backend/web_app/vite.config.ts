import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@protobufjs/inquire': fileURLToPath(
        new URL('./src/shims/protobuf-inquire.cjs', import.meta.url),
      ),
    },
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'vendor-react',
              test: /node_modules[\\/](react|react-dom)[\\/]/,
              priority: 40,
            },
            {
              name: 'vendor-motion',
              test: /node_modules[\\/]framer-motion[\\/]/,
              priority: 30,
            },
            {
              name: 'vendor-mediapipe',
              test: /node_modules[\\/]@mediapipe[\\/]tasks-vision[\\/]/,
              priority: 30,
            },
            {
              name: 'vendor-protobuf',
              test: /node_modules[\\/](protobufjs|@protobufjs)[\\/]/,
              priority: 30,
            },
          ],
        },
      },
    },
  },
})
