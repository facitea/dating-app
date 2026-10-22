<template>
  <section class="complete-page">
    <div class="complete-card">
      <p class="eyebrow">Welcome In</p>
      <h1>가입 승인이 완료되었어요</h1>
      <p class="description">
        사진 심사가 승인되어 이제 소개팅 앱의 모든 기능을 이용할 수 있습니다.
        프로필을 다시 확인하고 바로 홈으로 이동해보세요.
      </p>

      <div class="summary-box">
        <strong>현재 상태</strong>
        <p>운영자 심사가 승인되었고 가입 절차가 모두 끝났습니다.</p>
      </div>

      <div class="action-row">
        <button class="primary-button" type="button" @click="goHome">홈으로 이동</button>
        <button class="secondary-button" type="button" @click="logout">로그아웃</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const router = useRouter();

onMounted(async () => {
  try {
    const { data } = await api.get("/user/me");
    if (data.status !== "ACTIVE") {
      router.replace("/review-pending");
    }
  } catch (error) {
    clearToken();
    router.replace("/");
  }
});

const goHome = () => {
  router.push("/home");
};

const logout = () => {
  clearToken();
  router.replace("/");
};
</script>

<style scoped>
.complete-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
}

.complete-card {
  width: min(100%, 560px);
  margin: auto 0;
  padding: 30px 22px 24px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 251, 247, 0.97)),
    rgba(255, 251, 247, 0.95);
  border: 1px solid rgba(240, 210, 193, 0.92);
  box-shadow: 0 24px 50px rgba(109, 70, 36, 0.14);
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 10px;
  color: #b35a28;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #35231a;
  font-size: clamp(2rem, 7vw, 2.5rem);
  line-height: 1.08;
}

.description {
  margin: 12px 0 24px;
  color: #6a5246;
  line-height: 1.6;
}

.summary-box {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fff7ed 0%, #fff3ea 100%);
  border: 1px solid #f0d8c4;
  color: #6a5246;
}

.summary-box strong {
  display: block;
  margin-bottom: 8px;
  color: #3b281f;
}

.summary-box p {
  margin: 0;
  line-height: 1.5;
}

.action-row {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  flex-direction: column;
}

.primary-button,
.secondary-button {
  flex: 1;
  border: none;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  background: linear-gradient(135deg, #cd6d2d 0%, #ec9456 100%);
  color: #fff;
}

.secondary-button {
  background: #fff;
  color: #4d3129;
  border: 1px solid #e6c1b4;
}

@media (min-width: 768px) {
  .complete-page {
    padding: 28px;
  }

  .complete-card {
    padding: 36px 34px 30px;
  }

  .action-row {
    flex-direction: row;
  }
}
</style>
