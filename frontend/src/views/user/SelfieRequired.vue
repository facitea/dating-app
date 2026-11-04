<template>
<section class="selfie-gate">
 <main class="selfie-content"><span class="brand">그럼우리</span><h1>{{pending?'셀피 확인 중이에요':'셀피 인증'}}</h1><p class="description">{{pending?'확인이 완료되면 자동으로 이동해요.':'본인 확인을 위해 셀피를 촬영해 주세요.'}}</p><p v-if="status?.state==='RETRY' && status.note" class="retry-note">{{status.note}}</p></main>
 <footer class="selfie-actions"><p v-if="error" class="error" role="alert">{{error}}</p><button class="authenticate" :disabled="!loaded||busy||pending" @click="capture.open()">{{pending?'확인 중':'인증하기'}}</button><button class="refresh" :disabled="busy" @click="load()">{{busy?'확인 중…':'새로고침'}}</button></footer>
 <SelfieCaptureDialog ref="capture" required @submitted="load()"/>
</section>
</template>
<script setup>
import {ref,computed,onMounted,onUnmounted} from 'vue';import {useRouter} from 'vue-router';import api from '../../api/api';import SelfieCaptureDialog from '../../components/SelfieCaptureDialog.vue';
const router=useRouter(),status=ref(),capture=ref(),busy=ref(false),loaded=ref(false),error=ref('');const pending=computed(()=>status.value?.state==='PENDING');let timer,disposed=false,loading=false;
async function load(silent=false){if(loading)return;loading=true;if(!silent)busy.value=true;try{const [r,s]=await Promise.all([api.get('/selfie/request'),api.get('/selfie')]);if(disposed)return;status.value=s.data;loaded.value=true;error.value='';if(!r.data.required){const u=(await api.get('/user/me')).data;if(!disposed)router.replace(u.status==='ACTIVE'?'/today':'/review-pending');}}catch(e){if(!disposed&&!silent)error.value=e.response?.data?.message||'상태를 확인하지 못했어요. 다시 시도해 주세요.';}finally{loading=false;busy.value=false;}}
onMounted(()=>{load();timer=setInterval(()=>{if(!document.hidden)load(true);},5000);});onUnmounted(()=>{disposed=true;clearInterval(timer);});
</script>
<style scoped>
.selfie-gate{box-sizing:border-box;max-width:480px;min-height:100dvh;margin:0 auto;padding:calc(28px + env(safe-area-inset-top)) 24px calc(18px + env(safe-area-inset-bottom));display:flex;flex-direction:column;background:#fffaf7;color:#40332b;font-family:inherit}.selfie-content{flex:1;padding-bottom:32px}.brand{display:block;font-size:15px;font-weight:650;color:#986c57;margin-bottom:clamp(64px,14dvh,120px)}.selfie-content h1{font-size:28px;line-height:1.35;letter-spacing:-1px;font-weight:700;margin:0 0 14px}.description{font-size:14px;line-height:1.7;color:#937f73;margin:0}.retry-note{margin:24px 0 0;padding:14px 16px;background:#f8ece5;border-radius:12px;font-size:13px;line-height:1.7;white-space:pre-wrap;overflow-wrap:anywhere}.selfie-actions{display:grid;gap:8px}.selfie-actions button{box-sizing:border-box;width:100%;min-height:52px;padding:14px 20px;border:0;border-radius:14px;font:inherit;font-size:15px;font-weight:600;cursor:pointer}.authenticate{background:#d56f4e;color:white}.authenticate:disabled{background:#eddbd0;color:#a68c7d;cursor:default}.refresh{background:transparent;color:#99725e}.refresh:disabled{opacity:.5;cursor:default}.selfie-actions button:focus-visible{outline:2px solid #ac5439;outline-offset:3px}.error{font-size:13px;line-height:1.6;color:#b34d39;margin:0 0 8px}
</style>
