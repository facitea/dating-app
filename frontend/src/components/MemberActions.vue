<template>
<dialog ref="modal" class="social-dialog action-sheet" @cancel="busy && $event.preventDefault()">
 <header><h2>{{mode==='report'?'상대 신고하기':person?.nickname}}</h2><button aria-label="닫기" :disabled="busy" @click="modal.close()">×</button></header>
 <template v-if="mode==='menu'">
  <button v-if="kind==='chat'" class="sheet-item" @click="mode='delete'">채팅 삭제</button>
  <button v-if="['match','matched'].includes(kind)" class="sheet-item" @click="mode='delete'">상대 삭제하기</button>
  <button v-if="['matched','conversation'].includes(kind)" class="sheet-item danger" @click="mode='end'">매칭 해제</button>
  <button v-if="kind==='chat'" class="sheet-item danger" @click="mode='block'">상대방 차단</button>
  <button v-if="['report','conversation'].includes(kind)" class="sheet-item danger" @click="mode='report'">상대 신고하기</button>
 </template>
 <form v-else @submit.prevent="submit">
  <template v-if="mode==='report'"><p>신고하면 서로의 추천·매칭·채팅에서 제외됩니다. 내용은 관리자가 확인합니다.</p><label>신고 사유<select v-model="reason"><option value="HARASSMENT">욕설·괴롭힘</option><option value="SCAM">금전 요구·사기 의심</option><option value="FAKE_PROFILE">도용·허위 프로필</option><option value="INAPPROPRIATE">부적절한 사진·메시지</option><option value="OTHER">기타</option></select></label><label>상세 내용<textarea v-model="detail" maxlength="2000" required placeholder="신고 내용을 작성해 주세요" /></label></template>
  <p v-else-if="mode==='end'">매칭을 해제할까요? 서로의 매칭과 대화가 종료되고 다시 메시지를 보낼 수 없습니다. 기존 크레딧은 반환되지 않습니다.</p><p v-else-if="mode==='block'">상대를 차단할까요? 서로의 추천·매칭·채팅에서 제외되며 메시지를 주고받을 수 없습니다.</p>
  <p v-else>{{kind==='chat'?'내 채팅 목록과 기존 대화가 삭제됩니다. 상대방의 대화는 유지되며, 새 메시지가 오면 목록에 다시 나타납니다.':'내 매칭현황에서 상대를 삭제합니다. 채팅과 상대방의 목록은 유지됩니다.'}}</p>
  <button class="primary" :disabled="busy || (mode==='report' && !detail.trim())">{{busy?'처리 중…':mode==='report'?'신고 접수':mode==='block'?'차단하기':mode==='end'?'매칭 해제':'삭제하기'}}</button>
 </form><p v-if="error" role="alert">{{error}}</p>
</dialog></template>
<script setup>
import {ref} from 'vue';import api from '../api/api';import {refreshBadges} from './notificationEvents';import './social.css';
const emit=defineEmits(['changed']);const modal=ref(),person=ref(),mode=ref('menu'),kind=ref(),reason=ref('HARASSMENT'),detail=ref(''),error=ref(''),busy=ref(false);let room;
function open(p,type,roomId){person.value=p;kind.value=type;room=roomId;mode.value='menu';error.value='';detail.value='';reason.value='HARASSMENT';modal.value.showModal();}
async function submit(){if(busy.value)return;busy.value=true;error.value='';try{
 if(mode.value==='report')await api.post('/safety/reports',{reportedUserId:person.value.id,reasonCode:reason.value,detail:detail.value.trim()});
 else if(mode.value==='end')await api.post(`/social/matches/${person.value.id}/end`);
 else if(mode.value==='block')await api.post(`/social/blocks/${person.value.id}`);
 else await api.delete(kind.value==='chat'?`/social/rooms/${room}`:`/social/matches/${person.value.id}`);
 modal.value.close();refreshBadges();emit('changed',mode.value);
}catch(e){error.value=e.response?.data?.message||'처리하지 못했어요. 다시 시도해 주세요.';}finally{busy.value=false;}}
defineExpose({open});
</script><style scoped>.action-sheet{margin:auto auto 16px}.sheet-item{display:block;width:100%;text-align:left;margin-top:12px}.danger{color:#ad372b!important}</style>
