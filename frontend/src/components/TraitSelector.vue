<template>
<section class="trait-picker" :aria-label="mode==='answers'?'나는 어떤 사람인가':'어떤 사람을 만나고 싶은가'">
  <section v-for="section in sections" :key="section" class="trait-category">
    <header><h2>{{section==='성격'?'성향':section}}</h2><span>{{count(section)}}개 선택 · 최대 {{limit(section)}}개</span></header>
    <div class="trait-keywords">
      <button v-if="mode==='preferences'" type="button" :aria-pressed="isAny(section)" @click="clearSection(section)">상관없음</button>
      <template v-for="entry in entries(section)" :key="entry.group.key+':'+entry.item">
        <span v-if="isCustom(entry)" class="custom-keyword">{{entry.item}} <span aria-hidden="true">{{hobbyEmoji(entry.item)}}</span><button class="remove-custom" type="button" :aria-label="entry.item+' 삭제'" @click="toggle(entry.group,entry.item)">×</button></span>
        <button v-else type="button" :aria-pressed="selected(entry.group).includes(entry.item)" :disabled="!selected(entry.group).includes(entry.item) && count(section)>=limit(section)" @click="toggle(entry.group,entry.item)">{{entry.item}}<span v-if="entry.group.key==='hobbies'" aria-hidden="true"> {{hobbyEmoji(entry.item)}}</span></button>
      </template>
    </div>
    <div v-if="section==='관심사·취미'" class="custom-interest"><input v-model="custom" maxlength="30" aria-label="관심사 직접 작성" placeholder="직접 작성" @keydown.enter.prevent="addCustom(section)"/><button type="button" @click="addCustom(section)">추가</button></div>
    <p v-if="notice.section===section" class="trait-note" role="status">{{notice.text}}</p>
    <p v-if="legacySimilar(section)" class="trait-note">이전에 저장한 유사 성향 조건은 키워드 또는 상관없음을 선택하면 변경됩니다.</p>
  </section>
</section>
</template>
<script setup>
import {computed,ref} from 'vue';
import {hobbyEmoji} from './hobbyEmoji';
const isCustom=entry=>entry.group.key==='hobbies'&&!entry.group.options.includes(entry.item);
import {catalog,parseTraits} from './traits';
const props=defineProps({modelValue:String,mode:{type:String,default:'answers'},scope:{type:String,default:'all'}});
const emit=defineEmits(['update:modelValue']);
// Short, balanced lists. Previously saved choices remain visible and removable.
const custom=ref('');const limit=section=>section==='관심사·취미'?10:3;
function addCustom(section){const value=custom.value.trim();if(!value)return;const group=groups(section)[0];if(!selected(group).includes(value))toggle(group,value);custom.value='';}
const featured={hobbies:true,
personality:['밝은','차분한','다정한','배려심 있는','외향적인','내향적인','유머 있는','솔직한','책임감 있는','독립적인','감성적인','현실적인','계획적인','즉흥적인','호기심 많은','감정 기복이 적은'],
talk_role:true,talk_mood:true,support:true,
pace:true,romance:true,balance:true,
words:['좋아한다고 자주 말하는','칭찬을 잘하는'],actions:['생활 속에서 챙겨주는','힘들 때 곁에 있어주는'],time:['둘만의 시간 중시','함께 취미를 즐기는'],touch:['스킨십을 좋아하는','천천히 맞춰가는'],anniversary:true,
contact_frequency:true,contact_method:['메시지 선호','전화 선호','직접 만나서 대화 선호'],busy:true,
planning:true,place:['실내 선호','야외 선호','집에서 쉬는 데이트','새로운 장소 탐방'],activities:['맛집','카페','산책','영화','운동','여행'],
conflict:true,discomfort:true,reconcile:true,
alone:true,partner_friends:true,privacy:true,autonomy:['각자 결정 존중','중요한 일은 함께 의논'],
rhythm:true,holiday:true,exercise:['꾸준히 운동','가벼운 산책 중심'],work_rest:['일에 몰입','일과 삶의 균형 중시','휴식 중시'],
marriage:true,children:true,money:true,
trust_behavior:['약속을 지키는','말과 행동이 같은'],respect_behavior:['상대의 속도를 존중하는','거절을 존중하는'],independence_behavior:['혼자 있는 시간을 존중하는'],initiative_behavior:['먼저 약속을 제안하는','결정을 함께하는'],conflict_behavior:['대화를 통해 해결하는','사과 뒤 행동을 바꾸는'],growth_behavior:['서로의 일을 응원하는'],longterm_behavior:['미래를 의논하는','책임을 나누는']
};
const allowed=computed(()=>catalog.filter(g=>props.scope==='hobbies'?g.key==='hobbies':props.scope==='personality'?g.key==='personality':props.scope==='more'?g.key!=='personality':true));
const sections=computed(()=>[...new Set(allowed.value.map(g=>g.section))]);
const data=computed(()=>parseTraits(props.modelValue));
const notice=ref({});
const groups=section=>allowed.value.filter(g=>g.section===section);
const rule=group=>data.value.preferences[group.key]||{mode:'ANY',values:[]};
const selected=group=>props.mode==='answers'?(data.value.answers[group.key]||[]):(rule(group).values||[]);
const entries=section=>groups(section).flatMap(group=>[...new Set([...group.options,...selected(group)])].filter(item=>featured[group.key]===true||featured[group.key]?.includes(item)||selected(group).includes(item)).map(item=>({group,item})));
const count=section=>groups(section).reduce((total,group)=>total+selected(group).length,0);
const isAny=section=>groups(section).every(group=>rule(group).mode==='ANY');
const legacySimilar=section=>props.mode==='preferences'&&groups(section).some(group=>rule(group).mode==='SIMILAR');
function clearSection(section){const d=parseTraits(props.modelValue);for(const group of groups(section))d.preferences[group.key]={mode:'ANY',values:[]};notice.value={};emit('update:modelValue',JSON.stringify(d));}
function toggle(group,item){const d=parseTraits(props.modelValue);let list=[...selected(group)];notice.value={};if(list.includes(item))list=list.filter(value=>value!==item);else if(count(group.section)<limit(group.section))list.push(item);else{notice.value={section:group.section,text:`최대 ${limit(group.section)}개까지 선택할 수 있어요.`};return;}if(props.mode==='answers')d.answers[group.key]=list;else{for(const other of groups(group.section))if(d.preferences[other.key]?.mode==='SIMILAR')d.preferences[other.key]={mode:'ANY',values:[]};d.preferences[group.key]={mode:list.length?'SPECIFIC':'ANY',values:list};}emit('update:modelValue',JSON.stringify(d));}
</script>
<style scoped>
.custom-keyword{position:relative;display:inline-flex;align-items:center;gap:4px;max-width:100%;padding:6px 28px 6px 9px;border:1px solid #b65c3f;border-radius:9px;background:#b65c3f;color:white;font-size:12px;line-height:1.4;overflow-wrap:anywhere}
.trait-keywords .remove-custom{position:absolute;right:-3px;top:-5px;display:grid;place-items:center;width:24px;height:24px;min-height:24px!important;padding:0;border:2px solid #fffaf6;border-radius:50%;background:#75422f;color:white;font-size:16px;line-height:1}

.custom-interest{display:flex;gap:8px;margin-top:10px}.custom-interest input{min-width:0;flex:1;padding:9px;border:1px solid #e5c8ba;border-radius:9px}.custom-interest button{border:0;border-radius:9px;background:#b65c3f;color:white;padding:8px 14px}
.trait-picker{display:block;min-width:0;color:#50382c}
.trait-category{padding:0 0 16px;margin:0 0 16px;border-bottom:1px solid #efdfd3}
.trait-category:last-child{padding-bottom:0;margin-bottom:0;border-bottom:0}
.trait-category header{display:flex;align-items:baseline;justify-content:space-between;gap:8px;margin:0 0 9px}
.trait-category h2{font-size:15px!important;font-weight:750;margin:0!important;color:#50382c}
.trait-category header>span{font-size:11px;color:#a56a50;white-space:nowrap}
.trait-keywords{display:flex;flex-wrap:wrap;gap:5px}
.trait-keywords button{font:inherit;font-size:12px;cursor:pointer;line-height:1.4;box-sizing:border-box;max-width:100%;white-space:normal;overflow-wrap:anywhere;text-align:left;min-height:34px;padding:6px 9px;border:1px solid #e5c8ba;background:#fffaf6;color:#705649;border-radius:9px}
.trait-keywords button[aria-pressed=true]{background:#b65c3f;border-color:#b65c3f;color:#fff}
.trait-keywords button:disabled{opacity:.45;cursor:default}
.trait-note{font-size:11px;line-height:1.5;color:#a65b40;margin:10px 0 0}
.trait-keywords button:focus-visible{outline:3px solid #f0ac89;outline-offset:2px}
</style>
