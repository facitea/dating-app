import api from "../api/api";
import { browserTokenStorage } from "./browserTokenStorage";

function resolveResponseData(response) {
  return response?.data ?? response;
}

// 자체 인증서비스 API를 다른 화면이나 JS 프로젝트에서도 재사용하기 위한 클라이언트입니다.
export function createAuthClient({
  httpClient = api,
  storage = browserTokenStorage,
  clientType = "WEB",
} = {}) {
  const authHeaders = {
    "X-Client-Type": clientType,
  };

  async function post(path, payload = {}) {
    const response = await httpClient.post(path, payload, { headers: authHeaders });
    return resolveResponseData(response);
  }

  function persistAuthResponse(data) {
    storage.setAuthTokens?.(data);
    return data;
  }

  return {
    login(credentials) {
      return post("/auth/login", credentials).then(persistAuthResponse);
    },

    signup(signupForm) {
      return post("/auth/signup", signupForm).then(persistAuthResponse);
    },

    requestEmailCode(email, purpose = "SIGNUP") {
      return post("/auth/verification/request", {
        targetType: "EMAIL",
        targetValue: email,
        purpose,
      });
    },

    confirmEmailCode(email, code, purpose = "SIGNUP") {
      return post("/auth/verification/confirm", {
        targetType: "EMAIL",
        targetValue: email,
        purpose,
        code,
      });
    },

    requestPhoneCode(phone) {
      return post("/auth/phone/request", { phone });
    },

    verifyPhoneAndLogin(phone, code) {
      return post("/auth/phone/verify", { phone, code }).then(persistAuthResponse);
    },

    requestProfilePhoneCode(phone) {
      return post("/profile/phone/request", { phone });
    },

    verifyProfilePhone(phone, code) {
      return post("/profile/phone/verify", { phone, code });
    },

    requestPasswordReset(email) {
      return post("/auth/password/reset/request", { email });
    },

    confirmPasswordReset(token, newPassword) {
      return post("/auth/password/reset/confirm", { token, newPassword });
    },

    refresh() {
      return post("/auth/refresh").then(persistAuthResponse);
    },

    clear() {
      storage.clear?.();
    },
  };
}

export const authClient = createAuthClient();
