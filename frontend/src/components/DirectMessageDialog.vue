<template>
  <dialog ref="modal" class="social-dialog">
    <form @submit.prevent="send"><header><h2>메시지 보내기</h2><button type="button" aria-label="닫기" :disabled="busy" @click="modal.close()">×</button></header>
      <template v-if="quote"><p>{{ quote.cost }}크레딧으로 대화방을 열어요. 이후 대화는 무료입니다.</p><p class="muted">보유 {{ quote.balance }}크레딧</p><label>첫 메시지<textarea v-model="text" maxlength="2000" required placeholder="메시지를 입력하세요" :disabled="busy" /></label><button class="primary" :disabled="busy || !text.trim() || quote.balance < quote.cost">{{ busy ? '보내는 중…' : `${quote.cost}크레딧 · 보내기` }}</button><p v-if="quote.balance < quote.cost">크레딧이 부족합니다. 충전 기능은 준비 중입니다.</p></template>
      <p v-else-if="!error">불러오는 중…</p><p v-if="error" role="alert">{{ error }}</p>
    </form>
  </dialog>
</template>
<script setup>
import {requestId} from "./requestId";
import {ref} from 'vue';import {useRouter} from 'vue-router';import api from '../api/api';import './social.css';
const modal=ref(),quote=ref(),text=ref(''),error=ref(''),busy=ref(false),router=useRouter();let target,clientId;
async function open(id){target=id;quote.value=null;text.value='';error.value='';clientId=requestId();modal.value.showModal();try{const {data}=await api.get(`/social/contact/${id}`);if(data.roomId){modal.value.close();router.push(`/chats/${data.roomId}`);}else quote.value=data;}catch(e){error.value=e.response?.data?.message||'대화 비용을 확인하지 못했어요.';}}
async function send(){if(busy.value)return;busy.value=true;error.value='';try{const {data}=await api.post('/social/direct',{targetId:target,text:text.value,clientId,expectedCost:quote.value.cost});modal.value.close();router.push(`/chats/${data.roomId}`);}catch(e){error.value=e.response?.data?.message||'전송 결과를 확인하지 못했어요. 다시 보내도 중복 차감되지 않습니다.';}finally{busy.value=false;}}
defineExpose({open});
</script>
