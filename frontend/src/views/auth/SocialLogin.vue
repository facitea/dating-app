<template>
  <section class="status-page">
    <div class="status-card">로그인 정보를 확인하고 있습니다...</div>
  </section>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { setToken } from "../../utils/auth";

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
  display: flex;
  justify-content: center;
  align-items: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
}

.status-card {
  width: min(100%, 420px);
  padding: 28px 24px;
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 251, 247, 0.97)),
    rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(240, 206, 193, 0.9);
  color: #4d3129;
  font-size: 18px;
  line-height: 1.55;
  text-align: center;
  box-shadow: 0 24px 50px rgba(104, 58, 42, 0.14);
  backdrop-filter: blur(18px);
}
</style>
