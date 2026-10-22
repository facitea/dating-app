<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Dating App</p>
      <h1>로그인</h1>
      <p class="description">이메일 계정으로 로그인해 서비스를 시작해보세요.</p>

      <form class="auth-form" @submit.prevent="login">
        <label>
          <span>이메일</span>
          <input ref="emailInput" v-model.trim="form.email" type="email" placeholder="you@example.com" required />
        </label>

        <label>
          <span>비밀번호</span>
          <input ref="passwordInput" v-model="form.password" type="password" placeholder="8자 이상 입력" required />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? '로그인 중...' : '이메일로 로그인' }}
        </button>
      </form>

      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="helper-row">
        <RouterLink to="/reset-password">비밀번호를 잊으셨나요?</RouterLink>
        <RouterLink to="/signup">회원가입</RouterLink>
      </div>

      <div class="policy-box">
        <strong>안내</strong>
        <p>사진 심사 대기나 반려 상태의 계정은 로그인 후 심사 대기 화면에서 다시 등록 절차를 이어갈 수 있습니다.</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../api/api";
import { setToken } from "../utils/auth";

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const emailInput = ref(null);
const passwordInput = ref(null);
const form = reactive({
  email: "",
  password: "",
});

const login = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const { data } = await api.post("/auth/login", form);
    setToken(data.token);

    if (data.user?.status === "ACTIVE") {
      router.push("/home");
      return;
    }

    router.push("/review-pending");
  } catch (error) {
    const status = error.response?.status;
    const message =
      error.response?.data?.message ||
      (status === 401
        ? "이메일 또는 비밀번호를 다시 확인해주세요."
        : "로그인에 실패했습니다. 잠시 후 다시 시도해주세요.");

    errorMessage.value = message;
    window.alert(message);

    await nextTick();
    if (!form.email) {
      emailInput.value?.focus();
      return;
    }

    passwordInput.value?.focus();
    passwordInput.value?.select?.();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #fff3e8 0%, #ffd8c2 45%, #ffc2b3 100%);
}

.auth-card {
  width: min(100%, 440px);
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 252, 250, 0.92);
  box-shadow: 0 18px 45px rgba(98, 49, 34, 0.14);
}

.eyebrow {
  margin: 0 0 10px;
  color: #9d4b32;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #33211d;
}

.description {
  margin: 12px 0 24px;
  color: #6e534c;
  line-height: 1.5;
}

.auth-form {
  display: grid;
  gap: 16px;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input {
  border: 1px solid #e6c1b4;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
}

.primary-button {
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  margin-top: 8px;
  background: #d45d38;
  color: #fff;
}

.message {
  margin-top: 16px;
}

.error {
  color: #b72f2f;
}

.helper-row {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.helper-row a {
  color: #a54123;
  font-weight: 700;
}

.policy-box {
  margin-top: 22px;
  padding: 16px;
  border-radius: 16px;
  background: #fff6f1;
  border: 1px solid #f0d0c3;
  color: #6a4a41;
}

.policy-box strong {
  display: block;
  margin-bottom: 8px;
  color: #3d251f;
}

.policy-box p {
  margin: 0;
  line-height: 1.5;
}
</style>