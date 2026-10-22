import { createApp } from "vue";
import App from "./App.vue";
import "./styles.css";
import router from "./router"; // 🔥 추가

createApp(App)
  .use(router) // 🔥 핵심 추가
  .mount("#app");
