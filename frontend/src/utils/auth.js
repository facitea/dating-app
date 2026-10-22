import { SIGNUP_APPROVAL_NOTICE_KEY, TOKEN_KEY, browserTokenStorage } from "../auth-client/browserTokenStorage";

export { SIGNUP_APPROVAL_NOTICE_KEY, TOKEN_KEY };

export const getToken = () => browserTokenStorage.getAccessToken();

export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const setAuthTokens = (tokens) => browserTokenStorage.setAuthTokens(tokens);

export const clearToken = () => { const token=getToken(); if(token)import('../components/push').then(m=>m.detachPush(token));browserTokenStorage.clear(); };

export const hasToken = () => Boolean(getToken());

export const resetSignupApprovalNotice = () => {
  sessionStorage.removeItem(SIGNUP_APPROVAL_NOTICE_KEY);
};

export const hasShownSignupApprovalNotice = () =>
  sessionStorage.getItem(SIGNUP_APPROVAL_NOTICE_KEY) === "true";

export const markSignupApprovalNoticeShown = () => {
  sessionStorage.setItem(SIGNUP_APPROVAL_NOTICE_KEY, "true");
};
