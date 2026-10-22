import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    // Allow access from the local network so phone testing works too.
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": { target: "http://127.0.0.1:8082", changeOrigin: true },
      "/uploads": { target: "http://127.0.0.1:8082", changeOrigin: true },
    },
  },
});
