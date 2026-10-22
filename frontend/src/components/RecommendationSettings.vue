<template>
  <button v-if="!initial" class="filter-trigger" @click="open">추천조건</button>
  <component :is="initial ? 'section' : 'dialog'" ref="modal" class="preference-dialog" :class="initial ? 'initial-preferences' : 'social-dialog'">
    <form @submit.prevent="save">
      <header><h2>{{ initial ? '추천조건을 설정해 주세요' : '추천조건' }}</h2><button v-if="!initial" type="button" aria-label="닫기" :disabled="busy" @click="modal.close()">×</button></header>
      <p v-if="loading" role="status">추천조건을 불러오는 중…</p>
      <template v-if="ready">
        <p v-if="firstSetup" class="muted">저장한 조건으로 첫 인연을 준비할게요. 원하는 조건이 없으면 상관없음을 선택해 주세요.</p><p v-else class="muted">변경한 조건은 {{ effective }}부터 적용돼요.</p>
        <fieldset :disabled="busy">
          <div class="age-setting">
            <div class="age-heading"><strong>희망 나이대</strong><label class="age-any"><input type="checkbox" :checked="ageAny" @change="toggleAgeAny($event.target.checked)"/>상관없음</label></div>
            <template v-if="!ageAny">
              <div class="age-range-values"><span>{{ form.birthYearMax == null ? '하한 없음' : `${form.birthYearMax}년생 (${youngest}세)` }}</span><span aria-hidden="true">—</span><span>{{ form.birthYearMin == null ? '상한 없음' : `${form.birthYearMin}년생 (${oldest}세)` }}</span></div>
              <div class="age-range" :style="{ '--range-start': `${(youngest-18)/(maximumAge-18)*100}%`, '--range-end': `${(oldest-18)/(maximumAge-18)*100}%` }">
                <div class="age-range-track" aria-hidden="true"></div>
                <input class="age-slider age-lower" :class="{'at-end':youngest===maximumAge}" type="range" :value="youngest" min="18" :max="maximumAge" step="1" aria-label="최소 나이" :aria-valuetext="`${referenceYear-youngest}년생, ${youngest}세`" @input="setYoungest($event.target.value); $event.target.value=youngest"/>
                <input class="age-slider age-upper" type="range" :value="oldest" min="18" :max="maximumAge" step="1" aria-label="최대 나이" :aria-valuetext="`${referenceYear-oldest}년생, ${oldest}세`" @input="setOldest($event.target.value); $event.target.value=oldest"/>
              </div>
              <p class="age-note muted">{{ referenceYear }}년 기준 연나이 · 1세 단위로 조절할 수 있어요.</p>
            </template>
            <p v-else class="age-note muted">나이 제한 없이 추천받습니다.</p>
          </div>
          <div class="radius-setting">
            <label for="recommendation-radius">지역범위 <strong>{{ radius === 505 ? '상관없음' : `반경 ${radius}km` }}</strong></label>
            <div class="age-range radius-range" :style="{ '--range-start': '0%', '--range-end': `${(radius-5)/500*100}%` }">
              <div class="age-range-track" aria-hidden="true"></div>
              <input id="recommendation-radius" v-model.number="radius" class="radius-slider" type="range" min="5" max="505" step="5" :aria-valuetext="radius === 505 ? '상관없음' : `반경 ${radius}킬로미터`" />
            </div>
            <div class="radius-labels"><span>5km</span><span>250km</span><span>상관없음</span></div>
            <p class="muted">{{ region || '프로필의 등록 지역' }} 중심점 기준 거리예요.</p>
            <p v-if="!radiusAvailable" class="muted">반경을 지정하려면 프로필의 거주 지역을 먼저 선택해 주세요.</p>
            <p v-if="legacyRegion" class="muted">기존 조건은 {{ legacyRegion }}입니다. 저장하면 선택한 반경으로 바뀝니다.</p>
          </div>
          <label v-for="field in fields" :key="field.key">{{ field.label }}<select v-model="form[field.key]"><option value="ANY">상관없음</option><option v-for="o in field.options" :key="o[0]" :value="o[0]">{{ o[1] }}</option></select></label>
        </fieldset>
      </template>
      <p v-if="error" role="alert">{{ error }}</p>
      <button v-if="!ready && !loading" type="button" @click="load">다시 불러오기</button>
      <p v-if="saved" role="status">저장했어요. 현재 카드는 그대로 유지됩니다.</p>
      <button v-if="ready" class="primary" :disabled="busy">{{ busy ? '저장 중…' : firstSetup ? '저장하고 인연 보기' : '다음 회차에 적용' }}</button>
    </form>
  </component>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '../api/api';
import './social.css';
const props=defineProps({initial:Boolean});
const emit=defineEmits(['saved']);
const firstSetup=ref(false);
onMounted(()=>{if(props.initial)load();});
const modal=ref(),form=ref({}),at=ref(),busy=ref(false),ready=ref(false),loading=ref(false),error=ref(''),saved=ref(false);
const referenceYear=ref(2026),radius=ref(505),region=ref(''),radiusAvailable=ref(false),legacyRegion=ref('');
const maximumAge=computed(()=>Math.max(100, referenceYear.value-(form.value.birthYearMin??referenceYear.value), referenceYear.value-(form.value.birthYearMax??referenceYear.value)));
const ageAny=computed(()=>form.value.birthYearMin==null&&form.value.birthYearMax==null);
const youngest=computed(()=>form.value.birthYearMax==null?18:referenceYear.value-form.value.birthYearMax);
const oldest=computed(()=>form.value.birthYearMin==null?maximumAge.value:referenceYear.value-form.value.birthYearMin);
function toggleAgeAny(any) {
  form.value.birthYearMin=any?null:referenceYear.value-35;
  form.value.birthYearMax=any?null:referenceYear.value-24;
}
function setYoungest(value) { form.value.birthYearMax=referenceYear.value-Math.min(oldest.value,Math.max(18,Number(value))); }
function setOldest(value) { form.value.birthYearMin=referenceYear.value-Math.max(youngest.value,Math.min(maximumAge.value,Number(value))); }
const effective=computed(()=>at.value?new Date(at.value).toLocaleString('ko-KR',{timeZone:'Asia/Seoul',month:'long',day:'numeric',hour:'numeric',minute:'2-digit'}):'다음 회차');
const fields=[{key:'mbti',label:'희망 MBTI',options:'INTJ INTP ENTJ ENTP INFJ INFP ENFJ ENFP ISTJ ISFJ ESTJ ESFJ ISTP ISFP ESTP ESFP'.split(' ').map(x=>[x,x])},{key:'smoking',label:'흡연여부',options:[['NON_SMOKER','비흡연'],['SMOKER','흡연'],['OCCASIONAL','가끔 흡연']]},{key:'drinking',label:'음주여부',options:[['NONE','안 마심'],['SOMETIMES','가끔'],['OFTEN','자주']]},{key:'religion',label:'종교',options:[['NONE','무교'],['CHRISTIAN','기독교'],['CATHOLIC','천주교'],['BUDDHIST','불교'],['OTHER','기타']]}];
function failure(e,fallback){
  if(e.response?.status===404)return '추천조건 API가 없습니다. 백엔드 서버를 최신 코드로 재시작해 주세요.';
  if(!e.response)return '서버에 연결하지 못했어요. 백엔드 실행 상태를 확인해 주세요.';
  return e.response.data?.message||fallback;
}
async function open(){if(!modal.value.open)modal.value.showModal();await load();}
async function load(){
  if(loading.value)return;
  loading.value=true;ready.value=false;error.value='';saved.value=false;
  try{
    const {data}=await api.get('/social/preferences');
    if(!Number.isInteger(data.referenceYear)){error.value='출생연도·거리 설정을 사용하려면 백엔드 서버를 최신 코드로 재시작해 주세요.';return;}
    firstSetup.value=data.configured===false;referenceYear.value=data.referenceYear;region.value=data.region;radiusAvailable.value=data.radiusAvailable;
    const c=data.pending||data.active;
    form.value={...c,birthYearMin:c.birthYearMin ?? (c.ageMax==null?null:data.referenceYear-c.ageMax),birthYearMax:c.birthYearMax ?? (c.ageMin==null?null:data.referenceYear-c.ageMin)};
    radius.value=c.radiusKm??505;
    legacyRegion.value=c.radiusKm==null?({CITY:'같은 시·도',DISTRICT:'같은 시·군·구'}[c.regionScope]||''):'';
    at.value=data.effectiveAt||data.nextRoundAt;ready.value=true;
  }catch(e){error.value=failure(e,'추천조건을 불러오지 못했어요.');}finally{loading.value=false;}
}
async function save(){
  if(busy.value||!ready.value)return;error.value='';saved.value=false;
  if(form.value.birthYearMin!=null&&form.value.birthYearMax!=null&&form.value.birthYearMin>form.value.birthYearMax){error.value='시작 출생연도는 마지막 출생연도보다 앞선 연도를 선택해 주세요.';return;}
  if(radius.value!==505&&!radiusAvailable.value){error.value='프로필의 거주 지역을 먼저 선택해 주세요.';return;}
  busy.value=true;
  try{const {data}=await api.put('/social/preferences',{...form.value,ageMin:null,ageMax:null,regionScope:'ANY',radiusKm:radius.value===505?null:radius.value});at.value=data.effectiveAt;legacyRegion.value='';saved.value=true;const wasFirst=firstSetup.value;firstSetup.value=false;emit('saved',data);if(wasFirst&&!props.initial)modal.value.close();}
  catch(e){error.value=failure(e,'추천조건을 저장하지 못했어요.');}finally{busy.value=false;}
}
</script>
<style scoped>
.initial-preferences { padding: 12px 0 28px; color: #50382c; }
.initial-preferences h2 { margin: 0 0 12px; }
.initial-preferences .muted { color: #806b60; font-size: 13px; line-height: 1.7; }
.initial-preferences label { display: grid; gap: 8px; margin: 16px 0; font-size: 14px; }
.initial-preferences select { min-height: 48px; border: 1px solid #ead6c9; background: white; color: #50382c; }
.initial-preferences .primary { width: 100%; min-height: 52px; border: 0; border-radius: 14px; background: #b65c3f; color: white; font-weight: 700; margin-top: 20px; }

.preference-dialog fieldset{border:0;margin:0;padding:0;min-width:0}
.age-setting{margin:20px 0;padding:16px;background:#fff2ea;border-radius:16px}
.age-heading{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:16px;font-size:14px}
.preference-dialog .age-any{display:flex;align-items:center;gap:6px;margin:0;min-height:44px;font-size:12px;cursor:pointer}
.age-any input{appearance:auto;width:18px;height:18px;margin:0;padding:0;accent-color:#d56f4e}
.age-range-values{display:flex;align-items:center;justify-content:space-between;gap:4px;color:#a55137;font-size:12px;font-weight:700}
.age-range{position:relative;height:44px;margin:4px 0}
.age-range-track{position:absolute;left:10px;right:10px;top:19px;height:6px;border-radius:8px;background:linear-gradient(to right,#e9d5c9 var(--range-start),#d56f4e var(--range-start),#d56f4e var(--range-end),#e9d5c9 var(--range-end))}
.preference-dialog :is(.age-slider,.radius-slider){position:absolute;inset:0;appearance:none!important;-webkit-appearance:none!important;width:100%!important;height:44px;min-height:44px;padding:0!important;margin:0;border:0!important;border-radius:0!important;background:transparent;box-shadow:none;pointer-events:none;outline-offset:2px}
:is(.age-slider,.radius-slider)::-webkit-slider-runnable-track{height:6px;background:transparent;border:0}
:is(.age-slider,.radius-slider)::-moz-range-track{height:6px;background:transparent;border:0}
:is(.age-slider,.radius-slider)::-webkit-slider-thumb{appearance:none;-webkit-appearance:none;pointer-events:auto;width:20px;height:20px;margin-top:-7px;border-radius:50%;border:0;background:#d56f4e;box-shadow:none;cursor:grab;touch-action:none}
:is(.age-slider,.radius-slider)::-moz-range-thumb{pointer-events:auto;width:20px;height:20px;border-radius:50%;border:0;background:#d56f4e;box-shadow:none;cursor:grab;touch-action:none}
.age-lower{z-index:2}.age-upper{z-index:3}.age-lower.at-end,.age-slider:focus-visible{z-index:4}
.age-slider:active::-webkit-slider-thumb{cursor:grabbing}
.age-setting .age-note{font-size:11px;margin-bottom:0;line-height:1.6}
.age-note{margin-top:0}.radius-setting{margin:20px 0;padding:16px;background:#fff2ea;border-radius:16px}
.radius-setting label{display:flex;justify-content:space-between;align-items:center;margin:0 0 8px}
.radius-setting strong{color:#b75f42;font-size:15px}
.preference-dialog .radius-slider{pointer-events:auto;cursor:pointer}
.preference-dialog .radius-setting label{font-size:14px;font-weight:700}
.radius-setting strong,.age-range-values{color:#b75f42;font-size:13px}
.radius-labels{display:flex;justify-content:space-between;font-size:11px;color:#947568}.radius-setting p{margin-bottom:0;font-size:11px}
</style>
