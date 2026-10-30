import { createApp } from "vue";
import App from "./App.vue";
import privateImage from "./privateImage";
import "./styles.css";
import router from "./router"; // 🔥 추가

createApp(App)
  .directive("private-src", privateImage)
  .use(router) // 🔥 핵심 추가
  .mount("#app");
