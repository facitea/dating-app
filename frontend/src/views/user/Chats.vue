<template><section class="social-page"><h1>채팅</h1><p v-if="error" role="alert">{{error}} <button @click="load">다시 확인</button></p><p v-if="loading">불러오는 중…</p><p v-else-if="!rooms.length" class="social-empty">서로 좋아요를 보내거나<br>메시지를 보내 대화를 시작해 보세요.</p><article v-for="r in rooms" v-longpress="()=>actions.open(r.person,'chat',r.id)" tabindex="0" @keydown.enter.self="router.push(`/chats/${r.id}`)" @click="router.push(`/chats/${r.id}`)" :key="r.id" class="member-row room-link" ><AvatarLink :person="r.person"/><div class="member-copy"><button class="person-link" @click.stop="router.push(`/members/${r.person.id}`)">{{r.person.nickname}}</button><p>{{r.lastMessage || '매칭됐어요. 첫 인사를 건네보세요.'}}</p></div><span v-if="r.unread" class="unread">{{r.unread>99?'99+':r.unread}}</span></article><MemberActions ref="actions" @changed="load"/></section></template>
<script setup>
import AvatarLink from '../../components/AvatarLink.vue';
import {useRouter} from 'vue-router';import MemberActions from '../../components/MemberActions.vue';import {vLongpress} from '../../components/longPress';
import {ref,onMounted,onUnmounted} from 'vue';import api from '../../api/api';import '../../components/social.css';
const router=useRouter(),actions=ref();
const rooms=ref([]),error=ref(''),loading=ref(true);let timer,inflight=false,disposed=false;const photo=p=>p?new URL(p,api.defaults.baseURL).href:'';
async function load(){if(inflight||disposed)return;inflight=true;try{const {data}=await api.get('/social/rooms');if(!disposed){rooms.value=data;error.value='';}}catch{error.value='채팅 목록을 불러오지 못했어요.';}finally{loading.value=false;inflight=false;}}
onMounted(()=>{load();timer=setInterval(()=>{if(!document.hidden)load();},3000);});onUnmounted(()=>{disposed=true;clearInterval(timer);});
</script><style scoped>.room-link{text-decoration:none;color:inherit}.unread{background:#d56f4e;color:white;border-radius:20px;padding:4px 7px;font-size:11px}</style>
