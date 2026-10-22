<template>
  <section class="user-app-shell" :class="{'white-surface':route.path.startsWith('/members/') || route.path.startsWith('/chats/')}">
    <div v-if="checking" class="user-app-loading">
      <div class="loading-card">
        <p class="eyebrow">Loading</p>
        <h2>사용자 화면을 준비하고 있어요</h2>
        <p>로그인 상태와 계정 상태를 확인하는 중입니다.</p>
      </div>
    </div>

    <template v-else>
      <div v-if="eligible" class="app-topbar"><RouterLink to="/notifications" :aria-label="`알림 모아보기${inboxUnread?' · 새 알림 '+inboxUnread+'개':''}`"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M9 21h6"/></svg><span v-if="inboxUnread">{{inboxUnread>99?'99+':inboxUnread}}</span></RouterLink></div><main class="user-app-main">
        <router-view />
      </main>

      <nav class="bottom-nav" aria-label="사용자 하단 내비게이션">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to" :aria-label="item.label + (badge(item.to) ? ' · 새 소식 있음' : '')" class="nav-item" :class="{ active: route.path === item.to || route.path.startsWith(item.to + '/' ) }">
          <span class="nav-icon-wrap"><svg class="nav-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path :d="item.icon" />
          </svg><span v-if="badge(item.to)" class="nav-badge" :class="{ 'nav-dot': item.to === '/today' }" aria-hidden="true">{{ item.to === '/today' ? '' : badge(item.to) > 99 ? '99+' : badge(item.to) }}</span></span>
          <span class="nav-label">{{ item.label }}</span>
        </RouterLink>
      </nav>
    </template>
  </section>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch } from "vue";
import { RouterLink, useRoute, useRouter } from "vue-router";
import api from "../../api/api";
import { resetNotificationCache } from "../../components/notificationEvents";
import { clearToken } from "../../utils/auth";

const route = useRoute();
const router = useRouter();
const checking = ref(true);
const inboxUnread=ref(0);
const badges=ref({today:false,matches:0,chats:0});
let badgeTimer,disposed=false,eligible=false,inflight=false,queued=false;
function badge(path){return path==='/today'?badges.value.today:path==='/matches'?badges.value.matches:path==='/chats'?badges.value.chats:0;}
async function loadBadges(){
  if(disposed||!eligible||document.hidden)return;
  if(inflight){queued=true;return;}inflight=true;
  try{const {data}=await api.get('/social/notifications');if(!disposed)badges.value=data;const unread=await api.get('/social/inbox/unread');if(!disposed)inboxUnread.value=unread.data;}catch{/* Keep last known state during a connection failure. */}
  finally{inflight=false;if(queued){queued=false;loadBadges();}}
}
function resume(){if(!document.hidden)loadBadges();}
watch(()=>route.path,loadBadges);
onUnmounted(()=>{disposed=true;clearInterval(badgeTimer);window.removeEventListener('notifications-changed',loadBadges);window.removeEventListener('online',resume);document.removeEventListener('visibilitychange',resume);});

const navItems = [
  {
    to: "/today",
    label: "오늘의 인연",
    icon: "M12 20.25S4.5 15.88 4.5 9.9A4.1 4.1 0 0 1 8.6 5.75c1.4 0 2.63.7 3.4 1.76a4.1 4.1 0 0 1 3.4-1.76 4.1 4.1 0 0 1 4.1 4.15c0 5.98-7.5 10.35-7.5 10.35Z",
  },
  { to: "/matches", label: "매칭현황", icon: "M8 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Zm8 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6ZM2 20v-3a6 6 0 0 1 12 0v3Zm14 0v-3a8 8 0 0 0-1-4 5 5 0 0 1 7 4v3Z" },
  {
    to: "/chats",
    label: "채팅",
    icon: "M5.25 5.25h13.5a1.5 1.5 0 0 1 1.5 1.5v8.25a1.5 1.5 0 0 1-1.5 1.5H10.5l-4.95 3.3a.75.75 0 0 1-1.17-.62V16.5h-.63a1.5 1.5 0 0 1-1.5-1.5V6.75a1.5 1.5 0 0 1 1.5-1.5Zm2.25 4.13a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Zm4.5 0a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Zm4.5 0a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Z",
  },
  {
    to: "/profile",
    label: "프로필",
    icon: "M12 12.25a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 1.75c-4.05 0-7.25 2.28-7.25 5.08 0 .65.52 1.17 1.17 1.17h12.16c.65 0 1.17-.52 1.17-1.17 0-2.8-3.2-5.08-7.25-5.08Z",
  },
];

onMounted(async () => {
  try {
    const { data } = await api.get("/user/me");
    if(disposed)return;
    resetNotificationCache(data.id || data.email);
    eligible=data.status==='ACTIVE';
    if(eligible){loadBadges();badgeTimer=setInterval(loadBadges,3000);window.addEventListener('notifications-changed',loadBadges);window.addEventListener('online',resume);document.addEventListener('visibilitychange',resume);}

    if (data.status === "SUSPENDED") {
      window.alert("운영 정책상 이용이 제한되었습니다.");
      clearToken();
      router.replace("/");
      return;
    }

    // 심사 전 계정도 프로필 필수 항목은 입력할 수 있어야 하므로 프로필 화면은 허용한다.
    if (data.status !== "ACTIVE" && !["/profile","/settings","/profile/edit","/profile/photos","/support"].includes(route.path)) {
      router.replace("/review-pending");
      return;
    }
  } catch (error) {
    clearToken();
    router.replace("/");
    return;
  } finally {
    checking.value = false;
  }
});
</script>

<style scoped>
.nav-icon-wrap{position:relative;display:inline-flex;align-items:center;justify-content:center}
.nav-badge{position:absolute;top:-7px;left:15px;min-width:17px;height:17px;padding:0 4px;display:flex;align-items:center;justify-content:center;box-sizing:border-box;border-radius:10px;background:#e64047;color:#fff;font-size:10px;font-weight:800;line-height:1;box-shadow:0 0 0 2px #35231f}
.nav-badge.nav-dot{top:-3px;left:19px;min-width:8px;width:8px;height:8px;padding:0}

.user-app-shell { min-height: 100vh; }.user-app-shell.white-surface{background:#fff;}
.user-app-main { min-height: 100vh; padding-bottom: calc(96px + env(safe-area-inset-bottom)); }
.user-app-loading { min-height: 100vh; display: grid; place-items: center; padding: 20px; }
.loading-card {
  width: min(100%, 420px);
  padding: 28px 22px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.1);
  text-align: center;
}
.eyebrow {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}
.loading-card h2 { margin: 0; color: #31211d; }
.loading-card p:last-child { margin-bottom: 0; color: #6f564d; line-height: 1.6; }
.bottom-nav {
  position: fixed;
  left: 50%;
  bottom: max(12px, env(safe-area-inset-bottom));
  transform: translateX(-50%);
  width: min(calc(100% - 24px), 520px);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 10px;
  border-radius: 28px;
  background: rgba(45, 31, 28, 0.92);
  box-shadow: 0 18px 40px rgba(36, 20, 16, 0.28);
  z-index: 30;
}
.nav-item {
  display: grid;
  place-items: center;
  gap: 4px;
  min-height: 58px;
  padding: 8px 6px;
  border-radius: 20px;
  color: rgba(255, 245, 240, 0.7);
  text-decoration: none;
}
.nav-item.active {
  background: linear-gradient(180deg, rgba(234, 143, 100, 0.26), rgba(213, 111, 78, 0.18));
  color: #fff8f5;
}
.nav-icon {
  width: 22px;
  height: 22px;
  fill: currentColor;
  opacity: 0.9;
}
.nav-item.active .nav-icon {
  opacity: 1;
  filter: drop-shadow(0 4px 8px rgba(245, 191, 164, 0.22));
}
.nav-label { font-size: 11px; font-weight: 700; line-height: 1.1; text-align: center; }
.app-topbar{max-width:480px;margin:auto;display:flex;justify-content:flex-end;height:42px;padding:0 16px;box-sizing:border-box}.app-topbar a{position:relative;display:grid;place-items:center;width:42px;color:#92553f}.app-topbar svg{width:22px;height:22px;fill:none;stroke:currentColor;stroke-width:1.7}.app-topbar span{position:absolute;top:3px;right:0;background:#db4940;color:#fff;font-size:9px;min-width:14px;height:14px;border-radius:10px;text-align:center}</style>
