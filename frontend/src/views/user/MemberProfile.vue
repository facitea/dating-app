<template>
 <section class="public-profile">
  <header class="profile-header"><button class="icon-button" aria-label="뒤로" @click="router.back()"><svg viewBox="0 0 24 24"><path d="m14 5-7 7 7 7"/></svg></button><span>프로필</span><button class="icon-button more" v-if="data" aria-label="상대 메뉴" @click="actions.open(data.person,'report')">⋯</button><span v-else class="header-space"/></header>
  <p v-if="error" class="profile-state" role="alert">{{error}}</p>
  <template v-if="data">
   <div class="photo-section"><div class="profile-photos" @scroll.passive="photoScroll" aria-label="프로필 사진"><img v-for="(url,i) in data.images" :key="url" :src="photo(url)" :alt="`${data.person.nickname} 프로필 사진 ${i+1}`" :loading="i?'lazy':'eager'"/><div v-if="!data.images.length" class="empty-photo">♡</div></div><span v-if="data.images.length>1" class="photo-count">{{activePhoto+1}} / {{data.images.length}}</span></div>
   <p v-if="data.totalImages>data.images.length" class="photo-lock">내 사진 {{data.ownImageCount}}장으로 상대 사진 {{data.images.length}}장을 볼 수 있어요. <RouterLink to="/profile/photos">사진 추가하기</RouterLink></p><div class="profile-body">
    <section class="identity"><h1>{{data.person.nickname}}<span>{{data.person.age}}</span></h1><p><svg viewBox="0 0 24 24"><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2.5"/></svg>{{data.person.region}}</p></section>
    <section v-if="data.introduction" class="profile-section intro-section"><h2>자기소개</h2><p>{{data.introduction}}</p></section>
    <section v-if="facts.length" class="profile-section"><h2>기본 정보</h2><dl class="profile-facts"><div v-for="[label,value] in facts" :key="label"><dt>{{label}}</dt><dd>{{value}}</dd></div></dl></section>
    <section v-if="data.personality" class="profile-section"><h2>나의 성격</h2><p>{{data.personality}}</p></section>
    <section v-if="data.idealType" class="profile-section"><h2>이런 분을 만나고 싶어요</h2><p>{{data.idealType}}</p></section>
    <div class="profile-contact"><button @click="direct.open(data.person.id)"><svg viewBox="0 0 24 24"><path d="M20 11.5a8 8 0 0 1-8 8H5l-3 3v-11a9 9 0 0 1 18 0Z"/></svg>메시지 보내기</button></div>
   </div>
  </template><p v-else-if="!error" class="profile-state" role="status">불러오는 중…</p>
  <DirectMessageDialog ref="direct"/><MemberActions ref="actions" @changed="removed"/>
 </section>
</template>
<script setup>
import {ref,watch,computed} from 'vue';import {useRoute,useRouter} from 'vue-router';import api from '../../api/api';import DirectMessageDialog from '../../components/DirectMessageDialog.vue';import MemberActions from '../../components/MemberActions.vue';import '../../components/social.css';
const activePhoto=ref(0);
const route=useRoute(),router=useRouter(),data=ref(),error=ref(''),direct=ref(),actions=ref();const photo=p=>new URL(p,api.defaults.baseURL).href;
const labels={OCCASIONAL:'가끔 흡연',NON_SMOKER:'비흡연',SMOKER:'흡연',SOMETIMES:'가끔',OFTEN:'자주 음주',SOCIAL:'가끔 음주',NONE:'없음',CHRISTIAN:'기독교',CHRISTIANITY:'기독교',CATHOLIC:'천주교',BUDDHIST:'불교',BUDDHISM:'불교',OTHER:'기타'};
let sequence=0;watch(()=>route.params.id,async id=>{const current=++sequence;data.value=null;activePhoto.value=0;error.value='';try{const res=await api.get(`/social/profiles/${id}`);if(current===sequence)data.value=res.data;}catch(e){if(current===sequence)error.value=e.response?.data?.message||'프로필을 불러오지 못했어요.';}},{immediate:true});
const facts=computed(()=>data.value?[
 ['직업',data.value.job],['MBTI',data.value.mbti],['흡연',labels[data.value.smoking]],
 ['음주',data.value.drinking==='NONE'?'안 마심':labels[data.value.drinking]],
 ['종교',data.value.religion==='NONE'?'무교':labels[data.value.religion]]
].filter(([,value])=>value):[]);
function photoScroll(e){activePhoto.value=Math.round(e.target.scrollLeft/e.target.clientWidth);}
function removed(){router.replace('/today');}
</script>
<style scoped>
.public-profile{max-width:480px;min-height:100dvh;margin:auto;background:#fff;color:#252321;padding:0 0 12px;box-sizing:border-box}
.profile-header{height:58px;display:flex;align-items:center;justify-content:space-between;padding:0 12px;background:#fff}.profile-header>span{font-size:15px;font-weight:600}.icon-button{width:44px;height:44px;border:0;background:transparent;color:#302925;display:grid;place-items:center;cursor:pointer;border-radius:50%}.icon-button svg{width:22px;height:22px;fill:none;stroke:currentColor;stroke-width:1.8}.more{font-size:27px}.header-space{width:44px}
.photo-section{position:relative}.profile-photos{display:flex;overflow-x:auto;scroll-snap-type:x mandatory;scrollbar-width:none;overscroll-behavior-x:contain}.profile-photos::-webkit-scrollbar{display:none}.profile-photos img{flex:0 0 100%;width:100%;min-width:0;aspect-ratio:1/1.08;object-fit:cover;object-position:center 30%;scroll-snap-align:start;scroll-snap-stop:always}.empty-photo{width:100%;aspect-ratio:1/1.08;display:grid;place-items:center;background:#f6f1ed;font-size:54px;color:#b78c77}.photo-count{position:absolute;right:18px;bottom:18px;background:rgba(30,25,22,.48);color:white;border-radius:20px;padding:6px 12px;font-size:11px;font-variant-numeric:tabular-nums}
.profile-body{padding:0 24px}.identity{padding:27px 0 24px}.identity h1{font-size:26px!important;font-weight:750;letter-spacing:-.8px;margin:0;line-height:1.35;overflow-wrap:anywhere}.identity h1 span{font-weight:400;margin-left:10px;color:#746d68;font-size:22px}.identity p{display:flex;align-items:center;gap:5px;color:#8c837d;font-size:13px;margin:10px 0 0}.identity svg{width:15px;height:15px;fill:none;stroke:currentColor;stroke-width:1.6}
.profile-section{padding:24px 0;border-top:1px solid #f0eeec}.profile-section h2{font-size:14px!important;letter-spacing:-.2px;margin:0 0 13px;font-weight:700;color:#302925}.profile-section p{font-size:14px;line-height:1.9;margin:0;color:#68615c;white-space:pre-wrap;overflow-wrap:anywhere}.profile-facts{display:grid;grid-template-columns:1fr 1fr;gap:21px 16px;margin:0}.profile-facts div{min-width:0}.profile-facts dt{font-size:11px;color:#9d948e;margin-bottom:6px}.profile-facts dd{font-size:14px;font-weight:500;margin:0;overflow-wrap:anywhere}.profile-contact{padding:10px 0 20px}.profile-contact button{display:flex;align-items:center;justify-content:center;gap:9px;border:0;width:100%;min-height:52px;border-radius:15px;background:#d56f4e;color:#fff;font:inherit;font-size:14px;font-weight:650;cursor:pointer}.profile-contact svg{width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:1.6}.profile-state{padding:48px 24px;text-align:center;font-size:14px;color:#8c837d}.icon-button:focus-visible,.profile-contact button:focus-visible{outline:2px solid #92553f;outline-offset:2px}
@media(max-width:350px){.profile-body{padding:0 20px}.identity h1{font-size:23px!important}}
.photo-lock{margin:0;padding:15px 24px;background:#faf7f4;color:#958276;font-size:12px;line-height:1.8}.photo-lock a{color:#c96c49;white-space:nowrap}</style>
