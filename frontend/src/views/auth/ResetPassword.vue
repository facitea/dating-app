<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Reset Password</p>
      <h1>비밀번호 재설정</h1>
      <p class="description">
        먼저 재설정 토큰을 발급받고, 새 비밀번호를 입력해 변경할 수 있습니다.
      </p>

      <form class="auth-form" @submit.prevent="requestReset">
        <label>
          <span>이메일</span>
          <input v-model.trim="requestForm.email" type="email" placeholder="you@example.com" required />
        </label>
        <button class="secondary-button" :disabled="requestLoading">
          {{ requestLoading ? "발급 중..." : "재설정 토큰 발급" }}
        </button>
      </form>

      <p v-if="requestMessage" class="message success">{{ requestMessage }}</p>
      <p v-if="requestError" class="message error">{{ requestError }}</p>

      <div v-if="issuedToken" class="token-box">
        <strong>개발용 재설정 토큰</strong>
        <code>{{ issuedToken }}</code>
      </div>

      <form class="auth-form confirm-form" @submit.prevent="confirmReset">
        <label>
          <span>재설정 토큰</span>
          <input v-model.trim="confirmForm.token" type="text" placeholder="발급받은 토큰" required />
        </label>

        <label>
          <span>새 비밀번호</span>
          <input v-model="confirmForm.newPassword" type="password" placeholder="8자 이상 입력" required minlength="8" />
        </label>

        <button class="primary-button" :disabled="confirmLoading">
          {{ confirmLoading ? "변경 중..." : "비밀번호 변경" }}
        </button>
      </form>

      <p v-if="confirmMessage" class="message success">{{ confirmMessage }}</p>
      <p v-if="confirmError" class="message error">{{ confirmError }}</p>

      <p class="helper-text">
        로그인 화면으로 돌아가기
        <RouterLink to="/">로그인</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { authClient } from "../../auth-client";

const router = useRouter();
const requestLoading = ref(false);
const confirmLoading = ref(false);
const requestMessage = ref("");
const requestError = ref("");
const confirmMessage = ref("");
const confirmError = ref("");
const issuedToken = ref("");

const requestForm = reactive({
  email: "",
});

const confirmForm = reactive({
  token: "",
  newPassword: "",
});

const requestReset = async () => {
  requestLoading.value = true;
  requestMessage.value = "";
  requestError.value = "";
  issuedToken.value = "";

  try {
    const data = await authClient.requestPasswordReset(requestForm.email);
    requestMessage.value = data.message;
    issuedToken.value = data.resetToken || "";
    confirmForm.token = data.resetToken || "";
  } catch (error) {
    requestError.value = error.response?.data?.message || "재설정 토큰 발급에 실패했습니다.";
  } finally {
    requestLoading.value = false;
  }
};

const confirmReset = async () => {
  confirmLoading.value = true;
  confirmMessage.value = "";
  confirmError.value = "";

  try {
    const data = await authClient.confirmPasswordReset(confirmForm.token, confirmForm.newPassword);
    confirmMessage.value = data.message;
    setTimeout(() => router.push("/"), 800);
  } catch (error) {
    confirmError.value = error.response?.data?.message || "비밀번호 변경에 실패했습니다.";
  } finally {
    confirmLoading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
}

.auth-card {
  width: min(100%, 480px);
  min-height: min(860px, calc(100vh - 36px));
  margin: auto 0;
  padding: 28px 22px 24px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 251, 247, 0.97)),
    rgba(255, 252, 247, 0.94);
  border: 1px solid rgba(240, 210, 193, 0.92);
  box-shadow:
    0 26px 52px rgba(109, 70, 36, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.62);
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
  font-size: clamp(1.9rem, 7vw, 2.4rem);
  line-height: 1.08;
}

.description {
  margin: 12px 0 24px;
  color: #6a5246;
  line-height: 1.62;
}

.auth-form {
  display: grid;
  gap: 14px;
}

.confirm-form {
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid rgba(236, 206, 193, 0.9);
}

label {
  display: grid;
  gap: 8px;
  color: #4d382d;
  font-weight: 600;
}

input {
  border: 1px solid #ebc7a9;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 2px rgba(77, 56, 45, 0.04);
}

.primary-button,
.secondary-button {
  width: 100%;
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
  box-shadow: 0 16px 28px rgba(205, 109, 45, 0.24);
}

.secondary-button {
  background: rgba(255, 255, 255, 0.92);
  color: #57372d;
  border: 1px solid #ebc7a9;
}

.message {
  margin-top: 14px;
}

.success {
  color: #1f7a43;
}

.error {
  color: #b72f2f;
}

.token-box {
  margin-top: 14px;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fff6ed 0%, #fff2e8 100%);
  border: 1px solid #efd6c1;
}

.token-box strong {
  display: block;
  margin-bottom: 8px;
  color: #422921;
}

.token-box code {
  display: block;
  overflow-wrap: anywhere;
  color: #8a3f24;
}

.helper-text {
  margin-top: auto;
  color: #6a5246;
  text-align: center;
}

.helper-text a {
  color: #a3461a;
  font-weight: 700;
}

@media (min-width: 768px) {
  .auth-page {
    padding: 28px;
  }

  .auth-card {
    min-height: auto;
    padding: 36px 34px 28px;
  }
}

@media (max-width: 420px) {
  .auth-card {
    border-radius: 28px;
    padding: 24px 18px 20px;
  }
}
</style>
