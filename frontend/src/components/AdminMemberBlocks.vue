<template>
    <section class="member-blocks">
        <header>
            <h3>차단·신고한 회원 <span v-if="loaded">{{ data.total }}명</span></h3><button type="button" :disabled="busy"
                @click="load">{{ busy ? '조회 중…' : loaded ? '새로고침' : '목록 확인' }}</button>
        </header>
        <p>이 회원이 직접 차단했거나 신고한 상대입니다.</p>
        <p v-if="error" role="alert">{{ error }}</p><template v-if="loaded">
            <p v-if="!data.items.length">차단·신고한 회원이 없습니다.</p>
            <article v-for="u in data.items" :key="u.id">
                <div><button class="member-name" type="button" @click="$emit('member', u.id)">{{ u.nickname }}
                        <small>#{{ u.id }}</small></button><span v-if="u.status === 'DELETED'"> · 탈퇴 회원</span>
                    <div class="tags"><span v-if="u.directlyBlocked">직접 차단</span><span v-if="u.reported">신고로 접촉
                            제한</span></div>
                </div>
                <div><button type="button" @click="$emit('member', u.id)">회원 보기</button> <button class="release"
                        type="button" :disabled="releasing" @click="target = u; reason = ''; releaseError = ''">차단 해제</button>
                </div>
            </article>
            <footer v-if="data.total > 30"><button :disabled="busy || page === 0"
                    @click="page--; load()">이전</button>{{ page + 1 }}<button :disabled="busy || (page + 1) * 30 >= data.total"
                    @click="page++; load()">다음</button></footer>
        </template>
        <form v-if="target" class="release-confirm" @submit.prevent="release"><strong>{{ target.nickname }} 님에 대한 차단을
                해제할까요?</strong>
            <p>이 회원이 설정한 직접 차단과 신고에 따른 접촉 제한을 해제합니다. 신고 기록·처리 상태는 유지됩니다. 상대가 설정한 차단이나 계정 정지는 해제되지 않습니다.</p><label>민원 처리
                사유<input v-model="reason" maxlength="200" required :disabled="releasing" /></label>
            <div><button type="button" :disabled="releasing" @click="target = null">취소</button> <button class="release"
                    :disabled="releasing || !reason.trim()">{{ releasing ? '해제 중…' : '확인 후 해제' }}</button></div>
            <p v-if="releaseError" role="alert">{{ releaseError }}</p>
        </form>
        <p v-if="notice" role="status">{{ notice }}</p>
    </section>
</template>
<script
    setup>    import { ref, onUnmounted } from 'vue'; import api from '../api/api'; const props = defineProps({ userId: Number }); defineEmits(['member']); const target = ref(null), reason = ref(''), releasing = ref(false), releaseError = ref(''), notice = ref(''); async function release() { if (releasing.value) return; releasing.value = true; releaseError.value = ''; try { await api.post(`/admin/console/members/${props.userId}/blocks/${target.value.id}/release`, { reason: reason.value.trim() }); target.value = null; notice.value = '이 회원이 설정한 차단을 해제했습니다.'; page.value = 0; await load(); } catch (e) { releaseError.value = e.response?.data?.message || '해제하지 못했습니다. 다시 확인해 주세요.'; } finally { releasing.value = false; } }
    const data = ref({ total: 0, items: [] }), loaded = ref(false), busy = ref(false), error = ref(''), page = ref(0); let disposed = false; async function load() { if (busy.value) return; busy.value = true; error.value = ''; try { const res = await api.get(`/admin/console/members/${props.userId}/blocks`, { params: { page: page.value } }); if (!disposed) { data.value = res.data; loaded.value = true; } } catch (e) { if (!disposed) error.value = e.response?.data?.message || '목록을 불러오지 못했습니다.'; } finally { busy.value = false; } } onUnmounted(() => disposed = true);</script>
<style
    scoped>
    .release {
        color: #ad513d !important
    }

    .release-confirm {
        margin-top: 18px;
        padding: 18px;
        background: #fff6ee;
        border-radius: 12px;
        font-size: 13px
    }

    .release-confirm label {
        display: grid;
        gap: 6px;
        margin: 14px 0
    }

    .release-confirm input {
        padding: 10px;
        border: 1px solid #dfd5cd;
        border-radius: 8px;
        font: inherit;
    }

    .member-blocks {
        margin: 24px 0;
        padding: 20px;
        border: 1px solid #e6e9ed;
        border-radius: 12px
    }

    .member-blocks header,
    .member-blocks article {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px
    }

    .member-blocks h3 {
        margin: 0;
        font-size: 15px
    }

    .member-blocks p {
        font-size: 12px;
        color: #7d8998
    }

    .member-blocks article {
        padding: 14px 0;
        border-top: 1px solid #edf0f3;
        font-size: 12px
    }

    .member-blocks button {
        font: inherit;
        padding: 9px 13px;
        background: #fff;
        border: 1px solid #dce1e7;
        border-radius: 8px;
        color: #536171;
        cursor: pointer
    }

    .member-blocks button:disabled {
        opacity: .5
    }

    .member-blocks .member-name {
        border: 0;
        padding: 0;
        font-weight: 600;
        color: #334d67
    }

    .member-name small {
        color: #9aa4af
    }

    .tags {
        display: flex;
        gap: 6px;
        margin-top: 7px
    }

    .tags span {
        font-size: 11px;
        padding: 4px 7px;
        border-radius: 6px;
        background: #fff1e8;
        color: #a85f42
    }

    footer {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 16px;
        margin-top: 16px
    }
</style>
