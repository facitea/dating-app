<template>
    <section class="compensation">
        <h3>보상 크레딧 주기</h3>
        <p>서비스 이용에 불편을 겪은 회원에게 메시지와 함께 지급합니다.</p>
        <form @submit.prevent="confirming = true"><label>지급 크레딧<input v-model.number="amount" type="number" min="1"
                    max="10000" required :disabled="busy || confirming" /></label><label>회원에게 전달할 메시지<textarea
                    v-model="message" rows="3" maxlength="1000" required :disabled="busy || confirming" /></label><button
                v-if="!confirming" class="outlined">지급 내용 확인</button></form>
        <div v-if="confirming" class="preview"><strong>{{ name }} 님에게 {{ amount }}크레딧 지급</strong>
            <p>{{ message }}</p><button :disabled="busy" @click="confirming = false">수정</button><button class="accent"
                :disabled="busy" @click="grant">{{ busy ? '지급 중…' : '확인하고 지급' }}</button>
        </div>
        <p role="status">{{ status }}</p>
        <p v-if="error" role="alert">{{ error }}</p>
    </section>
</template>
<script setup>
import { ref, watch } from 'vue'; import api from '../api/api'; import { requestId } from './requestId';
const props = defineProps({ userId: Number, name: String }); const amount = ref(10), message = ref('서비스 이용에 불편을 드려 죄송합니다. 감사와 사과의 마음을 담아 보상 크레딧을 드립니다.'), busy = ref(false), confirming = ref(false), error = ref(''), status = ref(''); let pending;
watch(() => props.userId, () => { pending = null; confirming.value = false; error.value = ''; status.value = ''; });
async function grant() { if (busy.value) return; busy.value = true; error.value = ''; if (!pending || pending.amount !== amount.value || pending.message !== message.value.trim()) pending = { amount: amount.value, message: message.value.trim(), requestId: requestId() }; try { const { data } = await api.post(`/admin/console/members/${props.userId}/compensation`, pending); status.value = `지급 완료 · 현재 잔액 ${data.balance}크레딧`; confirming.value = false; pending = null; } catch (e) { error.value = e.response?.data?.message || '지급 결과를 확인하지 못했습니다. 다시 시도해도 중복 지급되지 않습니다.'; } finally { busy.value = false; } }
</script>
<style
    scoped>
    .compensation {
        margin: 24px 0;
        padding: 20px;
        background: #fff7ef;
        border-radius: 14px
    }

    .compensation form {
        display: grid;
        gap: 12px
    }

    .compensation p {
        font-size: 12px;
        line-height: 1.7;
        white-space: pre-wrap
    }

    .preview {
        margin-top: 16px;
        padding: 16px;
        background: white;
        border-radius: 10px
    }

    .preview button {
        margin-right: 8px
    }
</style>
