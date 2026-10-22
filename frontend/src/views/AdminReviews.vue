<template>
  <section class="admin-page">
    <div class="admin-card">
      <div class="header-row">
        <div>
          <p class="eyebrow">Admin Review</p>
          <h1>회원가입 심사 관리</h1>
        </div>
      </div>

      <div class="toolbar">
        <label>
          <span>운영자 키</span>
          <input v-model.trim="adminKey" type="password" placeholder="X-Admin-Key 입력" />
        </label>

        <label>
          <span>목록 상태</span>
          <select v-model="statusFilter">
            <option value="PENDING_REVIEW">심사 대기</option>
            <option value="REJECTED">반려</option>
            <option value="ACTIVE">승인 완료</option>
          </select>
        </label>

        <label>
          <span>회원 검색</span>
          <input v-model.trim="searchKeyword" type="text" placeholder="이메일, 닉네임, 지역, 직업 검색" />
        </label>

        <label class="checkbox-filter">
          <input v-model="dueSoonOnly" type="checkbox" />
          <span>마감 임박 회원만 보기</span>
        </label>

        <button class="primary-button" @click="loadDashboard" :disabled="loading">
          {{ loading ? '불러오는 중...' : '대시보드 새로고침' }}
        </button>
      </div>

      <div class="summary-grid" v-if="summary">
        <article class="summary-card warm">
          <span>심사 대기</span>
          <strong>{{ summary.pendingReviewCount }}명</strong>
          <p>사진과 프로필을 확인해야 하는 가입 대기 회원입니다.</p>
        </article>
        <article class="summary-card blush">
          <span>반려 상태</span>
          <strong>{{ summary.rejectedCount }}명</strong>
          <p>수정 후 다시 심사를 기다리는 회원입니다.</p>
        </article>
        <article class="summary-card sand">
          <span>승인 완료</span>
          <strong>{{ summary.activeCount }}명</strong>
          <p>가입 승인이 끝나 서비스 이용이 가능한 회원입니다.</p>
        </article>
        <article class="summary-card alert">
          <span>마감 임박</span>
          <strong>{{ summary.dueSoonCount }}명</strong>
          <p>오늘 또는 내일 안에 자동 정리 기한이 도래하는 회원입니다.</p>
        </article>
      </div>

      <div class="template-box">
        <strong>반려 사유 템플릿 관리</strong>
        <div class="template-editor-grid">
          <label v-for="item in templateConfig" :key="item.key">
            <span>{{ item.label }}</span>
            <textarea v-model.trim="rejectTemplates[item.key]" rows="3"></textarea>
          </label>
        </div>
        <div class="template-row">
          <button class="template-button" @click="saveTemplates">템플릿 저장</button>
          <button class="template-button" @click="resetTemplates">기본값 복원</button>
          <button class="template-button" @click="applyTemplate('face')">현재 목록에 얼굴 템플릿 적용</button>
        </div>
        <p class="template-help">템플릿은 현재 브라우저에 저장되며, 회원 카드의 반려 사유에 바로 넣어 쓸 수 있습니다.</p>
      </div>

      <p v-if="message" class="message success">{{ message }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div v-if="candidates.length" class="candidate-list">
        <article v-for="candidate in candidates" :key="candidate.userId" class="candidate-card">
          <div class="candidate-head">
            <div>
              <strong>{{ candidate.nickname }}</strong>
              <p>{{ candidate.email }}</p>
            </div>
            <span class="status-badge">{{ candidate.status }}</span>
          </div>

          <div class="info-grid">
            <div><span>지역</span><strong>{{ candidate.region || '-' }}</strong></div>
            <div><span>직업</span><strong>{{ candidate.job || '-' }}</strong></div>
            <div><span>MBTI</span><strong>{{ candidate.mbti || '-' }}</strong></div>
            <div><span>성별/생년월일</span><strong>{{ candidate.gender }} / {{ candidate.birthDate }}</strong></div>
          </div>

          <div class="notice-grid">
            <div class="review-rule" :class="{ incomplete: !candidate.profileComplete }">
              <strong>프로필 필수 항목</strong>
              <p>{{ candidate.profileComplete ? '완료됨' : '미완료 상태입니다.' }}</p>
            </div>

            <div class="review-rule" :class="{ incomplete: !hasMinimumImages(candidate) }">
              <strong>심사용 사진 수</strong>
              <p>{{ candidate.images.length }}장 등록됨 / 승인 기준은 최소 2장입니다.</p>
            </div>

            <div class="review-rule" :class="deadlineRuleClass(candidate)">
              <strong>자동 정리 기한</strong>
              <p v-if="candidate.reviewDeadlineAt">{{ formatDeadline(candidate.reviewDeadlineAt) }}까지</p>
              <p v-else>기한 없음</p>
              <p v-if="candidate.reviewDeadlineAt" class="sub-line">남은 기간: {{ candidate.remainingDays }}일</p>
            </div>
          </div>

          <p class="intro">{{ candidate.introduction || '자기소개 없음' }}</p>

          <div class="image-list" v-if="candidate.images.length">
            <div v-for="image in candidate.images" :key="image.id" class="image-item">
              <img :src="toAbsoluteImageUrl(image.imageUrl)" alt="profile review" />
              <span>{{ image.imageOrder }}번 사진{{ image.isMain ? ' · 대표' : '' }}</span>
            </div>
          </div>

          <p v-else class="empty-image-text">아직 등록된 사진이 없습니다.</p>

          <label class="memo-box">
            <span>운영자 메모</span>
            <textarea
              v-model.trim="adminMemos[candidate.userId]"
              rows="3"
              placeholder="심사 시 참고할 내부 메모를 저장할 수 있습니다."
            ></textarea>
          </label>

          <div class="button-row compact-row">
            <button class="template-button small" @click="saveMemo(candidate.userId)">메모 저장</button>
            <button class="template-button small" @click="toggleHistory(candidate.userId)">
              {{ expandedHistoryUserId === candidate.userId ? '이력 닫기' : '이력 보기' }}
            </button>
          </div>

          <div v-if="expandedHistoryUserId === candidate.userId" class="history-box">
            <p v-if="historyLoading" class="history-empty">이력을 불러오는 중입니다.</p>
            <template v-else>
              <div v-if="histories[candidate.userId]?.length" class="history-list">
                <article v-for="history in histories[candidate.userId]" :key="history.id" class="history-item">
                  <div class="history-head">
                    <strong>{{ history.actionType }}</strong>
                    <span>{{ formatDateTime(history.createdAt) }}</span>
                  </div>
                  <p>{{ history.detail }}</p>
                </article>
              </div>
              <p v-else class="history-empty">아직 저장된 처리 이력이 없습니다.</p>
            </template>
          </div>

          <label class="reject-box">
            <span>반려 사유</span>
            <textarea
              v-model.trim="rejectComments[candidate.userId]"
              rows="3"
              placeholder="반려 시 사용자에게 보여줄 사유를 입력해주세요. 반려 시 재등록 기한은 7일로 갱신됩니다."
            ></textarea>
          </label>

          <div class="template-row inline-template-row">
            <button class="template-button small" @click="fillRejectComment(candidate.userId, rejectTemplates.face)">얼굴 식별 어려움</button>
            <button class="template-button small" @click="fillRejectComment(candidate.userId, rejectTemplates.count)">사진 수 부족</button>
            <button class="template-button small" @click="fillRejectComment(candidate.userId, rejectTemplates.profile)">프로필 내용 부족</button>
            <button class="template-button small" @click="fillRejectComment(candidate.userId, rejectTemplates.policy)">운영 정책 위반</button>
          </div>

          <div class="action-row">
            <button
              class="approve-button"
              :disabled="!hasMinimumImages(candidate) || !candidate.profileComplete"
              @click="approveCandidate(candidate.userId)"
            >
              승인
            </button>
            <button class="reject-button" @click="rejectCandidate(candidate.userId)">반려</button>
          </div>

          <p v-if="!hasMinimumImages(candidate) || !candidate.profileComplete" class="approval-help">
            승인하려면 사진 2장 이상과 프로필 필수 항목 완료가 모두 필요합니다.
          </p>
        </article>
      </div>

      <p v-else-if="loaded" class="empty-state">조회된 회원이 없습니다.</p>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import api from "../api/api";

const apiBaseURL = String(api.defaults.baseURL || "");
const assetBaseURL = apiBaseURL.endsWith("/api") ? apiBaseURL.slice(0, -4) : apiBaseURL;
const templateStorageKey = "adminRejectTemplates";

const defaultRejectTemplates = {
  face: "대표 사진에서 얼굴 식별이 어려워 반려되었습니다. 밝고 선명한 정면 사진으로 다시 등록해주세요.",
  count: "심사 기준에 필요한 사진 수가 부족해 반려되었습니다. 대표 사진 포함 최소 2장을 등록해주세요.",
  profile: "프로필 필수 항목이 충분히 작성되지 않아 반려되었습니다. 직업, MBTI, 성격, 이상형, 자기소개를 모두 보완해주세요.",
  policy: "운영 정책에 맞지 않는 사진 또는 정보가 포함되어 반려되었습니다. 정책에 맞게 수정 후 다시 등록해주세요.",
};

const templateConfig = [
  { key: "face", label: "얼굴 식별 어려움" },
  { key: "count", label: "사진 수 부족" },
  { key: "profile", label: "프로필 내용 부족" },
  { key: "policy", label: "운영 정책 위반" },
];

const adminKey = ref(localStorage.getItem("adminReviewKey") || "");
const statusFilter = ref("PENDING_REVIEW");
const searchKeyword = ref("");
const dueSoonOnly = ref(false);
const candidates = ref([]);
const summary = ref(null);
const rejectComments = ref({});
const adminMemos = ref({});
const histories = ref({});
const expandedHistoryUserId = ref(null);
const historyLoading = ref(false);
const rejectTemplates = ref(loadRejectTemplates());
const loading = ref(false);
const loaded = ref(false);
const message = ref("");
const errorMessage = ref("");

function loadRejectTemplates() {
  try {
    const stored = localStorage.getItem(templateStorageKey);
    return stored ? { ...defaultRejectTemplates, ...JSON.parse(stored) } : { ...defaultRejectTemplates };
  } catch {
    return { ...defaultRejectTemplates };
  }
}

const getHeaders = () => ({
  "X-Admin-Key": adminKey.value,
});

const persistAdminKey = () => {
  localStorage.setItem("adminReviewKey", adminKey.value);
};

const hasMinimumImages = (candidate) => candidate.images.length >= 2;

const loadDashboard = async () => {
  loading.value = true;
  message.value = "";
  errorMessage.value = "";

  try {
    persistAdminKey();
    const [summaryRes, candidatesRes] = await Promise.all([
      api.get(`/admin/reviews/summary`, { headers: getHeaders() }),
      api.get(`/admin/reviews`, {
        headers: getHeaders(),
        params: { status: statusFilter.value, dueSoonOnly: dueSoonOnly.value, q: searchKeyword.value },
      }),
    ]);

    summary.value = summaryRes.data;
    candidates.value = candidatesRes.data;
    rejectComments.value = Object.fromEntries(candidates.value.map((candidate) => [candidate.userId, candidate.reviewComment || ""]));
    adminMemos.value = Object.fromEntries(candidates.value.map((candidate) => [candidate.userId, candidate.adminMemo || ""]));
    loaded.value = true;
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "심사 대시보드 조회에 실패했습니다.";
  } finally {
    loading.value = false;
  }
};

const toggleHistory = async (userId) => {
  if (expandedHistoryUserId.value === userId) {
    expandedHistoryUserId.value = null;
    return;
  }

  expandedHistoryUserId.value = userId;

  if (histories.value[userId]) {
    return;
  }

  historyLoading.value = true;
  try {
    const { data } = await api.get(`/admin/reviews/${userId}/history`, {
      headers: getHeaders(),
    });
    histories.value = {
      ...histories.value,
      [userId]: data,
    };
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "처리 이력을 불러오지 못했습니다.";
  } finally {
    historyLoading.value = false;
  }
};

const approveCandidate = async (userId) => {
  try {
    persistAdminKey();
    const { data } = await api.post(`/admin/reviews/${userId}/approve`, null, {
      headers: getHeaders(),
    });
    message.value = data.message;
    histories.value = {};
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "승인 처리에 실패했습니다.";
  }
};

const rejectCandidate = async (userId) => {
  try {
    persistAdminKey();
    const reviewComment = rejectComments.value[userId];
    if (!reviewComment) {
      errorMessage.value = "반려 사유를 먼저 입력해주세요.";
      return;
    }

    const { data } = await api.post(
      `/admin/reviews/${userId}/reject`,
      { reviewComment },
      { headers: getHeaders() }
    );
    message.value = data.message;
    histories.value = {};
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "반려 처리에 실패했습니다.";
  }
};

const saveMemo = async (userId) => {
  try {
    persistAdminKey();
    const adminMemo = adminMemos.value[userId];
    if (!adminMemo) {
      errorMessage.value = "운영자 메모를 먼저 입력해주세요.";
      return;
    }

    const { data } = await api.put(
      `/admin/reviews/${userId}/memo`,
      { adminMemo },
      { headers: getHeaders() }
    );
    message.value = data.message;
    histories.value = {};
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "운영자 메모 저장에 실패했습니다.";
  }
};

const saveTemplates = () => {
  localStorage.setItem(templateStorageKey, JSON.stringify(rejectTemplates.value));
  message.value = "반려 사유 템플릿을 저장했습니다.";
  errorMessage.value = "";
};

const resetTemplates = () => {
  rejectTemplates.value = { ...defaultRejectTemplates };
  localStorage.removeItem(templateStorageKey);
  message.value = "반려 사유 템플릿을 기본값으로 되돌렸습니다.";
  errorMessage.value = "";
};

const applyTemplate = (key) => {
  if (!rejectTemplates.value[key]) return;
  candidates.value.forEach((candidate) => {
    rejectComments.value[candidate.userId] = rejectTemplates.value[key];
  });
};

const fillRejectComment = (userId, text) => {
  rejectComments.value[userId] = text;
};

const deadlineRuleClass = (candidate) => {
  if (candidate.remainingDays === 0) return "danger";
  if (candidate.remainingDays === 1) return "warning";
  return "";
};

const toAbsoluteImageUrl = (path) => {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  return `${assetBaseURL}${path}`;
};

const formatDeadline = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};

const formatDateTime = (value) => {
  if (!value) return "-";
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

onMounted(loadDashboard);
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #fff4eb 0%, #ffd9c8 100%);
}

.admin-card {
  width: min(100%, 1180px);
  margin: 0 auto;
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 251, 247, 0.96);
  box-shadow: 0 20px 50px rgba(99, 52, 35, 0.14);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0 0 10px;
  color: #aa5131;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #36231c;
}

.toolbar {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 1.5fr 0.9fr 1.2fr 1fr auto;
  gap: 16px;
  align-items: end;
}

.checkbox-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid #e6c1b4;
  border-radius: 14px;
  background: #fff;
}

.summary-grid {
  margin-top: 20px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid #efd5ca;
}

.summary-card span {
  display: block;
  color: #916456;
  font-size: 13px;
  font-weight: 700;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 1.7rem;
  color: #34211d;
}

.summary-card p {
  margin: 10px 0 0;
  color: #6f544c;
  line-height: 1.55;
}

.warm { background: linear-gradient(180deg, rgba(255,245,238,0.95) 0%, rgba(255,234,225,0.96) 100%); }
.blush { background: linear-gradient(180deg, rgba(255,240,235,0.95) 0%, rgba(250,224,215,0.96) 100%); }
.sand { background: linear-gradient(180deg, rgba(255,249,240,0.95) 0%, rgba(247,230,214,0.96) 100%); }
.alert { background: linear-gradient(180deg, rgba(255,239,228,0.98) 0%, rgba(255,223,205,0.96) 100%); }

.template-box {
  margin-top: 18px;
  padding: 18px;
  border-radius: 18px;
  background: #fff7ef;
  border: 1px solid #efd5ca;
}

.template-box strong {
  display: block;
  color: #38231d;
}

.template-editor-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.template-row,
.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.template-button {
  border: 1px solid #e2bba9;
  border-radius: 999px;
  background: #fff;
  color: #8b4c34;
  padding: 10px 14px;
  font-weight: 700;
  cursor: pointer;
}

.template-button.small {
  padding: 8px 12px;
  font-size: 13px;
}

.template-help {
  margin: 10px 0 0;
  color: #7a5d55;
  font-size: 14px;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input,
select,
textarea {
  border: 1px solid #e6c1b4;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  font-family: inherit;
}

textarea {
  resize: vertical;
}

.primary-button,
.approve-button,
.reject-button {
  border: none;
  border-radius: 14px;
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button,
.approve-button {
  background: #cd6d2d;
  color: #fff;
}

.approve-button:disabled {
  cursor: not-allowed;
  background: #d9c5b8;
  color: #7c665a;
}

.reject-button {
  background: #fff;
  color: #7c2d24;
  border: 1px solid #e0b4a8;
}

.message {
  margin-top: 16px;
}

.success {
  color: #1f7a43;
}

.error {
  color: #b72f2f;
}

.candidate-list {
  margin-top: 24px;
  display: grid;
  gap: 20px;
}

.candidate-card {
  padding: 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #efd5ca;
}

.candidate-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.candidate-head p {
  margin: 6px 0 0;
  color: #7a5d55;
}

.status-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff1e9;
  color: #8d462a;
  font-size: 13px;
  font-weight: 700;
}

.info-grid,
.notice-grid {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.info-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.notice-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.info-grid span {
  display: block;
  margin-bottom: 6px;
  color: #906759;
  font-size: 13px;
}

.info-grid strong {
  color: #3a251e;
}

.intro {
  margin-top: 16px;
  color: #5f473f;
  line-height: 1.6;
}

.review-rule {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff7ef;
  border: 1px solid #efd5ca;
  color: #6f544c;
}

.review-rule strong {
  display: block;
  margin-bottom: 6px;
  color: #3a251e;
}

.review-rule p,
.sub-line {
  margin: 0;
}

.sub-line {
  margin-top: 6px;
  font-size: 13px;
  color: #8b6d64;
}

.review-rule.incomplete {
  background: #fff2f0;
  border-color: #efc1b8;
}

.review-rule.warning {
  background: #fff0dc;
  border-color: #eba35f;
}

.review-rule.danger {
  background: #ffe7e2;
  border-color: #e48072;
}

.image-list {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.image-item {
  display: grid;
  gap: 8px;
}

.image-item img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
  border: 1px solid #efd5ca;
  background: #fff6f1;
}

.image-item span {
  color: #6f544c;
  font-size: 13px;
}

.empty-image-text {
  margin-top: 16px;
  color: #8b6d64;
}

.memo-box,
.reject-box {
  margin-top: 16px;
}

.compact-row {
  margin-top: 12px;
}

.history-box {
  margin-top: 14px;
  padding: 14px;
  border-radius: 16px;
  background: #fff8f3;
  border: 1px solid #efd5ca;
}

.history-list {
  display: grid;
  gap: 10px;
}

.history-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #f0ddd3;
}

.history-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.history-head strong {
  color: #3a251e;
  font-size: 14px;
}

.history-head span {
  color: #8b6d64;
  font-size: 13px;
}

.history-item p,
.history-empty {
  margin: 8px 0 0;
  color: #654d44;
  line-height: 1.55;
}

.inline-template-row {
  margin-top: 12px;
}

.action-row {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.approval-help {
  margin-top: 12px;
  color: #b15a39;
  font-size: 14px;
  font-weight: 600;
}

.empty-state {
  margin-top: 24px;
  color: #7a5d55;
}

@media (max-width: 1100px) {
  .toolbar,
  .summary-grid,
  .template-editor-grid,
  .info-grid,
  .notice-grid {
    grid-template-columns: 1fr;
  }
}
</style>
