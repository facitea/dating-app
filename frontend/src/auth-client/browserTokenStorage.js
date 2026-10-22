// 브라우저 환경에서 access token을 저장하는 기본 저장소입니다.
export const TOKEN_KEY = "token";
export const SIGNUP_APPROVAL_NOTICE_KEY = "signupApprovalNoticeShown";

export const browserTokenStorage = {
  getAccessToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  setAuthTokens({ token }) {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    }
  },

  clear() {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(SIGNUP_APPROVAL_NOTICE_KEY);
  },
};
