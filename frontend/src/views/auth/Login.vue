<template>
  <section class="welcome-page">
    <div class="welcome-atmosphere" aria-hidden="true"><div class="color-wave wave-one"></div><div class="color-wave wave-two"></div></div>
    <div class="welcome-brand">
      <h1 class="welcome-wordmark" aria-label="그럼우리">
        <span class="wordmark-first" aria-hidden="true">그럼</span><span class="wordmark-last" aria-hidden="true">우리</span>
      </h1>
    </div>

    <div class="welcome-actions">
      <button class="welcome-enter" @click="openLogin">이메일로 로그인 <span aria-hidden="true">↗</span></button>
      <RouterLink class="welcome-signup" to="/signup">회원가입</RouterLink>
    </div>
    <dialog ref="loginDialog" class="login-sheet" @click="closeOnBackdrop">
      <header class="sheet-header"><h2>로그인</h2><button type="button" aria-label="닫기" @click="loginDialog.close()">×</button></header>
      <form class="login-fields" aria-label="이메일 로그인" @submit.prevent="login">
        <label class="sr-only" for="login-email">이메일</label>
        <input id="login-email" ref="emailInput" v-model.trim="form.email" type="email"
          autocomplete="username" inputmode="email" autocapitalize="none" :spellcheck="false"
          placeholder="이메일" :aria-describedby="errorMessage ? 'login-error' : undefined" required />
        <label class="sr-only" for="login-password">비밀번호</label>
        <input id="login-password" ref="passwordInput" v-model="form.password" type="password"
          autocomplete="current-password" placeholder="비밀번호"
          :aria-describedby="errorMessage ? 'login-error' : undefined" required />
        <p v-if="errorMessage" id="login-error" class="login-error" role="alert">{{ errorMessage }}</p>
        <button class="login-submit" :disabled="loading">{{ loading ? '로그인 중…' : '로그인' }}</button>
      </form>
      <nav class="login-links" aria-label="계정 메뉴">
        <RouterLink to="/reset-password">비밀번호 찾기</RouterLink>
        <span aria-hidden="true">·</span>
        <RouterLink to="/signup">회원가입</RouterLink>
      </nav>
    </dialog>
  </section>
</template>

<script setup>
import { nextTick, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { authClient } from "../../auth-client";
import { clearToken, setAuthTokens } from "../../utils/auth";

const router = useRouter();
const loading = ref(false);
const loginDialog = ref(null);
function openLogin() { loginDialog.value.showModal(); }
function closeOnBackdrop(event) {
  if (event.target !== loginDialog.value) return;
  const box = loginDialog.value.getBoundingClientRect();
  if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) loginDialog.value.close();
}
const errorMessage = ref("");
const emailInput = ref(null);
const passwordInput = ref(null);
const form = reactive({ email: "", password: "" });

function moveAfterLogin(data) {
  setAuthTokens(data);
  router.push(data.user?.status === "ACTIVE" ? "/today" : "/review-pending");
}

function resolveLoginErrorMessage(error) {
  const status = error.response?.status;
  const serverMessage = error.response?.data?.message;

  if (serverMessage) return serverMessage;
  if (status === 400 || status === 401) return "이메일 또는 비밀번호를 다시 확인해 주세요.";
  if (status === 403) return "현재 로그인할 수 없는 계정 상태입니다. 운영 정책 또는 심사 상태를 확인해 주세요.";
  return "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
}

function resolveLoginFocusField(error, message) {
  const serverFocusField = error.response?.data?.focusField;

  if (serverFocusField === "email") return "email";
  if (serverFocusField === "password") return "password";
  if (serverFocusField === "phone") return "email";

  if (message.includes("이메일") && !message.includes("비밀번호")) {
    return "email";
  }

  return "password";
}

async function focusLoginField(field) {
  await nextTick();

  if (field === "email") {
    emailInput.value?.focus();
    emailInput.value?.select?.();
    return;
  }

  passwordInput.value?.focus();
  passwordInput.value?.select?.();
}

function getLoginValidationError() {
  if (!form.email.trim()) {
    return {
      field: "email",
      message: "이메일을 입력해 주세요.",
    };
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    return {
      field: "email",
      message: "올바른 이메일 형식으로 입력해 주세요.",
    };
  }

  if (!form.password) {
    return {
      field: "password",
      message: "비밀번호를 입력해 주세요.",
    };
  }

  if (form.password.length < 8) {
    return {
      field: "password",
      message: "비밀번호는 8자 이상 입력해 주세요.",
    };
  }

  return null;
}

const login = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const validationError = getLoginValidationError();
    if (validationError) {
      errorMessage.value = validationError.message;
      await focusLoginField(validationError.field);
      return;
    }

    const data = await authClient.login(form);
    moveAfterLogin(data);
  } catch (error) {
    clearToken();
    const message = resolveLoginErrorMessage(error);
    errorMessage.value = message;

    await focusLoginField(resolveLoginFocusField(error, message));
  } finally {
    loading.value = false;
  }
};


</script>

<style scoped>
.welcome-page {
  position: relative; isolation: isolate; min-height: 100vh; min-height: 100dvh;
  display: flex; flex-direction: column; padding: max(24px, env(safe-area-inset-top)) 26px max(20px, env(safe-area-inset-bottom));
  background: #ef886b; color: #4c281e;
}
.welcome-atmosphere { position: absolute; inset: 0; overflow: hidden; z-index: -1; pointer-events: none; background: linear-gradient(160deg, #ffd8c3 0%, #f7ae8e 38%, #ee8166 100%); }
.color-wave { position: absolute; width: 180%; height: 110%; border-radius: 46%; }
.wave-one { top: -74%; left: -25%; background: linear-gradient(130deg, #fff2e4, #ffdbc5); animation: wave-drift 16s ease-in-out infinite alternate; }
.wave-two { bottom: -92%; left: -36%; background: linear-gradient(130deg, rgba(255, 215, 187, .6), rgba(239, 124, 95, .1)); animation: wave-drift 20s ease-in-out -6s infinite alternate-reverse; }
.welcome-brand { flex: 1; min-height: 300px; display: grid; place-items: center; padding-bottom: 26px; }
.welcome-page .welcome-wordmark {
  display: flex; margin: 0; color: #512b21; font-size: clamp(56px, 16vw, 76px) !important;
  font-weight: 900; letter-spacing: -.075em; line-height: 1.15; white-space: nowrap;
  animation: wordmark-rise 1s cubic-bezier(.22, 1, .36, 1) both;
}
.wordmark-first { animation: join-left .75s cubic-bezier(.65, 0, .35, 1) .45s both; }
.wordmark-last { animation: join-right .75s cubic-bezier(.65, 0, .35, 1) .45s both; }
.welcome-actions { display: grid; width: 100%; gap: 6px; }
.welcome-enter { display: flex; align-items: center; justify-content: space-between; padding: 0 24px; min-height: 58px; width: 100%; border: 0; border-radius: 999px; background: #512b21; color: #fff5ed; font-size: 15px; font-weight: 700; cursor: pointer; }
.welcome-enter span { font-size: 24px; font-weight: 400; }
.welcome-signup { display: grid; place-items: center; min-height: 48px; font-size: 14px; font-weight: 600; text-decoration: none; }
.login-sheet { position: fixed; inset: auto 0 0; margin: 0 auto; width: min(100%, 480px); max-width: 100%; max-height: 90dvh; overflow-y: auto; border: 0; border-radius: 28px 28px 0 0; padding: 26px 24px max(22px, env(safe-area-inset-bottom)); color: #512b21; background: #fffaf6; box-shadow: 0 -12px 60px #512b2120; }
.login-sheet[open] { animation: sheet-rise .25s ease-out; }
.login-sheet::backdrop { background: rgba(53, 28, 20, .32); }
.sheet-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 22px; }
.sheet-header h2 { margin: 0; font-weight: 800; }
.sheet-header button { border: 0; background: transparent; color: inherit; width: 44px; font-size: 28px; cursor: pointer; }
.login-fields { display: grid; gap: 10px; }
.login-fields input { min-height: 52px; border: 1px solid #ead9cf; background: #fff; color: #33211d; }
.login-fields input::placeholder { color: #8a7267; }
.login-submit { min-height: 54px; margin-top: 8px; border: 0; border-radius: 14px; background: #a95238; color: white; font: inherit; font-size: 15px; font-weight: 700; cursor: pointer; }
.login-submit:disabled { opacity: .65; cursor: wait; }
.login-links { display: flex; align-items: center; justify-content: center; gap: 18px; margin-top: 10px; }
.login-links a { display: inline-flex; align-items: center; min-height: 44px; color: #77584b; font-size: 13px; text-decoration: none; }
.login-links > span { color: #c2a99a; }
.login-error { margin: 2px 0; color: #9b2920; font-size: 13px; line-height: 1.5; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip-path: inset(50%); white-space: nowrap; border: 0; }
@keyframes wave-drift { from { transform: translate(-3%, 0) rotate(-16deg); } to { transform: translate(6%, 7%) rotate(12deg); } }
@keyframes wordmark-rise { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
@keyframes join-left { from { transform: translateX(-.16em); } to { transform: translateX(0); } }
@keyframes join-right { from { transform: translateX(.16em); } to { transform: translateX(0); } }
@keyframes sheet-rise { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0); } }
@media (max-height: 500px) { .welcome-brand { min-height: 200px; } }
@media (prefers-reduced-motion: reduce) { .welcome-page .welcome-wordmark, .wordmark-first, .wordmark-last, .color-wave, .login-sheet[open] { animation: none; } }
</style>
