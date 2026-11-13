<template>
  <section class="profile-page">
    <div v-if="profile" class="profile-shell">
      <header class="my-profile-title"><h1>내 프로필</h1></header>
      <section class="page-card">
        <div class="my-profile-hero"><RouterLink to="/profile/photos" class="main-photo"><img v-if="mainPhoto" v-private-src="mainPhoto" alt="내 대표사진"/><span v-else>사진 등록</span></RouterLink><div><h2>{{profile.nickname}}</h2><p>{{profile.region || '지역 미입력'}}</p><button type="button" class="edit-profile" @click="goToEditProfile">프로필 수정</button></div></div>

      </section>
      <section class="page-card completion-card"><header><strong>프로필 완성도</strong><b>{{completion?.percent ?? 0}}%</b></header><template v-if="completion"><progress :value="completion.percent" max="100" aria-label="프로필 완성도"/><div class="reward-steps"><button v-for="step in completion.steps" :key="step.percent" :disabled="claiming || !step.available" :class="{claimed:step.claimed,ready:step.available}" @click="claimReward(step)"><span>{{step.percent}}%</span><strong>{{step.claimed?'수령 완료':'+2 코인'}}</strong><small>{{step.claimed?'✓':step.available?'받기':completion.percent>=step.percent?'심사 후 수령':'미달성'}}</small></button></div><p class="reward-note">단계마다 2코인 · 각 보상은 한 번만 받을 수 있어요.</p><details><summary>완성도 항목 보기</summary><RouterLink v-for="item in completion.items" :key="item.label" :to="item.link" class="completion-item"><span>{{item.done?'✓':'○'}} {{item.label}}</span><span>10%</span></RouterLink></details></template><p v-if="rewardMessage" role="status">{{rewardMessage}}</p><p v-if="rewardError" role="alert">{{rewardError}} <button @click="loadCompletion">다시 확인</button></p></section>


      <section class="page-card">
        <dl class="summary-list preview-list">
          <div>
            <dt>생년월일</dt>
            <dd>{{ profile.birthDate || "-" }}</dd>
          </div>
          <div>
            <dt>성별</dt>
            <dd>{{ genderLabel }}</dd>
          </div>
          <div>
            <dt>거주 지역</dt>
            <dd>{{ profile.region || "-" }}</dd>
          </div>
          <div>
            <dt>직업</dt>
            <dd>{{ profile.job || "-" }}</dd>
          </div>
          <div>
            <dt>MBTI</dt>
            <dd>{{ profile.mbti || "-" }}</dd>
          </div>
          <div>
            <dt>흡연 여부</dt>
            <dd>{{ smokingStatusLabel }}</dd>
          </div>
          <div>
            <dt>음주 여부</dt>
            <dd>{{ drinkingStatusLabel }}</dd>
          </div>
          <div>
            <dt>종교</dt>
            <dd>{{ religionLabel }}</dd>
          </div>
        </dl>

        <TraitSummary :json="profile.traitsJson"/>
        <div class="text-blocks">
          <p><strong>내 성격</strong><br />{{ profile.personality || "미입력" }}</p>
          <p><strong>자기소개</strong><br />{{ profile.introduction || "미입력" }}</p>
        </div>
      </section>
      <section class="page-card profile-shortcuts"><RouterLink to="/settings">설정 <span>›</span></RouterLink><RouterLink to="/profile/photos">내 사진 관리 <span>›</span></RouterLink><RouterLink to="/credits">크레딧 충전·사용내역 <span>›</span></RouterLink><RouterLink to="/notifications">알림 모아보기 <span>›</span></RouterLink></section>
      <section class="page-card">
        <div class="section-head">
          <div>
            <p class="section-label">Summary</p>
          </div>
          <div class="summary-actions">
            <span class="status-pill">{{ profile.status }}</span>
          </div>
        </div>

        <dl class="summary-list">
          <div>
            <dt>이메일</dt>
            <dd>{{ profile.email }}</dd>
          </div>
          <div>
            <dt>닉네임</dt>
            <dd>{{ profile.nickname }}</dd>
          </div>
          <div>
            <dt>전화번호</dt>
            <dd>{{ profile.phone || "미인증" }}</dd>
          </div>
          <div>
            <dt>가입 방식</dt>
            <dd>{{ providerLabel }}</dd>
          </div>
          <div>
            <dt>등록 사진</dt>
            <dd>{{ imageCount }}장</dd>
          </div>
        </dl>
      </section>

      <section class="page-card"><details class="profile-menu"><summary>계정 및 설정</summary><div class="header-actions">
            <button class="ghost-button" type="button" @click="goToSupport">고객센터</button>
            <button class="ghost-button" type="button" @click="goToPhoneVerify">{{ phoneActionLabel }}</button>
            <button class="logout-button" type="button" @click="logout">로그아웃</button>
          </div></details></section>
    </div>
  </section>

</template>

<script setup>
import TraitSummary from "../../components/TraitSummary.vue";

import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const router = useRouter();
const profile = ref(null);
const imageCount = ref(0),mainPhoto=ref(''),completion=ref(null),claiming=ref(false),rewardMessage=ref(''),rewardError=ref('');
async function loadCompletion(){try{completion.value=(await api.get('/profile/completion')).data;rewardError.value='';}catch{rewardError.value='완성도를 불러오지 못했어요.';}}
async function claimReward(step){if(claiming.value)return;claiming.value=true;rewardError.value='';rewardMessage.value='';try{completion.value=(await api.post(`/profile/completion/${step.percent}/claim`)).data;rewardMessage.value=`${step.percent}% 달성 보상 2코인을 받았어요.`;}catch(e){await loadCompletion();rewardError.value=e.response?.data?.message||'수령 결과를 확인하지 못했어요. 다시 확인해 주세요.';}finally{claiming.value=false;}}


const providerLabel = computed(() => {
  if (profile.value?.provider === "GOOGLE") return "Google";
  if (profile.value?.provider === "FIREBASE_EMAIL") return "이메일 (Firebase)";
  if (profile.value?.provider === "PHONE") return "전화번호";
  return "자체 이메일";
});

const phoneActionLabel = computed(() => (profile.value?.phone ? "전화번호 변경" : "전화번호 인증"));

const genderLabel = computed(() => {
  if (profile.value?.gender === "MALE") return "남성";
  if (profile.value?.gender === "FEMALE") return "여성";
  return "-";
});

const smokingStatusLabel = computed(
  () =>
    ({
      NON_SMOKER: "비흡연",
      OCCASIONAL: "가끔 흡연",
      SMOKER: "흡연",
    })[profile.value?.smokingStatus] || "-",
);

const drinkingStatusLabel = computed(
  () =>
    ({
      NONE: "안 함",
      SOMETIMES: "가끔",
      OFTEN: "자주",
    })[profile.value?.drinkingStatus] || "-",
);

const religionLabel = computed(
  () =>
    ({
      NONE: "없음",
      CHRISTIAN: "기독교",
      CATHOLIC: "천주교",
      BUDDHIST: "불교",
      OTHER: "기타",
    })[profile.value?.religion] || "-",
);

async function fetchProfileWithFallback() {
  try {
    const { data } = await api.get("/profile/me");
    return data;
  } catch (error) {
    if (error.response?.status !== 404) {
      throw error;
    }

    const { data } = await api.get("/user/me");
    return {
      ...data,
      phone: "",
      birthDate: "",
      gender: "MALE",
      region: "",
      job: "",
      mbti: "",
      personality: "",
      idealType: "",
      introduction: "",
      smokingStatus: "NON_SMOKER",
      drinkingStatus: "NONE",
      religion: "NONE",
    };
  }
}

async function loadProfile() {
  try {
    const [myProfile, { data: images }] = await Promise.all([
      fetchProfileWithFallback(),
      api.get("/profile-images/me"),
    ]);

    profile.value = myProfile;
    imageCount.value = images.length;
    mainPhoto.value=(images.find(i=>i.isMain)||images[0])?.imageUrl||'';
    loadCompletion();
  } catch (error) {
    clearToken();
    router.replace("/");
  }
}

function goToEditProfile() {
  router.push("/profile/edit");
}

function goToSupport() {
  router.push("/support");
}

function goToPhoneVerify() {
  router.push("/profile/phone");
}


function logout() {
  clearToken();
  router.replace("/");
}

onMounted(loadProfile);
</script>

<style scoped>
.my-profile-title{min-height:42px;display:flex;align-items:center;padding-right:54px}.my-profile-title h1{font-size:20px!important;line-height:26px}
.my-profile-hero{display:flex;gap:18px;align-items:center;margin-bottom:22px}.main-photo{display:grid;place-items:center;width:108px;height:132px;flex:none;border-radius:18px;overflow:hidden;background:#f5e6dd;color:#a16d55;text-decoration:none;font-size:12px}.main-photo img{width:100%;height:100%;object-fit:cover}.my-profile-hero h2{font-size:21px}.my-profile-hero p{font-size:13px;color:#947568;margin:8px 0}.edit-profile{border:0;border-radius:12px;background:#d56f4e;color:#fff;padding:10px 16px;font:inherit;font-size:13px}
.completion-card header{display:flex;justify-content:space-between;gap:8px}.completion-card header b{color:#b65c3f}.completion-card progress{appearance:none;width:100%;height:8px;margin:20px 0 12px;border:0;border-radius:8px;overflow:hidden;background:#eeded4}.completion-card progress::-webkit-progress-bar{background:#eeded4}.completion-card progress::-webkit-progress-value{background:#d56f4e}.completion-card progress::-moz-progress-bar{background:#d56f4e}.reward-steps{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:6px}.reward-steps button{display:grid;gap:5px;padding:9px 2px;border:1px solid #e8d7cb;background:#fbf5f0;color:#9d8a7e;border-radius:12px;font:inherit;font-size:11px}.reward-steps strong{font-size:11px;white-space:nowrap}.reward-steps small{font-size:10px}.reward-steps .ready{background:#d56f4e;color:white;border-color:#d56f4e;cursor:pointer}.reward-steps .claimed{background:#f0e6de;color:#8b6049}.reward-note{font-size:11px;line-height:1.6;color:#947568}.completion-card summary{font-size:12px;cursor:pointer;padding:10px 0}.completion-item{display:flex;justify-content:space-between;font-size:12px;gap:8px;padding:9px 0;color:#805d4c;text-decoration:none}.completion-card [role]{font-size:12px;line-height:1.5}

.profile-page { min-height: 100vh; padding: max(16px, env(safe-area-inset-top)) 16px 24px; display: flex; justify-content: center; }
.profile-shell { width: min(100%, 720px); display: grid; gap: 16px; }
.page-card {
  padding: 24px 20px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
}
.eyebrow, .section-label { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1, h2 { margin: 0; color: #31211d; }
.description { margin: 12px 0 0; color: #6f564d; line-height: 1.7; }
.section-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.summary-actions { display: flex; align-items: center; gap: 10px; }
.status-pill { padding: 8px 12px; border-radius: 999px; background: #fff1e8; color: #9d5a43; font-size: 12px; font-weight: 800; }
.summary-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin: 0; }
.summary-list div { padding: 14px; border-radius: 18px; background: rgba(255, 247, 242, 0.9); }
.summary-list dt { color: #8b675b; font-size: 13px; }
.summary-list dd { margin: 8px 0 0; color: #2f211d; font-weight: 700; word-break: break-all; }
.preview-list { margin-bottom: 16px; }
.text-blocks { display: grid; gap: 12px; color: #5d4640; line-height: 1.7; }
.text-blocks p { margin: 0; padding: 16px; border-radius: 18px; background: rgba(255, 247, 242, 0.9); }
.ghost-button, .logout-button { border-radius: 999px; padding: 12px 18px; font-size: 14px; font-weight: 700; cursor: pointer; }
.ghost-button { border: 1px solid rgba(207, 150, 128, 0.36); background: rgba(255, 248, 243, 0.82); color: #6a4137; }
.logout-button { border: none; background: #f3dfd7; color: #7a473c; }
@media (max-width: 760px) {
  .summary-list { grid-template-columns: 1fr; }
  .section-head { flex-direction: column; align-items: flex-start; }
  .header-actions,
  .summary-actions { width: 100%; justify-content: space-between; }
}
</style>



<style scoped>
.my-profile-title{min-height:42px;display:flex;align-items:center;padding-right:54px}.my-profile-title h1{font-size:20px!important;line-height:26px}
.my-profile-hero{display:flex;gap:18px;align-items:center;margin-bottom:22px}.main-photo{display:grid;place-items:center;width:108px;height:132px;flex:none;border-radius:18px;overflow:hidden;background:#f5e6dd;color:#a16d55;text-decoration:none;font-size:12px}.main-photo img{width:100%;height:100%;object-fit:cover}.my-profile-hero h2{font-size:21px}.my-profile-hero p{font-size:13px;color:#947568;margin:8px 0}.edit-profile{border:0;border-radius:12px;background:#d56f4e;color:#fff;padding:10px 16px;font:inherit;font-size:13px}
.completion-card header{display:flex;justify-content:space-between;gap:8px}.completion-card header b{color:#b65c3f}.completion-card progress{appearance:none;width:100%;height:8px;margin:20px 0 12px;border:0;border-radius:8px;overflow:hidden;background:#eeded4}.completion-card progress::-webkit-progress-bar{background:#eeded4}.completion-card progress::-webkit-progress-value{background:#d56f4e}.completion-card progress::-moz-progress-bar{background:#d56f4e}.reward-steps{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:6px}.reward-steps button{display:grid;gap:5px;padding:9px 2px;border:1px solid #e8d7cb;background:#fbf5f0;color:#9d8a7e;border-radius:12px;font:inherit;font-size:11px}.reward-steps strong{font-size:11px;white-space:nowrap}.reward-steps small{font-size:10px}.reward-steps .ready{background:#d56f4e;color:white;border-color:#d56f4e;cursor:pointer}.reward-steps .claimed{background:#f0e6de;color:#8b6049}.reward-note{font-size:11px;line-height:1.6;color:#947568}.completion-card summary{font-size:12px;cursor:pointer;padding:10px 0}.completion-item{display:flex;justify-content:space-between;font-size:12px;gap:8px;padding:9px 0;color:#805d4c;text-decoration:none}.completion-card [role]{font-size:12px;line-height:1.5}
.profile-shortcuts{display:grid;gap:0}.profile-shortcuts a{display:flex;justify-content:space-between;padding:17px 0;color:#4b382e;text-decoration:none;font-size:14px;border-bottom:1px solid #f0e7e1}.profile-shortcuts a:last-child{border:0}.profile-shortcuts span{color:#ac8e7c}</style>
