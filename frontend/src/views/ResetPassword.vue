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
import api from "../api/api";

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
    const { data } = await api.post("/auth/password/reset/request", requestForm);
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
    const { data } = await api.post("/auth/password/reset/confirm", confirmForm);
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
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #fff8eb 0%, #ffd9bc 45%, #ffc9ae 100%);
}

.auth-card {
  width: min(100%, 480px);
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.94);
  box-shadow: 0 18px 45px rgba(109, 70, 36, 0.14);
}

.eyebrow {
  margin: 0 0 10px;
  color: #b35a28;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #35231a;
}

.description {
  margin: 12px 0 24px;
  color: #6a5246;
  line-height: 1.5;
}

.auth-form {
  display: grid;
  gap: 16px;
}

.confirm-form {
  margin-top: 24px;
}

label {
  display: grid;
  gap: 8px;
  color: #4d382d;
  font-weight: 600;
}

input {
  border: 1px solid #ebc7a9;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
}

.primary-button,
.secondary-button {
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  background: #cd6d2d;
  color: #fff;
}

.secondary-button {
  background: #fff;
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
  padding: 14px;
  border-radius: 14px;
  background: #fff6ed;
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
  margin-top: 20px;
  color: #6a5246;
}

.helper-text a {
  color: #a3461a;
  font-weight: 700;
}
</style>