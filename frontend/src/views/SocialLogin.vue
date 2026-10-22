<template>
  <section class="status-page">
    <div class="status-card">로그인 정보를 확인하고 있습니다...</div>
  </section>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { setToken } from "../utils/auth";

const router = useRouter();

onMounted(() => {
  const params = new URLSearchParams(window.location.search);
  const token = params.get("token");

  if (token) {
    setToken(token);
    router.replace("/home");
    return;
  }

  alert("소셜 로그인에 실패했습니다.");
  router.replace("/");
});
</script>

<style scoped>
.status-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #fff4ea 0%, #ffd9cb 100%);
}

.status-card {
  padding: 28px 32px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  color: #4d3129;
  font-size: 18px;
  box-shadow: 0 16px 40px rgba(104, 58, 42, 0.14);
}
</style>