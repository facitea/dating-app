import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup, setPersistence, inMemoryPersistence, signOut } from 'firebase/auth';
import { ref } from 'vue';
import api from '../api/api';
const config={apiKey:import.meta.env.VITE_FIREBASE_API_KEY,authDomain:import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,projectId:import.meta.env.VITE_FIREBASE_PROJECT_ID,appId:import.meta.env.VITE_FIREBASE_APP_ID};
export const googleConfigured=Object.values(config).every(Boolean);
const auth=googleConfigured?getAuth(initializeApp(config)):null;
const persistence=auth?setPersistence(auth,inMemoryPersistence):Promise.resolve();
export const pendingGoogle=ref(null);
export async function beginGoogle() {
  if(!auth)throw new Error('구글 로그인을 준비 중입니다. 이메일 로그인을 이용해 주세요.');
  await persistence;
  const provider=new GoogleAuthProvider();provider.setCustomParameters({prompt:'select_account'});
  const {user}=await signInWithPopup(auth,provider);
  pendingGoogle.value={user,email:user.email};
  return exchangeGoogle();
}
export async function googleToken() {
  if(!pendingGoogle.value)throw new Error('구글 인증을 다시 진행해 주세요.');
  return pendingGoogle.value.user.getIdToken(true);
}
export async function exchangeGoogle(existingPassword) {
  const {data}=await api.post('/auth/google',{idToken:await googleToken(),existingPassword});
  if(data.email&&pendingGoogle.value)pendingGoogle.value.email=data.email;
  return data;
}
export async function clearGoogle() {pendingGoogle.value=null;if(auth)await signOut(auth);}
export function googleError(e) {
  const messages={'auth/popup-closed-by-user':'구글 로그인이 취소됐어요.','auth/cancelled-popup-request':'이미 구글 로그인이 진행 중이에요.','auth/popup-blocked':'팝업이 차단됐어요. 브라우저에서 팝업을 허용해 주세요.','auth/unauthorized-domain':'이 접속 주소에는 구글 로그인이 설정되지 않았어요.','auth/network-request-failed':'네트워크 연결을 확인해 주세요.'};
  return e.response?.data?.message||messages[e.code]||(!e.code?e.message:null)||'구글 로그인에 실패했어요. 다시 시도해 주세요.';
}
