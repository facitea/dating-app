<template>
<section v-if="rows.length" class="trait-summary"><h2>{{title}}</h2>
<p v-if="compare && comparisonStatus==='ready'" class="match-note">{{matchCount ? `${mode==='preferences'?'나와 일치하는':'내 이상형'} 키워드 ${matchCount}개 · 색이 채워진 항목을 확인해 보세요.` : (mode==='preferences'?'상대의 이상형 중 나와 일치하는 키워드는 없어요.':'현재 이상형 조건과 일치하는 키워드는 없어요.')}}</p>
<p v-else-if="compare" class="match-note">{{comparisonStatus==='error'?'이상형 조건을 불러오지 못해 비교하지 못했어요.':comparisonStatus==='empty'?(mode==='preferences'?'내 성향을 입력하면 상대 이상형과 비교할 수 있어요.':'추천조건에서 이상형을 선택하면 일치 여부를 볼 수 있어요.'):'이상형 조건과 비교하고 있어요…'}}</p>
<div v-for="row in rows" :key="row.label"><h3>{{row.label==='성격'?'성향':row.label}}</h3><div class="summary-tags"><span v-for="entry in row.values" :key="entry.key" :class="{shared:entry.shared}">{{entry.value}}<small v-if="entry.shared">{{mode==='preferences'?'나와 일치':'내 이상형'}}</small></span></div></div>
</section>
<section v-else-if="compare" class="trait-summary"><h2>{{title}}</h2><p class="match-note">아직 등록한 성향 키워드가 없어요.</p></section>
</template>
<script setup>
import {computed} from 'vue';import {catalog,parseTraits} from './traits';
const props=defineProps({json:String,mode:{type:String,default:'answers'},title:{type:String,default:'나는 어떤 사람인가'},compare:Boolean,comparisonJson:String,comparisonStatus:{type:String,default:'ready'}});
const rows=computed(()=>{const d=parseTraits(props.json),mine=parseTraits(props.comparisonJson);const sections=new Map();for(const g of catalog){const values=props.mode==='answers'?(d.answers[g.key]||[]):d.preferences[g.key]?.mode==='SIMILAR'?(d.answers[g.key]||[]):d.preferences[g.key]?.mode==='SPECIFIC'?(d.preferences[g.key].values||[]):[];if(!values.length)continue;if(!sections.has(g.section))sections.set(g.section,[]);for(const value of values)sections.get(g.section).push({key:g.key+':'+value,value,shared:props.compare&&props.comparisonStatus==='ready'&&(props.mode==='preferences'?(mine.answers[g.key]||[]):mine.preferences[g.key]?.mode==='SPECIFIC'?(mine.preferences[g.key].values||[]):mine.preferences[g.key]?.mode==='SIMILAR'?(mine.answers[g.key]||[]):[]).includes(value)});}return [...sections].map(([label,values])=>({label,values}));});
const matchCount=computed(()=>rows.value.reduce((total,row)=>total+row.values.filter(v=>v.shared).length,0));
</script>
<style scoped>
.trait-summary{padding:18px 0;border-top:1px solid #f1e7e1}.trait-summary h2{font-size:15px!important;margin:0 0 12px}.trait-summary h3{font-size:12px;color:#927565;margin:16px 0 8px}.summary-tags{display:flex;flex-wrap:wrap;gap:5px}.summary-tags>span{border:1px solid #ecd7ca;border-radius:9px;background:#fff8f3;color:#805d4c;padding:6px 9px;font-size:12px;line-height:1.4;overflow-wrap:anywhere;max-width:100%;box-sizing:border-box}.summary-tags>span.shared{background:#b65c3f;border-color:#b65c3f;color:#fff}.summary-tags small{display:block;font-size:10px;margin-top:3px}.match-note{font-size:12px;color:#927565;line-height:1.6;margin:0 0 12px}
</style>
