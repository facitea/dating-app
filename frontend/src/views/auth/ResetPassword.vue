<template><section class="auth-page"><div class="auth-card"><h1>비밀번호 재설정</h1><p class="description">Firebase 이메일 계정으로 재설정 메일을 보내요. 받은 링크에서 새 비밀번호를 설정해 주세요.</p><form class="auth-form" @submit.prevent="send"><label><span>이메일</span><input v-model.trim="email" type="email" autocomplete="email" required/></label><button class="secondary-button" :disabled="busy">{{busy?'전송 중…':'재설정 메일 보내기'}}</button></form><p v-if="message" class="message success" role="status">{{message}}</p><p v-if="error" class="message error" role="alert">{{error}}</p><p class="description">Google 계정은 Google 로그인을 이용해 주세요. 기존 자체 이메일 계정은 기존 비밀번호로 로그인할 수 있으며, 해당 계정의 비밀번호를 잊었다면 고객센터로 문의해 주세요.</p><RouterLink to="/">로그인 화면으로</RouterLink></div></section></template>
<script setup>import {ref} from 'vue';import {resetEmailPassword,emailError} from '../../auth-client/emailAuth';const email=ref(''),busy=ref(false),message=ref(''),error=ref('');async function send(){if(busy.value)return;busy.value=true;message.value=error.value='';try{await resetEmailPassword(email.value);message.value='재설정 가능한 계정이라면 메일이 발송됩니다. 받은 편지함과 스팸함을 확인해 주세요.';}catch(e){if(e.code==='auth/user-not-found')message.value='재설정 가능한 계정이라면 메일이 발송됩니다.';else error.value=emailError(e);}finally{busy.value=false;}}</script>
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
