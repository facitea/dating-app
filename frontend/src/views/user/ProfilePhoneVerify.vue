<template>
  <section class="profile-page">
    <div class="profile-shell">
      <header class="page-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Phone Verify</p>
            <h1>전화번호 인증</h1>
            <p class="description">
              로그인 후에도 전화번호를 따로 등록하거나 변경할 수 있습니다. 인증이 끝나면 내 계정에 바로 저장됩니다.
            </p>
          </div>
          <button class="ghost-button" type="button" @click="goBack">돌아가기</button>
        </div>
      </header>

      <section class="page-card">
        <div class="status-card">
          <p class="status-label">현재 등록 상태</p>
          <strong>{{ currentPhoneLabel }}</strong>
          <span>{{ currentPhoneHint }}</span>
        </div>

        <form class="phone-form" @submit.prevent="verifyPhone">
          <label>
            <span>전화번호</span>
            <input
              ref="phoneInput"
              v-model.trim="phone"
              type="tel"
              inputmode="numeric"
              maxlength="11"
              placeholder="01012345678"
            />
          </label>

          <button class="ghost-button action-button" type="button" :disabled="requesting" @click="requestCode">
            {{ requesting ? "인증번호 요청 중..." : "인증번호 요청" }}
          </button>

          <p v-if="expiresText" class="helper-text">{{ expiresText }}</p>
          <p v-if="devCode" class="helper-text">개발용 인증번호: {{ devCode }}</p>

          <label>
            <span>인증번호</span>
            <input
              ref="codeInput"
              v-model.trim="code"
              type="text"
              inputmode="numeric"
              maxlength="6"
              placeholder="6자리 숫자"
            />
          </label>

          <button class="primary-button" :disabled="verifying">
            {{ verifying ? "인증 확인 중..." : "전화번호 인증 완료" }}
          </button>
        </form>

        <p v-if="successMessage" class="message success">{{ successMessage }}</p>
        <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { authClient } from "../../auth-client";
import { clearToken } from "../../utils/auth";

const router = useRouter();
const phoneInput = ref(null);
const codeInput = ref(null);

const currentPhone = ref("");
const phone = ref("");
const code = ref("");
const devCode = ref("");
const expiresText = ref("");
const requesting = ref(false);
const verifying = ref(false);
const successMessage = ref("");
const errorMessage = ref("");

const currentPhoneLabel = computed(() => currentPhone.value || "아직 등록된 전화번호가 없습니다.");
const currentPhoneHint = computed(() =>
  currentPhone.value
    ? "새 번호를 인증하면 기존 번호 대신 현재 계정에 저장됩니다."
    : "인증을 완료하면 이 계정의 전화번호가 새로 등록됩니다.",
);

function normalizePhone(value) {
  return value.replaceAll(/[^0-9]/g, "");
}

function resolveErrorMessage(error, fallbackMessage) {
  return error.response?.data?.message || fallbackMessage;
}

async function loadProfile() {
  try {
    const { data } = await api.get("/profile/me");
    currentPhone.value = data.phone || "";
    phone.value = data.phone || "";
  } catch (error) {
    clearToken();
    router.replace("/");
  }
}

function focusField(field) {
  if (field === "code") {
    codeInput.value?.focus();
    codeInput.value?.select?.();
    return;
  }

  phoneInput.value?.focus();
  phoneInput.value?.select?.();
}

async function requestCode() {
  successMessage.value = "";
  errorMessage.value = "";
  devCode.value = "";
  expiresText.value = "";

  phone.value = normalizePhone(phone.value);
  if (!phone.value) {
    errorMessage.value = "전화번호를 입력해 주세요.";
    focusField("phone");
    return;
  }

  requesting.value = true;

  try {
    const response = await authClient.requestProfilePhoneCode(phone.value);
    successMessage.value = response.message || "인증번호를 요청했습니다.";
    devCode.value = response.devCode || "";
    expiresText.value = response.expiresAt
      ? `인증 만료 시각: ${new Date(response.expiresAt).toLocaleString("ko-KR")}`
      : "";
    await nextTick();
    focusField("code");
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, "인증번호 요청에 실패했습니다.");
    focusField(error.response?.data?.focusField || "phone");
  } finally {
    requesting.value = false;
  }
}

async function verifyPhone() {
  successMessage.value = "";
  errorMessage.value = "";

  phone.value = normalizePhone(phone.value);
  code.value = normalizePhone(code.value);

  if (!phone.value) {
    errorMessage.value = "전화번호를 입력해 주세요.";
    focusField("phone");
    return;
  }

  if (!code.value) {
    errorMessage.value = "인증번호를 입력해 주세요.";
    focusField("code");
    return;
  }

  verifying.value = true;

  try {
    const response = await authClient.verifyProfilePhone(phone.value, code.value);
    successMessage.value = response.message || "전화번호 인증이 완료되었습니다.";
    currentPhone.value = phone.value;
    setTimeout(() => router.push("/profile"), 500);
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error, "전화번호 인증에 실패했습니다.");
    focusField(error.response?.data?.focusField || "phone");
  } finally {
    verifying.value = false;
  }
}

function goBack() {
  router.push("/profile");
}

onMounted(async () => {
  await loadProfile();
  await nextTick();
  focusField("phone");
});
</script>

<style scoped>
.profile-page { min-height: 100vh; padding: max(16px, env(safe-area-inset-top)) 16px 24px; display: flex; justify-content: center; }
.profile-shell { width: min(100%, 720px); display: grid; gap: 16px; }
.page-card {
  padding: 24px 20px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
}
.eyebrow { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1 { margin: 0; color: #31211d; }
.description { margin: 12px 0 0; color: #6f564d; line-height: 1.7; }
.section-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.status-card {
  display: grid;
  gap: 6px;
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 247, 242, 0.9);
  color: #5d4640;
}
.status-card strong { color: #2f211d; font-size: 18px; }
.status-label { margin: 0; color: #af5f42; font-size: 12px; font-weight: 800; letter-spacing: 0.08em; }
.phone-form { display: grid; gap: 14px; }
label { display: grid; gap: 8px; color: #543932; font-weight: 600; }
input {
  border: 1px solid #e5c2b2;
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.92);
  font-family: inherit;
}
.helper-text { margin: -2px 4px 2px; color: #8b675b; font-size: 13px; }
.primary-button, .ghost-button {
  border-radius: 999px;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.primary-button {
  border: none;
  background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%);
  color: #fff;
}
.ghost-button {
  border: 1px solid rgba(207, 150, 128, 0.36);
  background: rgba(255, 248, 243, 0.82);
  color: #6a4137;
}
.action-button { justify-self: start; }
.message { margin-top: 14px; font-weight: 700; }
.success { color: #1f7a43; }
.error { color: #b72f2f; }
@media (max-width: 760px) {
  .section-head { flex-direction: column; align-items: flex-start; }
}
</style>
