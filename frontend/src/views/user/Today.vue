<template>
  <section class="today-page daily-matches">
    <header class="match-heading"><h1 class="match-eyebrow">TODAY'S CONNECTION</h1><RecommendationSettings v-if="configured" @saved="conditionsSaved" /></header>

    <RecommendationSettings v-if="configured === false" initial @saved="conditionsSaved" />
    <div v-else-if="!deck" class="match-notice" role="status">
      <span class="notice-heart" aria-hidden="true">♡</span>
      <h2>{{ loading ? '오늘의 인연을 준비하고 있어요' : '잠시 연결이 어려워요' }}</h2>
      <p>{{ loading ? '불러오는 중…' : errorMessage }}</p>
      <button v-if="!loading" type="button" class="retry-button" @click="loadDeck">다시 불러오기</button>
    </div>

    <template v-else>
      <div class="deck-viewport">
      <div ref="track" class="match-track" role="region" aria-roledescription="carousel" aria-label="오늘의 인연 카드"
           tabindex="0" @scroll.passive="onScroll" @keydown.left.prevent="goTo(activeIndex - 1)" @keydown.right.prevent="goTo(activeIndex + 1)">
        <article v-for="card in deck.cards" :key="`${deck.cycleDay}-${card.slot}`" class="match-slide"
                 :aria-label="card.slot>=6?'추가 인연':`${card.slot < 3 ? '오전' : '오후'} 인연 ${card.slot % 3 + 1}`">
          <div class="flip-scene" :class="{ 'is-open': card.state === 'OPEN' }">
            <div class="flip-inner">
              <button class="card-cover" type="button" :disabled="card.state !== 'HIDDEN' || busySlot !== null"
                      :tabindex="activeIndex === card.slot && card.state === 'HIDDEN' ? 0 : -1"
                      :aria-hidden="card.state === 'OPEN'" @click="reveal(card)">
                <span class="cover-ornament" aria-hidden="true"><span>♡</span></span>
                <span class="cover-copy">
                  <template v-if="card.state === 'LOCKED'">
                    <span class="countdown">{{ remaining(card.unlockAt) }}</span><span>후 오픈</span>
                  </template>
                  <template v-else-if="card.state === 'PAUSED'"><strong>휴식 중</strong><span>설정에서 휴식 모드를 해제해 주세요.</span></template><template v-else-if="card.state === 'EMPTY'">
                    <strong>추천 준비 중</strong>
                  </template>
                  <template v-else-if="card.state === 'UNAVAILABLE'">
                    <strong>현재 확인할 수 없는 카드입니다</strong>
                  </template>
                  <template v-else>
                    <span>{{ busySlot === card.slot ? '불러오는 중…' : '탭하여 열기' }}</span>
                  </template>
                </span>
              </button>

              <div class="card-person" :aria-hidden="card.state !== 'OPEN'">
                <template v-if="card.profile">
                  <div class="person-photo"><button class="card-menu" aria-label="상대 메뉴" :tabindex="activeIndex === card.slot ? 0 : -1" @click="actions.open({id:card.profile.userId,nickname:card.profile.nickname},'report')">⋯</button>
                    <RouterLink class="photo-profile-link" :to="`/members/${card.profile.userId}`" :aria-label="`${card.profile.nickname} 프로필 보기`" :tabindex="activeIndex === card.slot ? 0 : -1">
                    <img v-if="!failedImages[card.slot] && photoUrl(card.profile.imageUrl)" :src="photoUrl(card.profile.imageUrl)"
                         :alt="`${card.profile.nickname}님의 프로필 사진`" draggable="false" @error="failedImages[card.slot] = true" />
                    <span v-else class="photo-placeholder" aria-label="사진을 불러올 수 없음">♡</span></RouterLink>
                    <div class="person-title"><h2><RouterLink :to="`/members/${card.profile.userId}`" :tabindex="activeIndex === card.slot ? 0 : -1">{{ card.profile.nickname }} <span>{{ card.profile.age }}</span></RouterLink></h2><p>{{ card.profile.region }}</p></div>
                  </div>
                  <div class="reaction-buttons">
                    <button type="button" class="like-button" :aria-pressed="card.reaction === 'LIKE'" :disabled="busySlot !== null || card.reaction === 'LIKE'" :tabindex="activeIndex === card.slot ? 0 : -1" @click="react(card,'LIKE')">{{ card.reaction === 'LIKE' ? '♥ 보낸 좋아요' : '♡ 좋아요' }}</button>
                    <button type="button" :tabindex="activeIndex === card.slot ? 0 : -1" @click="direct.open(card.profile.userId)">메시지 보내기</button>
                  </div>
                  <div class="person-details">
                    <div class="person-tags"><span v-if="card.profile.job">{{ card.profile.job }}</span><span v-if="card.profile.mbti">{{ card.profile.mbti }}</span></div>
                    <p v-if="card.profile.introduction" class="person-intro">{{ card.profile.introduction }}</p>

                  </div>
                </template>
              </div>
            </div>
          </div>
        </article>

        <article class="match-slide" aria-label="크레딧 추가 오픈 안내">
          <div class="credit-card">
<div class="credit-copy"><span class="credit-symbol" aria-hidden="true">＋</span><p>크레딧 구매 시<br>추가 오픈 가능</p></div>
<button class="retry-button" :disabled="extraBusy" @click="openExtra">10크레딧으로 1장 열기</button><span class="credit-status">1장당 10크레딧 · 횟수 제한 없음</span>
          </div>
        </article>
      </div>

        <button v-if="activeIndex > 0" type="button" class="peek-control previous" aria-label="이전 카드" @click="goTo(activeIndex - 1)"></button>
        <button v-if="activeIndex < deck.cards.length" type="button" class="peek-control next" aria-label="다음 카드" @click="goTo(activeIndex + 1)"></button>
      </div>
      <p v-if="errorMessage" class="match-error" role="alert">{{ errorMessage }} <button type="button" @click="loadDeck">다시 확인</button></p>
    </template>
    <dialog ref="extraModal" class="social-dialog" @cancel="extraBusy&&$event.preventDefault()"><h2>추가 인연 열기</h2><template v-if="extraQuote"><p>10크레딧으로 오늘의 인연 1장을 추가로 엽니다.</p><p>보유 {{extraQuote.balance}}크레딧 · 다음 오전 8시 초기화</p><p>이미 소개한 상대는 제외하며, 조건에 맞는 새 인연이 없으면 차감되지 않습니다.</p><RouterLink v-if="extraQuote.balance<10" to="/credits">크레딧 충전하기</RouterLink><button :disabled="extraBusy" @click="extraModal.close()">취소</button><button class="retry-button" :disabled="extraBusy||extraQuote.balance<10" @click="buyExtra">{{extraBusy?'처리 중…':'10크레딧 사용 후 열기'}}</button></template><p v-if="extraError" role="alert">{{extraError}}</p></dialog>
    <DirectMessageDialog ref="direct" /><MemberActions ref="actions" @changed="loadDeck"/>
  </section>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue';
import api from '../../api/api';
import {requestId} from '../../components/requestId';
import { acknowledgeNotifications } from '../../components/notificationEvents';
import RecommendationSettings from '../../components/RecommendationSettings.vue';
import DirectMessageDialog from '../../components/DirectMessageDialog.vue';
import MemberActions from '../../components/MemberActions.vue';
const direct = ref(),actions=ref();

const extraModal=ref(),extraQuote=ref(),extraBusy=ref(false),extraError=ref('');let pendingExtra;
async function openExtra(){if(extraBusy.value)return;extraBusy.value=true;extraError.value='';try{extraQuote.value=(await api.get('/matches/today/extra')).data;if(!pendingExtra||pendingExtra.cycleDay!==extraQuote.value.cycleDay)pendingExtra={cycleDay:extraQuote.value.cycleDay,expectedCost:extraQuote.value.cost,requestId:requestId()};extraModal.value.showModal();}catch(e){errorMessage.value=e.response?.data?.message||'가격을 불러오지 못했어요.';}finally{extraBusy.value=false;}}
async function buyExtra(){if(extraBusy.value||!extraQuote.value)return;extraBusy.value=true;extraError.value='';try{const {data}=await api.post('/matches/today/extra',pendingExtra);pendingExtra=null;await loadDeck();extraModal.value.close();await nextTick();goTo(data.slot);}catch(e){extraError.value=e.response?.data?.message||'결과를 확인하지 못했어요. 다시 눌러도 중복 차감되지 않습니다.';}finally{extraBusy.value=false;}}
const configured = ref(null);
function conditionsSaved(data) { configured.value=data.configured; if(!deck.value)loadDeck(); }
const deck = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const busySlot = ref(null);
const activeIndex = ref(0);
const failedImages = ref({});
const track = ref(null);
const now = ref(Date.now());
let serverOffset = 0;
let timer;
let retryAt = 0;
let disposed = false;

function remaining(target) {
  const seconds = Math.max(0, Math.ceil((Date.parse(target) - now.value) / 1000));
  return `${Math.floor(seconds / 3600)}시간 ${String(Math.floor(seconds % 3600 / 60)).padStart(2, '0')}분 ${String(seconds % 60).padStart(2, '0')}초`;
}
function photoUrl(path) {
  try {
    const url = new URL(path, new URL(api.defaults.baseURL, window.location.href));
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch { return ''; }
}
async function loadDeck() {
  if (loading.value || disposed) return;
  loading.value = true;
  errorMessage.value = '';
  try {
    if(configured.value !== true) {
      const settings=await api.get('/social/preferences');
      if(disposed)return;
      configured.value=settings.data.configured;
      if(configured.value!==true)return;
    }
    const { data } = await api.get('/matches/today');
    if (disposed) return;
    const changed = deck.value?.cycleDay !== data.cycleDay;
    serverOffset = Date.parse(data.serverNow) - Date.now();
    now.value = Date.now() + serverOffset;
    deck.value = data;
    acknowledgeNotifications([data.eventKey]);
    if (changed) {
      failedImages.value = {};
      activeIndex.value = 0;
      await nextTick();
      goTo(0, false);
    }
  } catch (error) {
    if(error.response?.status===428){configured.value=false;deck.value=null;}
    errorMessage.value = error.response?.data?.message || '인연을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.';
  } finally {
    loading.value = false;
    retryAt = Date.now() + 15000;
  }
}
async function reveal(card) {
  if (card.state !== 'HIDDEN' || busySlot.value !== null) return;
  await updateCard(card, 'reveal');
}
async function react(card, reaction) {
  if (busySlot.value !== null || card.reaction === reaction) return;
  await updateCard(card, 'reaction', { reaction });
}
async function updateCard(card, action, body) {
  const cycle = deck.value.cycleDay;
  busySlot.value = card.slot;
  errorMessage.value = '';
  try {
    const { data } = await api.post(`/matches/today/${cycle}/${card.slot}/${action}`, body);
    if (!disposed && deck.value?.cycleDay === cycle) deck.value.cards[card.slot] = data;
  } catch (error) {
    const message = error.response?.data?.message || '연결이 원활하지 않아요. 다시 시도해 주세요.';
    if ([403, 409].includes(error.response?.status)) await loadDeck();
    errorMessage.value = message;
  } finally { busySlot.value = null; }
}
function goTo(index, smooth = true) {
  if (!track.value) return;
  const bounded = Math.max(0, Math.min(deck.value?.cards.length||6, index));
  const slide = track.value.children[bounded];
  if (!slide) return;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  track.value.scrollTo({ left: slide.offsetLeft - (track.value.clientWidth - slide.clientWidth) / 2, behavior: smooth && !reduceMotion ? 'smooth' : 'instant' });
  activeIndex.value = bounded;
}
function onScroll() {
  if (!track.value) return;
  const left = track.value.scrollLeft;
  const centerOffset = (track.value.clientWidth - track.value.children[0].clientWidth) / 2;
  let closest = 0;
  [...track.value.children].forEach((slide, index, slides) => {
    if (Math.abs(slide.offsetLeft - centerOffset - left) < Math.abs(slides[closest].offsetLeft - centerOffset - left)) closest = index;
  });
  activeIndex.value = closest;
}
function tick() {
  now.value = Date.now() + serverOffset;
  if (!deck.value) return;
  if (now.value >= Date.parse(deck.value.resetAt)) {
    // Remove yesterday's revealed profiles even if the refresh request fails.
    deck.value = null;
    loadDeck();
  } else if (now.value >= Date.parse(deck.value.nextRefreshAt) && Date.now() >= retryAt) loadDeck();
}
function resume() { if (!document.hidden) { tick(); loadDeck(); } }
onMounted(() => {
  loadDeck(); timer = window.setInterval(tick, 1000);
  document.addEventListener('visibilitychange', resume);
  window.addEventListener('online', resume);
});
onUnmounted(() => {
  disposed = true; clearInterval(timer);
  document.removeEventListener('visibilitychange', resume);
  window.removeEventListener('online', resume);
});
</script>

<style scoped>
.daily-matches { width: 100%; max-width: 480px; margin: 0 auto; color: #31211d; }
.match-heading { margin: 4px 0 20px; display:flex; align-items:center; justify-content:space-between; gap:8px; }
.daily-matches .match-heading .match-eyebrow { margin: 0; color: #af5f42; font-size: 13px !important; letter-spacing: .14em; font-weight: 800; line-height: 1.5; }
.match-heading h1 { margin: 0; }
.deck-viewport { position: relative; width: calc(100% + 32px); margin-inline: -16px; }
.match-track { position: relative; display: flex; gap: 12px; padding-inline: 24px; overflow-x: auto; overscroll-behavior-x: contain; scroll-snap-type: x mandatory; scroll-padding-inline: 24px; scrollbar-width: none; }
.peek-control { position: absolute; top: 0; bottom: 0; width: 24px; border: 0; background: transparent; padding: 0; cursor: pointer; z-index: 2; }
.peek-control.previous { left: 0; }
.peek-control.next { right: 0; }
.match-track::-webkit-scrollbar { display: none; }
.match-slide { flex: 0 0 100%; min-width: 0; scroll-snap-align: center; scroll-snap-stop: always; }
.flip-scene { perspective: 1400px; height: clamp(400px, calc(100dvh - 222px), 720px); }
.flip-inner { position: relative; width: 100%; height: 100%; transform-style: preserve-3d; transition: transform .65s cubic-bezier(.22,.7,.22,1); }
.is-open .flip-inner { transform: rotateY(180deg); }
.card-cover, .card-person { position: absolute; inset: 0; width: 100%; height: 100%; backface-visibility: hidden; -webkit-backface-visibility: hidden; border-radius: 18px; overflow: hidden; border: 0; }
.card-cover { display: flex; flex-direction: column; justify-content: center; gap: 32px; align-items: center; padding: 24px; color: #8b4936; background: radial-gradient(ellipse at 50% 35%, #fff8ee 0%, transparent 60%), linear-gradient(145deg,#ffe3d6,#f3b39b); cursor: pointer; }
.card-cover:disabled { cursor: default; opacity: 1; }
.cover-ornament { display: grid; place-items: center; margin: 0; }
.cover-ornament span { font-size: 68px; line-height: 1; color: #d66d4d; }
.cover-copy { display: grid; gap: 12px; text-align: center; }
.cover-copy strong { font-size: 21px; font-weight: 700; letter-spacing: -.04em; }
.cover-copy > span { font-size: 13px; line-height: 1.6; }
.cover-copy .countdown { font-size: 20px; font-variant-numeric: tabular-nums; font-weight: 800; color: #ba5437; }
.card-person { transform: rotateY(180deg); background: #fffcf9; display: flex; flex-direction: column; }
.person-photo { position: relative; flex: 1; min-height: 180px; background: linear-gradient(135deg,#f5b490,#f8d6be); overflow: hidden; }
.person-photo::after { content: ''; position: absolute; inset: 50% 0 0; background: linear-gradient(transparent,rgba(49,33,29,.72)); pointer-events: none; }
.person-photo img { width: 100%; height: 100%; position: absolute; object-fit: cover; object-position: center 30%; }
.photo-placeholder { height: 100%; display: grid; place-items: center; font-size: 70px; color: #fff8f4; }
.person-title { position: absolute; bottom: 18px; left: 18px; right: 18px; z-index: 1; color: #fff8f4; }
.person-title h2 { margin: 0; font-size: 25px !important; }
.person-title h2 span { font-weight: 400; margin-left: 5px; }
.person-title p { margin: 5px 0 0; font-size: 12px; }
.person-details { flex: none; padding: 16px; }
.person-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.person-tags span { border-radius: 10px; padding: 6px 9px; background: #fff2ea; font-size: 11px; color: #8e5b4c; }
.person-intro { font-size: 13px; line-height: 1.65; margin: 12px 0 16px; max-height: 66px; overflow-y: auto; color: #6f564d; }
.reaction-buttons { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; padding:12px 12px 0; }
.reaction-buttons button { border: 1px solid transparent; border-radius: 14px; padding: 12px 8px; background: #f3e2db; color: #7a5247; font-weight: 700; font-size: 13px; cursor: pointer; }
.reaction-buttons .like-button { background: linear-gradient(135deg,#d56f4e,#ed9f79); color: white; }
.reaction-buttons .chosen { border-color: #ba5437; }
.credit-card { height: clamp(400px,calc(100dvh - 222px),720px); padding: 24px; display: flex; flex-direction: column; justify-content: center; gap: 32px; align-items: center; text-align: center; border-radius: 18px; border: 0; background: linear-gradient(160deg,#fffaf5,#f5d7c7); color: #8b4936; }
.credit-status { font-size: 11px; color: #8b675b; }
.credit-copy { display: grid; gap: 12px; }
.credit-symbol { font-size: 60px; color: #d66d4d; }
.credit-copy p { margin: 0; font-size: 14px; line-height: 1.7; }
.match-notice { padding: 60px 20px; text-align: center; color: #8b675b; }
.notice-heart { font-size: 56px; color: #d66d4d; }
.match-notice p { font-size: 14px; line-height: 1.7; }
.retry-button, .match-error button { border: 1px solid #e5c2b2; border-radius: 12px; color: #9d5a43; background: #fff8f4; padding: 10px 16px; }
.match-error { color: #b34b36; font-size: 13px; line-height: 1.6; }
@media (prefers-reduced-motion: reduce) { .flip-inner { transition: none; } }
@media (max-width: 350px) { .card-cover, .credit-card { padding: 20px; } .deck-viewport { width: calc(100% + 20px); margin-inline: -10px; } .cover-copy .countdown { font-size: 18px; } }
.card-menu{position:absolute;top:12px;right:12px;z-index:3;border:0;border-radius:50%;width:44px;height:44px;background:rgba(255,248,244,.92);color:#92553f;font-size:28px;cursor:pointer}.person-title a{color:inherit;text-decoration:none}.photo-profile-link{position:absolute;inset:0;display:block;color:inherit;text-decoration:none}.photo-profile-link:focus-visible{outline:3px solid #d56f4e;outline-offset:-4px}</style>
