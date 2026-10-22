<template>
  <section class="home-page">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>

    <div class="home-shell" v-if="profile && userSummary">
      <header class="topbar">
        <div>
          <p class="eyebrow">Dating Lounge</p>
          <h1>{{ profile.nickname }}님, 오늘의 인연을 둘러보세요</h1>
          <p class="subtitle">
            메인 화면에서 추천 카드, 빠른 반응, 프로필 요약까지 한 번에 볼 수 있게 구성했습니다.
            실제 추천 API와 매칭 API가 붙으면 이 구조를 그대로 확장할 수 있어요.
          </p>
        </div>

        <div class="topbar-actions">
          <button class="ghost-button" @click="scrollToSection('discover-section')">추천 보기</button>
          <button class="ghost-button" @click="scrollToSection('profile-section')">내 프로필</button>
          <button class="primary-button" @click="scrollToSection('match-section')">매칭 현황</button>
          <button class="logout-button" @click="logout">로그아웃</button>
        </div>
      </header>

      <section class="hero-panel">
        <div class="hero-copy">
          <span class="status-pill" :class="statusClass">{{ statusLabel }}</span>
          <h2>{{ heroTitle }}</h2>
          <p>{{ heroDescription }}</p>

          <div class="hero-highlights">
            <article class="highlight-card">
              <span>프로필 완성도</span>
              <strong>{{ profileCompletion }}%</strong>
              <small>소개, 직업, MBTI, 이상형, 자기소개 기준</small>
            </article>
            <article class="highlight-card">
              <span>등록 사진</span>
              <strong>{{ images.length }}장</strong>
              <small>대표 사진 포함 최대 5장</small>
            </article>
            <article class="highlight-card">
              <span>계정 연결</span>
              <strong>{{ providerLabel }}</strong>
              <small>{{ providerMessage }}</small>
            </article>
          </div>
        </div>

        <div class="hero-visual">
          <div class="profile-frame">
            <img
              v-if="mainImage"
              :src="resolveImageUrl(mainImage.imageUrl)"
              :alt="`${profile.nickname} 대표 사진`"
            />
            <div v-else class="profile-placeholder">
              <span>{{ profile.nickname.slice(0, 1) }}</span>
              <p>대표 사진을 등록하면 메인 화면의 분위기가 더 살아나요.</p>
            </div>
          </div>

          <div class="mini-profile">
            <strong>{{ profile.nickname }}</strong>
            <p>{{ profile.region }} · {{ profile.job || '직업 미입력' }}</p>
            <div class="chip-row">
              <span class="chip">{{ profile.gender === 'MALE' ? '남성' : '여성' }}</span>
              <span class="chip">{{ profile.mbti || 'MBTI 미입력' }}</span>
              <span class="chip">{{ ageLabel }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="dashboard-grid">
        <article class="stat-card warm">
          <span>오늘의 분위기</span>
          <strong>{{ moodSummary }}</strong>
          <p>{{ introductionPreview }}</p>
        </article>
        <article class="stat-card blush">
          <span>이상형 키워드</span>
          <strong>{{ idealTypeSummary }}</strong>
          <p>프로필의 이상형 문장을 바탕으로 추천 카드 분위기를 맞췄습니다.</p>
        </article>
        <article class="stat-card sand">
          <span>생활 스타일</span>
          <strong>{{ lifestyleSummary }}</strong>
          <p>흡연, 음주, 종교 정보는 이후 추천 정교화와 필터링에 연결할 수 있어요.</p>
        </article>
      </section>

      <section id="discover-section" class="section-block">
        <div class="section-head">
          <div>
            <p class="section-label">Discover</p>
            <h3>오늘의 추천 카드</h3>
          </div>
          <span class="section-caption">카드 액션은 현재 프론트 시안용 상태로 동작합니다.</span>
        </div>

        <div class="discover-grid">
          <article v-for="card in recommendationCards" :key="card.id" class="discover-card">
            <div class="discover-photo" :style="{ background: card.background }">
              <div class="discover-overlay">
                <span>{{ card.badge }}</span>
                <strong>{{ card.name }}, {{ card.age }}</strong>
              </div>
            </div>

            <div class="discover-body">
              <div class="discover-meta">
                <span>{{ card.region }}</span>
                <span>{{ card.job }}</span>
              </div>
              <p>{{ card.introduction }}</p>
              <div class="tag-row">
                <span v-for="tag in card.tags" :key="tag" class="tag">{{ tag }}</span>
              </div>

              <div class="action-row">
                <button class="action-button pass" @click="markReaction(card.id, 'pass')">패스</button>
                <button class="action-button detail" @click="openCard(card)">상세 보기</button>
                <button class="action-button like" @click="markReaction(card.id, 'like')">좋아요</button>
              </div>

              <p v-if="reactions[card.id] === 'like'" class="reaction-text like-text">호감 표시를 보냈어요. 매칭되면 바로 대화로 이어질 수 있어요.</p>
              <p v-else-if="reactions[card.id] === 'pass'" class="reaction-text pass-text">이번 카드는 넘겼어요. 다음 추천에서 더 잘 맞는 사람을 보여드릴게요.</p>
            </div>
          </article>
        </div>
      </section>

      <section id="match-section" class="section-block match-layout">
        <div class="section-head compact">
          <div>
            <p class="section-label">Matches</p>
            <h3>메인에서 보는 매칭 현황</h3>
          </div>
        </div>

        <div class="match-grid">
          <article class="match-card active-match">
            <span class="match-badge">새로운 매칭</span>
            <strong>{{ featuredMatch.name }}</strong>
            <p>{{ featuredMatch.copy }}</p>
            <div class="tag-row spacious">
              <span class="tag soft">{{ featuredMatch.region }}</span>
              <span class="tag soft">{{ featuredMatch.job }}</span>
              <span class="tag soft">{{ featuredMatch.keyword }}</span>
            </div>
            <button class="primary-button compact-button" @click="openChatPreview = true">대화 시작 흐름 보기</button>
          </article>

          <article class="match-card queue-card">
            <span class="match-badge calm">오늘의 상태</span>
            <ul class="queue-list">
              <li>
                <strong>{{ likedCount }}명</strong>
                <span>좋아요를 보낸 카드</span>
              </li>
              <li>
                <strong>{{ passedCount }}명</strong>
                <span>넘긴 카드</span>
              </li>
              <li>
                <strong>{{ images.length }}장</strong>
                <span>현재 등록된 프로필 사진</span>
              </li>
            </ul>
          </article>
        </div>
      </section>

      <section id="profile-section" class="section-block profile-layout">
        <div class="section-head compact">
          <div>
            <p class="section-label">Profile Snapshot</p>
            <h3>내 프로필 한눈에 보기</h3>
          </div>
        </div>

        <div class="profile-layout-grid">
          <article class="info-card">
            <h4>기본 정보</h4>
            <dl>
              <div>
                <dt>이메일</dt>
                <dd>{{ profile.email }}</dd>
              </div>
              <div>
                <dt>거주 지역</dt>
                <dd>{{ profile.region }}</dd>
              </div>
              <div>
                <dt>직업</dt>
                <dd>{{ profile.job || '아직 입력 전' }}</dd>
              </div>
              <div>
                <dt>MBTI</dt>
                <dd>{{ profile.mbti || '아직 입력 전' }}</dd>
              </div>
            </dl>
          </article>

          <article class="info-card">
            <h4>자기소개</h4>
            <p class="body-copy">{{ profile.introduction || '아직 자기소개가 등록되지 않았습니다.' }}</p>
            <h4>성격</h4>
            <p class="body-copy">{{ profile.personality || '성격 소개를 추가하면 더 잘 맞는 추천을 만들 수 있어요.' }}</p>
          </article>

          <article class="info-card">
            <h4>이상형과 생활 습관</h4>
            <p class="body-copy">{{ profile.idealType || '이상형 설명을 입력하면 추천 정확도를 더 높일 수 있습니다.' }}</p>
            <div class="tag-row spacious">
              <span class="tag soft">{{ smokingLabel }}</span>
              <span class="tag soft">{{ drinkingLabel }}</span>
              <span class="tag soft">{{ religionLabel }}</span>
            </div>
          </article>
        </div>
      </section>
    </div>

    <div v-else class="loading-shell">
      <div class="loading-card">
        <p class="eyebrow">Loading</p>
        <h2>메인 화면을 준비하고 있어요</h2>
        <p>프로필과 사진 정보를 불러오는 중입니다.</p>
      </div>
    </div>

    <div v-if="selectedCard" class="modal-backdrop" @click.self="selectedCard = null">
      <div class="modal-card">
        <div class="modal-photo" :style="{ background: selectedCard.background }">
          <span>{{ selectedCard.badge }}</span>
          <strong>{{ selectedCard.name }}, {{ selectedCard.age }}</strong>
        </div>
        <div class="modal-body">
          <h3>{{ selectedCard.name }}님의 카드</h3>
          <p>{{ selectedCard.introduction }}</p>
          <div class="tag-row spacious">
            <span v-for="tag in selectedCard.tags" :key="tag" class="tag soft">{{ tag }}</span>
          </div>
          <div class="modal-list">
            <div>
              <dt>활동 지역</dt>
              <dd>{{ selectedCard.region }}</dd>
            </div>
            <div>
              <dt>직업</dt>
              <dd>{{ selectedCard.job }}</dd>
            </div>
            <div>
              <dt>추천 이유</dt>
              <dd>{{ selectedCard.reason }}</dd>
            </div>
          </div>
          <div class="action-row modal-actions">
            <button class="action-button pass" @click="markReaction(selectedCard.id, 'pass'); selectedCard = null">패스</button>
            <button class="action-button like" @click="markReaction(selectedCard.id, 'like'); selectedCard = null">좋아요</button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="openChatPreview" class="modal-backdrop" @click.self="openChatPreview = false">
      <div class="modal-card chat-preview-card">
        <div class="chat-preview-header">
          <span class="match-badge">채팅 시작 미리보기</span>
          <button class="close-button" @click="openChatPreview = false">닫기</button>
        </div>
        <div class="chat-bubble received">안녕하세요. 오늘 추천 카드에서 보고 먼저 인사드려요.</div>
        <div class="chat-bubble sent">반가워요. 분위기가 편안해서 대화해보고 싶었어요 :)</div>
        <div class="chat-bubble received">주말에 카페나 산책 좋아하시면 얘기 나눠봐요.</div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";
import { clearToken } from "../utils/auth";

const router = useRouter();
const userSummary = ref(null);
const profile = ref(null);
const images = ref([]);
const selectedCard = ref(null);
const openChatPreview = ref(false);
const reactions = ref({});

const mainImage = computed(() => images.value.find((image) => image.isMain) || images.value[0] || null);

const profileCompletion = computed(() => {
  if (!profile.value) return 0;
  const fields = [profile.value.job, profile.value.mbti, profile.value.personality, profile.value.idealType, profile.value.introduction];
  const completed = fields.filter((value) => value && String(value).trim()).length;
  return Math.round((completed / fields.length) * 100);
});

const providerLabel = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "PHONE") return "전화번호";
  return "자체 이메일";
});

const providerMessage = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "PHONE") return "현재는 전화번호 인증 로그인으로 연결되어 있어요.";
  return "현재는 자체 이메일 로그인으로 연결되어 있어요.";
});

const statusLabel = computed(() => {
  const status = profile.value?.status;
  if (status === "ACTIVE") return "이용 가능";
  if (status === "PENDING_REVIEW") return "심사 진행 중";
  if (status === "REJECTED") return "재등록 필요";
  return "상태 확인 중";
});

const statusClass = computed(() => {
  const status = profile.value?.status;
  if (status === "ACTIVE") return "status-active";
  if (status === "PENDING_REVIEW") return "status-pending";
  if (status === "REJECTED") return "status-rejected";
  return "";
});

const ageLabel = computed(() => {
  if (!profile.value?.birthDate) return "나이 미입력";
  const birthYear = Number(String(profile.value.birthDate).slice(0, 4));
  return `${new Date().getFullYear() - birthYear + 1}세`;
});

const heroTitle = computed(() => {
  if (!profile.value) return "오늘의 추천을 준비하고 있어요.";
  return `${profile.value.region}에서 잘 맞는 인연을 찾을 준비가 됐어요.`;
});

const heroDescription = computed(() => {
  if (!profile.value) return "프로필을 분석해 메인 화면을 구성하고 있습니다.";
  const mbtiPart = profile.value.mbti ? `${profile.value.mbti} 성향과 ` : "";
  return `${mbtiPart}${profile.value.introduction ? "자기소개 톤" : "기본 프로필 정보"}을 바탕으로 소개팅 앱 메인처럼 첫 화면을 구성했습니다.`;
});

const moodSummary = computed(() => {
  if (!profile.value?.personality) return "차분하고 진중한 소개를 준비 중";
  return profile.value.personality.length > 20 ? `${profile.value.personality.slice(0, 20)}...` : profile.value.personality;
});

const introductionPreview = computed(() => {
  if (!profile.value?.introduction) {
    return "자기소개를 더 채우면 추천 카드의 분위기와 문구를 더 정교하게 만들 수 있어요.";
  }
  return profile.value.introduction;
});

const idealTypeSummary = computed(() => {
  if (!profile.value?.idealType) return "대화가 잘 통하는 사람";
  return profile.value.idealType.length > 24 ? `${profile.value.idealType.slice(0, 24)}...` : profile.value.idealType;
});

const smokingLabel = computed(() => ({ NON_SMOKER: "비흡연", SMOKER: "흡연", OCCASIONAL: "가끔 흡연" }[profile.value?.smokingStatus] || "흡연 정보 미입력"));
const drinkingLabel = computed(() => ({ NONE: "술 안 마셔요", SOMETIMES: "가끔 한잔", OFTEN: "술자리 좋아해요" }[profile.value?.drinkingStatus] || "음주 정보 미입력"));
const religionLabel = computed(() => ({ NONE: "무교", CHRISTIAN: "기독교", BUDDHIST: "불교", CATHOLIC: "천주교", OTHER: "기타 종교" }[profile.value?.religion] || "종교 정보 미입력"));

const lifestyleSummary = computed(() => `${smokingLabel.value} · ${drinkingLabel.value} · ${religionLabel.value}`);

const recommendationCards = computed(() => {
  if (!profile.value) return [];

  const region = profile.value.region || "서울";
  const ideal = profile.value.idealType || "대화가 편안한 사람";
  const mbti = profile.value.mbti || "감성형";

  return [
    {
      id: 1,
      name: "서윤",
      age: 29,
      region,
      job: "브랜드 디자이너",
      badge: "오늘 가장 잘 맞는 카드",
      introduction: `${ideal}을 중요하게 보는 분위기를 반영해, 차분하게 대화가 이어질 타입으로 구성한 추천 카드예요.`,
      tags: [mbti, "주말 데이트", "카페 좋아함"],
      reason: "이상형 문장과 자기소개의 톤이 비슷해 첫 카드로 배치했어요.",
      background: "linear-gradient(135deg, #f5b490 0%, #f8d6be 100%)",
    },
    {
      id: 2,
      name: "민지",
      age: 31,
      region: `${region.split(' ')[0]} 인근`,
      job: "콘텐츠 마케터",
      badge: "대화 텐션이 잘 맞을 확률",
      introduction: "자기소개 톤과 생활 습관을 기준으로 편안하게 대화를 이어가기 좋은 카드로 준비했어요.",
      tags: [drinkingLabel.value, "산책", "사진 취향"],
      reason: "대화 스타일과 라이프스타일이 비교적 잘 맞는 카드예요.",
      background: "linear-gradient(135deg, #ef8c74 0%, #f3c3b2 100%)",
    },
    {
      id: 3,
      name: "지안",
      age: 27,
      region: "같은 생활권",
      job: "제품 기획자",
      badge: "프로필 완성도 기반 추천",
      introduction: "직업, MBTI, 자기소개가 채워질수록 이런 메인 카드가 실제 추천 결과로 더 정교해질 수 있어요.",
      tags: [smokingLabel.value, religionLabel.value, "깊은 대화"],
      reason: "현재 프로필 정보가 반영될 때 어떤 느낌의 추천이 나올지 보여주는 카드입니다.",
      background: "linear-gradient(135deg, #d9826b 0%, #f2b8a3 100%)",
    },
  ];
});

const featuredMatch = computed(() => ({
  name: reactions.value[1] === "like" ? "서윤" : "민지",
  region: profile.value?.region || "서울",
  job: reactions.value[1] === "like" ? "브랜드 디자이너" : "콘텐츠 마케터",
  keyword: profile.value?.mbti || "대화형",
  copy: "상호 호감이 이어지면 메인 화면에서 바로 채팅 시작 흐름으로 연결되는 형태를 상상해서 구성했습니다.",
}));

const likedCount = computed(() => Object.values(reactions.value).filter((value) => value === "like").length);
const passedCount = computed(() => Object.values(reactions.value).filter((value) => value === "pass").length);

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
      birthDate: "",
      region: "",
      job: "",
      mbti: "",
      personality: "",
      idealType: "",
      introduction: "",
      smokingStatus: "NON_SMOKER",
      drinkingStatus: "NONE",
      religion: "NONE",
      gender: "MALE",
    };
  }
}

onMounted(async () => {
  try {
    const [{ data: me }, myProfile, { data: myImages }] = await Promise.all([
      api.get("/user/me"),
      fetchProfileWithFallback(),
      api.get("/profile-images/me"),
    ]);

    if (me.status !== "ACTIVE") {
      router.replace("/review-pending");
      return;
    }

    userSummary.value = me;
    profile.value = myProfile;
    images.value = myImages;
  } catch (error) {
    clearToken();
    router.replace("/");
  }
});

function resolveImageUrl(imageUrl) {
  if (!imageUrl) return "";
  if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) return imageUrl;
  return `http://${window.location.hostname}:8082${imageUrl}`;
}

function scrollToSection(sectionId) {
  document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function openCard(card) {
  selectedCard.value = card;
}

function markReaction(cardId, type) {
  reactions.value = {
    ...reactions.value,
    [cardId]: type,
  };
}

function logout() {
  clearToken();
  router.replace("/");
}
</script>

<style scoped>
.home-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 20% 10%, rgba(255, 214, 191, 0.95) 0%, rgba(255, 214, 191, 0) 30%),
    radial-gradient(circle at 80% 0%, rgba(241, 153, 118, 0.55) 0%, rgba(241, 153, 118, 0) 32%),
    linear-gradient(180deg, #fff8f3 0%, #fdebe2 42%, #f6d2bf 100%);
  padding: 40px 20px 72px;
  font-family: "SUIT Variable", "Pretendard", sans-serif;
}

.ambient {
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 999px;
  filter: blur(40px);
  opacity: 0.45;
  pointer-events: none;
}

.ambient-left { top: 120px; left: -120px; background: rgba(232, 156, 122, 0.55); }
.ambient-right { top: 40px; right: -100px; background: rgba(255, 223, 205, 0.95); }

.home-shell { position: relative; z-index: 1; width: min(1180px, 100%); margin: 0 auto; }
.topbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 28px; }
.eyebrow, .section-label { margin: 0 0 10px; color: #af5f42; font-size: 12px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1, h2, h3, h4, strong { color: #2f211d; }
h1 { margin: 0; font-size: clamp(2rem, 4vw, 3.4rem); line-height: 1.05; }
.subtitle { max-width: 640px; margin: 14px 0 0; color: #6f564d; line-height: 1.65; }
.topbar-actions { display: flex; gap: 12px; flex-wrap: wrap; justify-content: flex-end; }

.primary-button, .ghost-button, .logout-button, .action-button, .close-button {
  border: none; border-radius: 999px; padding: 13px 18px; font-weight: 700; cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}
.primary-button { background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%); color: #fff; box-shadow: 0 14px 30px rgba(213, 111, 78, 0.24); }
.ghost-button { background: rgba(255, 248, 243, 0.82); color: #6a4137; border: 1px solid rgba(207, 150, 128, 0.36); }
.logout-button, .close-button { background: #34211d; color: #fff; }
.primary-button:hover, .ghost-button:hover, .logout-button:hover, .action-button:hover, .close-button:hover { transform: translateY(-1px); }
.compact-button { margin-top: 16px; padding: 12px 16px; }

.hero-panel {
  display: grid; grid-template-columns: 1.35fr 0.9fr; gap: 24px; padding: 28px; border-radius: 32px;
  background: rgba(255, 252, 249, 0.72); border: 1px solid rgba(239, 208, 193, 0.7);
  box-shadow: 0 25px 60px rgba(109, 57, 41, 0.12); backdrop-filter: blur(14px);
}
.status-pill { display: inline-flex; align-items: center; padding: 8px 12px; border-radius: 999px; font-size: 13px; font-weight: 800; margin-bottom: 16px; }
.status-active { background: rgba(179, 233, 201, 0.8); color: #176540; }
.status-pending { background: rgba(255, 225, 179, 0.85); color: #7a4e0b; }
.status-rejected { background: rgba(255, 204, 204, 0.88); color: #902d2d; }
.hero-copy h2 { margin: 0; font-size: clamp(1.8rem, 3vw, 2.6rem); line-height: 1.15; }
.hero-copy p { margin: 12px 0 0; color: #6d554d; line-height: 1.7; }
.hero-highlights { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-top: 24px; }
.highlight-card, .stat-card, .info-card, .discover-card, .match-card { border-radius: 24px; background: rgba(255, 255, 255, 0.88); border: 1px solid rgba(235, 209, 199, 0.85); }
.highlight-card { padding: 18px; }
.highlight-card span, .stat-card span { display: block; color: #916456; font-size: 13px; font-weight: 700; }
.highlight-card strong, .stat-card strong { display: block; margin-top: 10px; font-size: 1.4rem; }
.highlight-card small { display: block; margin-top: 8px; color: #7b655d; line-height: 1.5; }
.hero-visual { display: grid; gap: 16px; }
.profile-frame { min-height: 420px; border-radius: 28px; overflow: hidden; background: linear-gradient(180deg, #f3c8b0 0%, #e89a79 100%); box-shadow: inset 0 1px 0 rgba(255,255,255,0.45); }
.profile-frame img { width: 100%; height: 100%; object-fit: cover; display: block; }
.profile-placeholder { min-height: 420px; display: grid; place-items: center; text-align: center; padding: 24px; color: #fffaf7; }
.profile-placeholder span { width: 96px; height: 96px; display: inline-grid; place-items: center; margin-bottom: 16px; border-radius: 999px; background: rgba(255, 255, 255, 0.2); font-size: 2.4rem; font-weight: 800; }
.profile-placeholder p { max-width: 240px; line-height: 1.6; }
.mini-profile { padding: 18px 20px; border-radius: 24px; background: rgba(52, 33, 29, 0.92); color: #fff; }
.mini-profile strong { color: #fff; font-size: 1.25rem; }
.mini-profile p { margin: 8px 0 0; color: rgba(255, 245, 240, 0.85); }
.chip-row, .tag-row { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px; }
.chip, .tag { display: inline-flex; align-items: center; padding: 8px 12px; border-radius: 999px; font-size: 13px; font-weight: 700; }
.chip { background: rgba(255, 255, 255, 0.12); color: #fff; }
.dashboard-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-top: 18px; }
.stat-card { padding: 22px; }
.stat-card p { margin: 10px 0 0; color: #6e5950; line-height: 1.65; }
.warm { background: linear-gradient(180deg, rgba(255, 245, 238, 0.92) 0%, rgba(255, 235, 225, 0.95) 100%); }
.blush { background: linear-gradient(180deg, rgba(255, 240, 235, 0.92) 0%, rgba(250, 224, 215, 0.95) 100%); }
.sand { background: linear-gradient(180deg, rgba(255, 249, 240, 0.92) 0%, rgba(247, 230, 214, 0.96) 100%); }
.section-block { margin-top: 22px; padding: 26px; border-radius: 30px; background: rgba(255, 251, 248, 0.82); border: 1px solid rgba(239, 208, 193, 0.72); box-shadow: 0 22px 44px rgba(109, 57, 41, 0.08); }
.section-head { display: flex; justify-content: space-between; align-items: end; gap: 16px; margin-bottom: 18px; }
.section-head h3 { margin: 0; font-size: 1.8rem; }
.section-caption { color: #8a695e; font-size: 14px; }
.discover-grid, .match-grid, .profile-layout-grid { display: grid; gap: 16px; }
.discover-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.match-grid { grid-template-columns: 1.1fr 0.9fr; }
.profile-layout-grid { grid-template-columns: 1fr 1.1fr 0.9fr; }
.discover-card { overflow: hidden; }
.discover-photo { position: relative; min-height: 260px; padding: 16px; display: flex; align-items: flex-end; }
.discover-overlay { width: 100%; padding: 18px; border-radius: 22px; background: linear-gradient(180deg, rgba(52, 33, 29, 0.08) 0%, rgba(52, 33, 29, 0.82) 100%); }
.discover-overlay span, .match-badge { display: inline-flex; padding: 7px 10px; border-radius: 999px; background: rgba(255, 244, 239, 0.18); color: #fff7f2; font-size: 12px; font-weight: 700; }
.discover-overlay strong { display: block; margin-top: 12px; color: #fff; font-size: 1.5rem; }
.discover-body { padding: 18px; }
.discover-meta { display: flex; gap: 10px; flex-wrap: wrap; color: #8a6558; font-size: 13px; font-weight: 700; }
.discover-body p, .body-copy { margin: 14px 0 0; color: #634d45; line-height: 1.68; }
.tag { background: #f8ede7; color: #7d5a50; }
.tag.soft { background: #fff6f1; }
.action-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 18px; }
.action-button { padding: 12px 14px; }
.action-button.pass { background: #f3ebe5; color: #654b43; }
.action-button.detail { background: #fff3ec; color: #a6593e; }
.action-button.like { background: linear-gradient(135deg, #d56f4e 0%, #eea27d 100%); color: #fff; }
.reaction-text { margin-top: 12px; font-size: 13px; font-weight: 700; }
.like-text { color: #a44e35; }
.pass-text { color: #7d6257; }
.match-card { padding: 22px; }
.active-match { background: linear-gradient(135deg, rgba(255, 239, 232, 0.98) 0%, rgba(252, 226, 216, 0.95) 100%); }
.queue-card { background: rgba(255, 255, 255, 0.9); }
.match-badge { background: rgba(175, 95, 66, 0.12); color: #a25a3e; }
.match-badge.calm { background: rgba(104, 80, 71, 0.1); color: #6c4d42; }
.match-card strong { display: block; margin-top: 14px; font-size: 1.45rem; }
.match-card p { margin: 12px 0 0; color: #6a534b; line-height: 1.65; }
.queue-list { list-style: none; padding: 0; margin: 18px 0 0; display: grid; gap: 14px; }
.queue-list li { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 14px 0; border-bottom: 1px solid rgba(235, 209, 199, 0.85); }
.queue-list li:last-child { border-bottom: none; }
.queue-list strong { margin: 0; font-size: 1.3rem; }
.queue-list span { color: #765d53; font-size: 14px; }
.info-card { padding: 22px; }
.info-card h4 { margin: 0 0 14px; font-size: 1.1rem; }
.info-card dl { display: grid; gap: 14px; margin: 0; }
.info-card dt { color: #916456; font-size: 13px; font-weight: 700; }
.info-card dd { margin: 8px 0 0; color: #33241f; font-weight: 700; }
.modal-backdrop { position: fixed; inset: 0; background: rgba(34, 22, 18, 0.55); backdrop-filter: blur(8px); display: grid; place-items: center; padding: 20px; z-index: 20; }
.modal-card { width: min(760px, 100%); border-radius: 28px; overflow: hidden; background: #fffaf7; box-shadow: 0 30px 80px rgba(40, 24, 20, 0.28); }
.modal-photo { min-height: 260px; padding: 24px; display: flex; flex-direction: column; justify-content: flex-end; color: #fff; }
.modal-photo span { display: inline-flex; width: fit-content; padding: 7px 10px; border-radius: 999px; background: rgba(255,255,255,0.2); font-size: 12px; font-weight: 700; }
.modal-photo strong { margin-top: 14px; color: #fff; font-size: 2rem; }
.modal-body { padding: 24px; }
.modal-body h3 { margin: 0; font-size: 1.8rem; }
.modal-body p { color: #634d45; line-height: 1.7; }
.modal-list { display: grid; gap: 14px; margin-top: 18px; }
.modal-list dt { color: #916456; font-size: 13px; font-weight: 700; }
.modal-list dd { margin: 6px 0 0; color: #33241f; font-weight: 700; }
.modal-actions { margin-top: 22px; }
.chat-preview-card { padding: 24px; }
.chat-preview-header { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 18px; }
.chat-bubble { max-width: 78%; padding: 14px 16px; border-radius: 20px; margin-top: 10px; line-height: 1.6; }
.chat-bubble.received { background: #f7ece6; color: #5f4940; }
.chat-bubble.sent { margin-left: auto; background: linear-gradient(135deg, #d56f4e 0%, #eea27d 100%); color: #fff; }
.loading-shell { min-height: 100vh; display: grid; place-items: center; }
.loading-card { width: min(480px, 100%); padding: 32px; border-radius: 28px; background: rgba(255, 251, 248, 0.88); border: 1px solid rgba(239, 208, 193, 0.72); text-align: center; }
.loading-card h2 { margin: 0; }
.loading-card p:last-child { color: #6f564d; line-height: 1.6; }

@media (max-width: 1100px) {
  .hero-panel, .discover-grid, .dashboard-grid, .match-grid, .profile-layout-grid { grid-template-columns: 1fr; }
}

@media (max-width: 760px) {
  .home-page { padding: 22px 14px 40px; }
  .topbar, .section-head, .chat-preview-header { flex-direction: column; align-items: flex-start; }
  .topbar-actions { width: 100%; justify-content: flex-start; }
  .hero-panel, .section-block { padding: 20px; border-radius: 24px; }
  .hero-highlights, .action-row { grid-template-columns: 1fr; }
  .profile-frame, .profile-placeholder { min-height: 320px; }
  .chat-bubble { max-width: 100%; }
}
</style>
