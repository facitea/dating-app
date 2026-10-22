<template>
  <section class="admin-page">
    <div class="admin-card">
      <div class="page-head">
        <div>
          <p class="eyebrow">Admin Safety</p>
          <h1>로맨스 스캠 모니터</h1>
          <p class="sub-copy">위험 회원 검토, 재가입 차단, 신고 처리까지 한곳에서 관리합니다.</p>
        </div>
        <RouterLink class="ghost-button" to="/admin/reviews">가입 심사로 이동</RouterLink>
      </div>

      <div class="toolbar">
        <label>
          <span>운영자 키</span>
          <input v-model.trim="adminKey" type="password" placeholder="X-Admin-Key 입력" />
        </label>
        <label>
          <span>위험 상태</span>
          <select v-model="riskStatus">
            <option value="WATCH">주의</option>
            <option value="HIGH_RISK">고위험</option>
            <option value="ALL">전체</option>
          </select>
        </label>
        <label>
          <span>검색</span>
          <input v-model.trim="searchKeyword" type="text" placeholder="이메일, 닉네임, 메모, 차단 사유" />
        </label>
        <label>
          <span>차단 식별값</span>
          <select v-model="identityType">
            <option value="ALL">전체</option>
            <option value="EMAIL">이메일</option>
            <option value="PHONE">전화번호</option>
          </select>
        </label>
        <label>
          <span>신고 상태</span>
          <select v-model="reportStatus">
            <option value="OPEN">처리 대기</option>
            <option value="RESOLVED">처리 완료</option>
          </select>
        </label>
        <button class="primary-button" @click="loadDashboard" :disabled="loading">{{ loading ? '불러오는 중...' : '새로고침' }}</button>
      </div>

      <p v-if="message" class="message success">{{ message }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="summary-grid" v-if="summary">
        <article class="summary-card amber"><span>고위험 회원</span><strong>{{ summary.highRiskUserCount }}명</strong></article>
        <article class="summary-card soft"><span>주의 회원</span><strong>{{ summary.watchUserCount }}명</strong></article>
        <article class="summary-card sand"><span>열린 신고</span><strong>{{ summary.openReportCount }}건</strong></article>
        <article class="summary-card mint"><span>처리 완료</span><strong>{{ summary.resolvedReportCount }}건</strong></article>
      </div>

      <div class="layout-grid">
        <section>
          <div class="section-head">
            <div>
              <h2>위험 회원 목록</h2>
              <p>운영자가 직접 상태를 조정하고 메모를 남길 수 있습니다.</p>
            </div>
          </div>

          <div v-if="riskUsers.length" class="risk-list">
            <article v-for="user in riskUsers" :key="user.userId" class="risk-card">
              <div class="risk-head">
                <div>
                  <strong>{{ user.nickname }}</strong>
                  <p>{{ user.email }}</p>
                </div>
                <div class="badge-group">
                  <span class="score-badge">{{ user.fraudRiskScore }}점</span>
                  <span class="status-badge" :class="user.fraudReviewStatus.toLowerCase()">{{ user.fraudReviewStatus }}</span>
                </div>
              </div>

              <div class="meta-grid">
                <div><span>가입 방식</span><strong>{{ user.provider }}</strong></div>
                <div><span>계정 상태</span><strong>{{ user.status }}</strong></div>
                <div><span>지역</span><strong>{{ user.region || '-' }}</strong></div>
                <div><span>열린 신고</span><strong>{{ user.openReportCount }}건</strong></div>
              </div>

              <p class="detail-copy"><strong>최근 탐지</strong><br />{{ user.latestRiskDetail }}</p>
              <p class="detail-copy"><strong>재가입 차단</strong><br />{{ user.identityBlocked ? `차단됨 (${user.blockedIdentityTypes || '내부 식별값'})` : '미차단' }}</p>
              <p class="detail-copy"><strong>운영 메모</strong><br />{{ user.adminMemo || '없음' }}</p>

              <label class="review-box">
                <span>재검토 메모</span>
                <textarea v-model.trim="reviewNotes[user.userId]" rows="3" placeholder="재검토 근거를 남겨주세요."></textarea>
              </label>
              <div class="template-row">
                <button
                  v-for="template in reviewTemplates"
                  :key="template"
                  type="button"
                  class="template-chip"
                  @click="applyReviewTemplate(user.userId, template)"
                >
                  {{ template }}
                </button>
              </div>

              <div class="action-grid">
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'MARK_NORMAL')">정상화</button>
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'MARK_WATCH')">주의 유지</button>
                <button class="ghost-button compact danger" @click="reviewRiskUser(user.userId, 'SUSPEND_ACCOUNT')">이용 제한</button>
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'RELEASE_ACCOUNT')">제한 해제</button>
                <button class="ghost-button compact danger" @click="reviewRiskUser(user.userId, 'BLOCK_IDENTITY')">재가입 차단</button>
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'UNBLOCK_IDENTITY')">차단 해제</button>
                <button class="ghost-button compact" @click="toggleLogs(user.userId)">{{ expandedUserId === user.userId ? '위험 로그 닫기' : '위험 로그 보기' }}</button>
              </div>

              <div v-if="expandedUserId === user.userId" class="logs-box">
                <article v-for="log in riskLogs[user.userId] || []" :key="log.id" class="log-item">
                  <div class="log-head"><strong>{{ riskTypeLabel(log.riskType) }}</strong><span>{{ formatDateTime(log.createdAt) }}</span></div>
                  <p>{{ log.detail }}</p>
                  <small>점수 {{ log.score }}</small>
                </article>
                <p v-if="!(riskLogs[user.userId] || []).length" class="muted-copy">표시할 위험 로그가 없습니다.</p>
              </div>
            </article>
          </div>
          <p v-else class="empty-state">조건에 맞는 위험 회원이 없습니다.</p>
        </section>

        <section>
          <div class="section-head">
            <div>
              <h2>사용자 신고</h2>
              <p>신고 접수 내역을 확인하고 운영 처리 메모를 남깁니다.</p>
            </div>
          </div>

          <div v-if="reports.length" class="report-list">
            <article v-for="report in reports" :key="report.id" class="report-card">
              <div class="report-head">
                <strong>#{{ report.id }} · {{ reasonLabel(report.reasonCode) }}</strong>
                <span class="report-status">{{ report.status }}</span>
              </div>
              <p class="report-meta">신고자 {{ report.reporterUserId }} → 대상 {{ report.reportedUserId }}</p>
              <p class="detail-copy">{{ report.detail }}</p>
              <label class="review-box">
                <span>처리 메모</span>
                <textarea v-model.trim="resolveNotes[report.id]" rows="3" placeholder="처리 결과를 남겨주세요."></textarea>
              </label>
              <div class="template-row">
                <button
                  v-for="template in reportTemplates"
                  :key="template"
                  type="button"
                  class="template-chip"
                  @click="applyReportTemplate(report.id, template)"
                >
                  {{ template }}
                </button>
              </div>
              <button class="primary-button compact" @click="resolveReport(report.id)" :disabled="report.status === 'RESOLVED'">처리 완료</button>
            </article>
          </div>
          <p v-else class="empty-state">조건에 맞는 신고가 없습니다.</p>

          <div class="section-head blocked-head">
            <div>
              <h2>재가입 차단 목록</h2>
              <p>차단된 이메일과 전화번호를 확인하고 필요 시 개별 해제합니다.</p>
            </div>
          </div>

          <div v-if="blockedIdentities.length" class="report-list">
            <article v-for="identity in blockedIdentities" :key="identity.id" class="report-card">
              <div class="report-head">
                <strong>#{{ identity.id }} · {{ identity.identityType }}</strong>
                <span class="report-status">{{ identity.active ? 'ACTIVE' : 'RELEASED' }}</span>
              </div>
              <p class="report-meta">식별값 {{ identity.identityValue }}</p>
              <p class="report-meta">대상 회원 {{ identity.sourceUserNickname || '-' }} / {{ identity.sourceUserEmail || '-' }}</p>
              <p class="detail-copy">{{ identity.reason || '운영 사유 없음' }}</p>
              <p class="report-meta">등록일 {{ formatDateTime(identity.createdAt) }}</p>
              <button class="ghost-button compact" @click="releaseBlockedIdentity(identity.id)">차단 해제</button>
            </article>
          </div>
          <p v-else class="empty-state">현재 활성화된 재가입 차단 식별값이 없습니다.</p>
        </section>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import api from "../../api/api";

const adminKey = ref(localStorage.getItem("adminReviewKey") || "");
const riskStatus = ref("WATCH");
const reportStatus = ref("OPEN");
const identityType = ref("ALL");
const searchKeyword = ref("");
const summary = ref(null);
const riskUsers = ref([]);
const reports = ref([]);
const blockedIdentities = ref([]);
const riskLogs = ref({});
const resolveNotes = ref({});
const reviewNotes = ref({});
const expandedUserId = ref(null);
const loading = ref(false);
const message = ref("");
const errorMessage = ref("");

const reviewTemplates = [
  "외부 메신저 유도 정황 확인",
  "투자·송금 유도 패턴 확인",
  "사칭 의심으로 이용 제한 유지",
  "재검토 결과 정상 사용자로 확인",
];

const reportTemplates = [
  "신고 내용 확인 후 경고 조치",
  "신고 내용 확인 후 이용 제한 처리",
  "증거 부족으로 모니터링 유지",
  "중복 신고 정리 완료",
];

const getHeaders = () => ({ "X-Admin-Key": adminKey.value });
const persistAdminKey = () => localStorage.setItem("adminReviewKey", adminKey.value);

const loadDashboard = async () => {
  loading.value = true;
  message.value = "";
  errorMessage.value = "";

  try {
    persistAdminKey();
    const [summaryRes, usersRes, reportsRes, blockedRes] = await Promise.all([
      api.get(`/admin/safety/summary`, { headers: getHeaders() }),
      api.get(`/admin/safety/users`, { headers: getHeaders(), params: { riskStatus: riskStatus.value, q: searchKeyword.value } }),
      api.get(`/admin/safety/reports`, { headers: getHeaders(), params: { status: reportStatus.value } }),
      api.get(`/admin/safety/blocked-identities`, { headers: getHeaders(), params: { q: searchKeyword.value, identityType: identityType.value } }),
    ]);

    summary.value = summaryRes.data;
    riskUsers.value = usersRes.data;
    reports.value = reportsRes.data;
    blockedIdentities.value = blockedRes.data;
    resolveNotes.value = Object.fromEntries(reports.value.map((report) => [report.id, report.adminNote || ""]));
    reviewNotes.value = Object.fromEntries(riskUsers.value.map((user) => [user.userId, user.adminMemo || ""]));
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "스캠 모니터 데이터를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const toggleLogs = async (userId) => {
  if (expandedUserId.value === userId) {
    expandedUserId.value = null;
    return;
  }
  expandedUserId.value = userId;
  if (riskLogs.value[userId]) return;

  try {
    const { data } = await api.get(`/admin/safety/users/${userId}/logs`, { headers: getHeaders() });
    riskLogs.value = { ...riskLogs.value, [userId]: data };
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "위험 로그를 불러오지 못했습니다.";
  }
};

const applyReviewTemplate = (userId, template) => {
  reviewNotes.value[userId] = template;
};

const applyReportTemplate = (reportId, template) => {
  resolveNotes.value[reportId] = template;
};

const reviewRiskUser = async (userId, action) => {
  const adminNote = reviewNotes.value[userId];
  if (!adminNote) {
    alert("재검토 메모를 먼저 입력해주세요.");
    return;
  }

  try {
    const { data } = await api.put(`/admin/safety/users/${userId}/review`, { action, adminNote }, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "위험 계정 재검토 처리에 실패했습니다.";
  }
};

const resolveReport = async (reportId) => {
  const adminNote = resolveNotes.value[reportId];
  if (!adminNote) {
    alert("처리 메모를 먼저 입력해주세요.");
    return;
  }

  try {
    const { data } = await api.put(`/admin/safety/reports/${reportId}/resolve`, { adminNote }, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "신고 처리 상태를 변경하지 못했습니다.";
  }
};

const releaseBlockedIdentity = async (blockedIdentityId) => {
  try {
    const { data } = await api.put(`/admin/safety/blocked-identities/${blockedIdentityId}/release`, {}, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "재가입 차단 해제에 실패했습니다.";
  }
};

const reasonLabel = (value) => ({ INVESTMENT: "투자·금전 유도", EXTERNAL_CONTACT: "외부 메신저 유도", IMPERSONATION: "사칭 의심", HARASSMENT: "부적절한 접근", OTHER: "기타" }[value] || value);

const riskTypeLabel = (value) => ({
  ADMIN_REVIEW: "운영자 검토",
  IDENTITY_BLOCKED: "재가입 차단",
  IDENTITY_UNBLOCKED: "재가입 차단 해제",
  MESSAGE_PATTERN: "메시지 패턴 탐지",
  REPORT_ACCUMULATED: "신고 누적",
})(value) || value;

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
  padding: 40px 20px 72px;
  background:
    radial-gradient(circle at 20% 10%, rgba(255, 214, 191, 0.95) 0%, rgba(255, 214, 191, 0) 30%),
    radial-gradient(circle at 80% 0%, rgba(241, 153, 118, 0.45) 0%, rgba(241, 153, 118, 0) 32%),
    linear-gradient(180deg, #fff8f3 0%, #fdebe2 42%, #f6d2bf 100%);
  font-family: "SUIT Variable", "Pretendard", sans-serif;
}

.admin-card {
  width: min(1240px, 100%);
  margin: 0 auto;
  padding: 32px;
  border-radius: 32px;
  background: rgba(255, 252, 249, 0.78);
  border: 1px solid rgba(239, 208, 193, 0.72);
  box-shadow: 0 25px 60px rgba(109, 57, 41, 0.12);
  backdrop-filter: blur(14px);
}

.page-head,
.section-head,
.risk-head,
.report-head,
.log-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1,
h2,
strong {
  color: #2f211d;
}

h1 {
  margin: 0;
  font-size: clamp(2rem, 3vw, 2.8rem);
}

.sub-copy,
.detail-copy,
.empty-state,
.report-meta {
  color: #6f564d;
  line-height: 1.65;
}

.toolbar,
.summary-grid,
.layout-grid,
.meta-grid {
  display: grid;
  gap: 16px;
}

.toolbar {
  margin-top: 28px;
  grid-template-columns: 1.15fr 0.75fr 1fr 0.75fr 0.85fr auto;
  align-items: stretch;
}

label {
  display: grid;
  gap: 8px;
  color: #543932;
  font-weight: 600;
}

input,
select,
textarea {
  border: 1px solid #e5c2b2;
  border-radius: 16px;
  padding: 14px 16px;
  font-size: 15px;
  background: rgba(255, 255, 255, 0.92);
  font-family: inherit;
}

textarea {
  resize: vertical;
}

.primary-button,
.ghost-button {
  border-radius: 999px;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.primary-button {
  border: none;
  background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%);
  color: #fff;
  box-shadow: 0 14px 30px rgba(213, 111, 78, 0.18);
}

.ghost-button {
  border: 1px solid rgba(207, 150, 128, 0.36);
  background: rgba(255, 248, 243, 0.82);
  color: #6a4137;
}

.ghost-button.danger {
  color: #a13a2b;
  border-color: #efb6aa;
}

.primary-button:hover,
.ghost-button:hover {
  transform: translateY(-1px);
}

.compact {
  padding: 11px 15px;
}

.message {
  margin-top: 16px;
  font-weight: 700;
}

.success { color: #1f7a43; }
.error { color: #b72f2f; }

.summary-grid {
  margin-top: 22px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.layout-grid {
  margin-top: 28px;
  grid-template-columns: 1.15fr 0.95fr;
  align-items: start;
}

.summary-card,
.risk-card,
.report-card,
.logs-box {
  border-radius: 24px;
  border: 1px solid rgba(235, 209, 199, 0.85);
  padding: 22px;
  background: rgba(255, 255, 255, 0.88);
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 1.9rem;
}

.amber { background: linear-gradient(180deg, rgba(255,239,229,0.98) 0%, rgba(255,223,205,0.96) 100%); }
.soft { background: linear-gradient(180deg, rgba(255,245,238,0.95) 0%, rgba(255,234,225,0.96) 100%); }
.sand { background: linear-gradient(180deg, rgba(255,249,240,0.95) 0%, rgba(247,230,214,0.96) 100%); }
.mint { background: linear-gradient(180deg, rgba(242,252,245,0.97) 0%, rgba(228,244,232,0.96) 100%); }

.risk-list,
.report-list {
  margin-top: 16px;
  display: grid;
  gap: 16px;
}

.badge-group,
.action-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.score-badge,
.status-badge,
.report-status {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.score-badge {
  background: #fff3e9;
  color: #914f30;
}

.status-badge.watch { background: #fff0dc; color: #9b5f11; }
.status-badge.high_risk { background: #ffe8e2; color: #ad3e2e; }

.meta-grid {
  margin-top: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.review-box {
  margin-top: 14px;
}

.template-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-chip {
  border: 1px solid rgba(207, 150, 128, 0.42);
  background: rgba(255, 247, 240, 0.9);
  color: #7a4b3f;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.template-chip:hover {
  background: rgba(250, 231, 220, 0.95);
}

.blocked-head {
  margin-top: 26px;
}

.logs-box {
  margin-top: 14px;
  background: #fff8f3;
}

.log-item + .log-item {
  margin-top: 10px;
}

@media (max-width: 1100px) {
  .toolbar,
  .summary-grid,
  .layout-grid,
  .meta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .admin-page {
    padding: 22px 14px 40px;
  }

  .admin-card {
    padding: 22px;
    border-radius: 24px;
  }

  .page-head,
  .section-head,
  .risk-head,
  .report-head,
  .log-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .badge-group,
  .action-grid {
    width: 100%;
  }

  .primary-button,
  .ghost-button {
    width: 100%;
    text-align: center;
  }
}
</style>
