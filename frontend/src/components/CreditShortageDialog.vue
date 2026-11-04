<template>
  <dialog ref="modal" class="credit-shortage" aria-labelledby="credit-shortage-title" aria-describedby="credit-shortage-question" @cancel="close" @close="creditPromptOpen=false">
    <h2 id="credit-shortage-title">크레딧이 부족해요</h2>
    <p id="credit-shortage-question">크레딧 상점으로 이동할까요?</p>
    <div class="credit-shortage-actions"><button type="button" @click="close">아니오</button><button type="button" class="credit-shortage-yes" @click="goToStore">네</button></div>
  </dialog>
</template>
<script setup>
import {ref,watch,nextTick} from 'vue';
import {useRouter,useRoute} from 'vue-router';
import {creditPromptOpen} from './creditPrompt';
const modal=ref(),router=useRouter(),route=useRoute();
function close(){creditPromptOpen.value=false;modal.value?.close();}
function goToStore(){close();router.push('/credits');}
watch(creditPromptOpen,async open=>{await nextTick();if(open&&!modal.value?.open)modal.value?.showModal();else if(!open)modal.value?.close();});
watch(()=>route.fullPath,close);
</script>
<style scoped>
.credit-shortage{box-sizing:border-box;width:calc(100% - 40px);max-width:360px;border:0;border-radius:20px;padding:24px;background:#fffaf7;color:#40332b;box-shadow:0 16px 60px #38281d26}
.credit-shortage::backdrop{background:#25191180}.credit-shortage h2{font-size:18px;margin:0 0 12px}.credit-shortage p{font-size:14px;line-height:1.6;margin:0 0 24px;color:#765f50}
.credit-shortage-actions{display:flex;gap:10px}.credit-shortage-actions button{flex:1;min-height:46px;border:0;border-radius:12px;background:#eee5de;color:#58473b;font:inherit;cursor:pointer}.credit-shortage-actions .credit-shortage-yes{background:#d56f4e;color:white}
</style>
