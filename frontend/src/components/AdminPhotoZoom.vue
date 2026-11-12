<template>
    <dialog ref="modal" class="photo-zoom" @close="source = ''" @click="backdrop">
        <header><span>사진 확대</span><button type="button" @click="modal.close()" aria-label="확대 사진 닫기">×</button></header>
        <div class="zoom-scroll"><img v-if="source" :src="source" alt="확대한 프로필 사진"
                :style="{ width: (zoom * 100) + '%', maxWidth: 'none' }" /></div>
        <footer><label>확대 <input type="range" min="1" max="3" step="0.1" v-model.number="zoom" /></label><button
                @click="zoom = 1">원래 크기</button></footer>
    </dialog>
</template>
<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'; import { useRoute } from 'vue-router'; const route = useRoute(), modal = ref(), source = ref(''), zoom = ref(1);
function open(e) { if (!route.path.startsWith('/admin/') || !e.target?.matches?.('.review-photos img,.compare img,.history-grid img,.duplicate-photos img,.community-moderation img')) return; const src = e.target.currentSrc || e.target.src; if (!src) return; source.value = src; zoom.value = 1; modal.value.showModal(); }
function key(e) { if (e.key === 'Enter' || e.key === ' ') { if (e.target?.matches?.('.review-photos img,.compare img,.history-grid img,.duplicate-photos img,.community-moderation img')) { e.preventDefault(); open(e); } } } function backdrop(e) { if (e.target === modal.value) modal.value.close(); }
onMounted(() => { document.addEventListener('click', open); document.addEventListener('keydown', key); }); onUnmounted(() => { document.removeEventListener('click', open); document.removeEventListener('keydown', key); }); watch(() => route.fullPath, () => modal.value?.close());
</script>
<style
    scoped>
    .photo-zoom {
        border: 0;
        border-radius: 16px;
        padding: 0;
        width: min(900px, calc(100% - 24px));
        max-height: 94dvh;
        background: #211c19;
        color: white
    }

    .photo-zoom::backdrop {
        background: #000b
    }

    .photo-zoom header,
    .photo-zoom footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 14px 20px;
        gap: 16px
    }

    .photo-zoom button {
        border: 0;
        background: #eee2d8;
        border-radius: 10px;
        padding: 8px 14px;
        color: #40332b;
        font: inherit;
        cursor: pointer
    }

    .zoom-scroll {
        overflow: auto;
        max-height: 72dvh
    }

    .zoom-scroll img {
        display: block;
        object-fit: contain;
        max-height: none
    }

    .photo-zoom label {
        display: flex;
        gap: 12px;
        align-items: center
    }
</style>
