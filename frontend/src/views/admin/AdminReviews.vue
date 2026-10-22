<template>
  <section class="admin-page">
    <div class="dashboard-shell">
      <header class="hero-card">
        <div class="hero-copy">
          <p class="eyebrow">Admin Dashboard</p>
          <h1>운영 대시보드</h1>
          <p class="sub-copy">가입 심사, 위험 계정, 신고 처리를 상단 카테고리 중심으로 한 화면에서 관리합니다.</p>
        </div>
        <div class="hero-actions">
          <label class="field">
            <span>운영자 키</span>
            <input v-model.trim="adminKey" type="password" placeholder="X-Admin-Key 입력" />
          </label>
          <div class="hero-buttons">
            <button class="primary-button" @click="loadDashboard" :disabled="loading">{{ loading ? "불러오는 중..." : "전체 새로고침" }}</button>
            <RouterLink class="ghost-button" to="/admin/safety">안전 모니터 단독 보기</RouterLink>
          </div>
        </div>
      </header>

      <div class="category-grid">
        <button v-for="item in categoryCards" :key="item.key" class="category-card" :class="item.theme" @click="scrollToSection(item.key)">
          <span>{{ item.label }}</span>
          <strong>{{ item.count }}</strong>
          <small>{{ item.description }}</small>
        </button>
      </div>

      <nav class="sticky-tabs">
        <button v-for="item in categoryCards" :key="item.key" class="tab-chip" :class="{ active: activeSection === item.key }" @click="scrollToSection(item.key)">
          {{ item.label }}
          <em>{{ item.count }}</em>
        </button>
      </nav>

      <p v-if="message" class="message success">{{ message }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <section :id="sectionIds.review" class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Review Center</p>
            <h2>가입 심사</h2>
            <p class="sub-copy">사진, 프로필 완성도, 운영자 메모와 이력을 함께 보고 승인 또는 반려합니다.</p>
          </div>
          <div class="section-tools">
            <span class="pill-count">현재 {{ candidates.length }}명</span>
            <button class="ghost-button compact" @click="toggleSection('review')">{{ collapsed.review ? "펼치기" : "접기" }}</button>
          </div>
        </div>

        <div class="mini-summary-grid">
          <article class="mini-summary warm"><span>심사 대기</span><strong>{{ reviewPendingCount }}명</strong></article>
          <article class="mini-summary blush"><span>반려 상태</span><strong>{{ reviewRejectedCount }}명</strong></article>
          <article class="mini-summary sand"><span>승인 완료</span><strong>{{ reviewActiveCount }}명</strong></article>
          <article class="mini-summary alert"><span>마감 임박</span><strong>{{ reviewDueSoonCount }}명</strong></article>
        </div>

        <div v-show="!collapsed.review" class="section-body">
          <div class="toolbar review-toolbar">
            <label class="field">
              <span>상태</span>
              <select v-model="statusFilter">
                <option value="PENDING_REVIEW">심사 대기</option>
                <option value="REJECTED">반려</option>
                <option value="ACTIVE">승인 완료</option>
              </select>
            </label>
            <label class="field">
              <span>성별</span>
              <select v-model="genderFilter">
                <option value="ALL">전체</option>
                <option value="MALE">남성</option>
                <option value="FEMALE">여성</option>
              </select>
            </label>
            <div class="template-row">
              <button
                v-for="item in periodOptions"
                :key="item.value"
                class="ghost-button compact"
                :class="{ active: periodFilter === item.value }"
                type="button"
                @click="selectPeriod(item.value)"
              >
                {{ item.label }}
              </button>
            </div>
            <label class="field search-wide">
              <span>검색</span>
              <input v-model.trim="searchKeyword" type="text" placeholder="이메일, 닉네임, 지역, 직업, 소개" />
            </label>
            <label class="checkbox-field">
              <input v-model="dueSoonOnly" type="checkbox" />
              <span>마감 임박만</span>
            </label>
            <label class="checkbox-field">
              <input v-model="profileCompleteOnly" type="checkbox" />
              <span>프로필 완성만</span>
            </label>
          </div>

          <div class="template-box">
            <strong>반려 사유 템플릿</strong>
            <div class="template-editor-grid">
              <label v-for="item in templateConfig" :key="item.key" class="field">
                <span>{{ item.label }}</span>
                <textarea v-model.trim="rejectTemplates[item.key]" rows="3"></textarea>
              </label>
            </div>
            <div class="template-row">
              <button class="ghost-button compact" @click="saveTemplates">템플릿 저장</button>
              <button class="ghost-button compact" @click="resetTemplates">기본값 복원</button>
            </div>
          </div>

          <div v-if="candidates.length" class="candidate-list">
            <article v-for="candidate in candidates" :key="candidate.userId" class="candidate-card">
              <div class="candidate-head">
                <div>
                  <strong>{{ candidate.nickname }}</strong>
                  <p>{{ candidate.email }}</p>
                </div>
                <div class="badge-row">
                  <span class="status-badge">{{ candidate.status }}</span>
                  <span class="risk-badge" :class="riskClass(candidate.fraudReviewStatus)">{{ candidate.fraudReviewStatus }} · {{ candidate.fraudRiskScore ?? 0 }}점</span>
                </div>
              </div>

              <div class="info-grid">
                <div><span>성별/생년월일</span><strong>{{ candidate.gender }} / {{ candidate.birthDate }}</strong></div>
                <div><span>지역</span><strong>{{ candidate.region || "-" }}</strong></div>
                <div><span>직업</span><strong>{{ candidate.job || "-" }}</strong></div>
                <div><span>가입 방식</span><strong>{{ candidate.provider }}</strong></div>
              </div>

              <div class="notice-grid">
                <div class="review-rule" :class="{ incomplete: !candidate.profileComplete }"><strong>프로필 필수 항목</strong><p>{{ candidate.profileComplete ? "완료" : "미완료" }}</p></div>
                <div class="review-rule" :class="{ incomplete: candidate.images.length < 2 }"><strong>사진 수</strong><p>{{ candidate.images.length }}장 / 최소 2장</p></div>
                <div class="review-rule" :class="deadlineRuleClass(candidate)"><strong>자동 정리 기한</strong><p>{{ candidate.reviewDeadlineAt ? formatDate(candidate.reviewDeadlineAt) : "기한 없음" }}</p><small v-if="candidate.reviewDeadlineAt">남은 기간 {{ candidate.remainingDays }}일</small></div>
              </div>

              <div class="card-actions">
                <button class="ghost-button compact" @click="selectedCandidate = candidate">상세 검토</button>
                <button class="ghost-button compact" @click="toggleHistory(candidate.userId)">{{ expandedHistoryUserId === candidate.userId ? "이력 닫기" : "이력 보기" }}</button>
                <button class="ghost-button compact" @click="saveMemo(candidate.userId)">메모 저장</button>
              </div>

              <label class="field"><span>운영자 메모</span><textarea v-model.trim="adminMemos[candidate.userId]" rows="3" placeholder="내부 메모를 남겨주세요."></textarea></label>
              <label class="field"><span>반려 사유</span><textarea v-model.trim="rejectComments[candidate.userId]" rows="3" placeholder="사용자에게 보여줄 반려 사유를 입력해주세요."></textarea></label>

              <div class="template-row">
                <button class="ghost-button compact" @click="fillRejectComment(candidate.userId, rejectTemplates.face)">얼굴 식별</button>
                <button class="ghost-button compact" @click="fillRejectComment(candidate.userId, rejectTemplates.count)">사진 부족</button>
                <button class="ghost-button compact" @click="fillRejectComment(candidate.userId, rejectTemplates.profile)">프로필 부족</button>
                <button class="ghost-button compact" @click="fillRejectComment(candidate.userId, rejectTemplates.policy)">정책 위반</button>
              </div>

              <div class="decision-row">
                <button class="approve-button" :disabled="candidate.images.length < 2 || !candidate.profileComplete" @click="approveCandidate(candidate.userId)">승인</button>
                <button class="reject-button" @click="rejectCandidate(candidate.userId)">반려</button>
              </div>

              <div v-if="expandedHistoryUserId === candidate.userId" class="history-box">
                <p v-if="historyLoading" class="muted-copy">이력을 불러오는 중입니다.</p>
                <template v-else>
                  <article v-for="history in histories[candidate.userId] || []" :key="history.id" class="history-item">
                    <div class="history-head"><strong>{{ history.actionType }}</strong><span>{{ formatDateTime(history.createdAt) }}</span></div>
                    <p>{{ history.detail }}</p>
                  </article>
                  <p v-if="!(histories[candidate.userId] || []).length" class="muted-copy">표시할 이력이 없습니다.</p>
                </template>
              </div>
            </article>
          </div>

          <p v-else-if="loaded" class="empty-state">조건에 맞는 심사 대상이 없습니다.</p>
        </div>
      </section>

      <section :id="sectionIds.risk" class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Safety Center</p>
            <h2>위험 계정</h2>
            <p class="sub-copy">프로필 위험도, 탐지 로그, 계정 제한 상태를 한 번에 보고 바로 재검토합니다.</p>
          </div>
          <div class="section-tools">
            <span class="pill-count">현재 {{ riskUsers.length }}명</span>
            <button class="ghost-button compact" @click="toggleSection('risk')">{{ collapsed.risk ? "펼치기" : "접기" }}</button>
          </div>
        </div>

        <div class="mini-summary-grid">
          <article class="mini-summary amber"><span>고위험 회원</span><strong>{{ highRiskCount }}명</strong></article>
          <article class="mini-summary soft"><span>주의 회원</span><strong>{{ watchCount }}명</strong></article>
          <article class="mini-summary sand"><span>열린 신고</span><strong>{{ openReportCount }}건</strong></article>
          <article class="mini-summary mint"><span>처리 완료</span><strong>{{ resolvedReportCount }}건</strong></article>
        </div>

        <div v-show="!collapsed.risk" class="section-body">
          <div class="toolbar risk-toolbar">
            <label class="field">
              <span>위험 상태</span>
              <select v-model="riskStatus">
                <option value="WATCH">주의</option>
                <option value="HIGH_RISK">고위험</option>
                <option value="ALL">전체</option>
              </select>
            </label>
            <label class="field search-wide">
              <span>검색</span>
              <input v-model.trim="riskSearchKeyword" type="text" placeholder="이메일, 닉네임, 메모, 안내 문구" />
            </label>
          </div>

          <div v-if="riskUsers.length" class="risk-list">
            <article v-for="user in riskUsers" :key="user.userId" class="risk-card">
              <div class="risk-head">
                <div>
                  <strong>{{ user.nickname }}</strong>
                  <p>{{ user.email }}</p>
                </div>
                <div class="badge-row">
                  <span class="score-badge">{{ user.fraudRiskScore }}점</span>
                  <span class="status-badge" :class="user.fraudReviewStatus.toLowerCase()">{{ user.fraudReviewStatus }}</span>
                </div>
              </div>

              <div class="info-grid">
                <div><span>가입 방식</span><strong>{{ user.provider }}</strong></div>
                <div><span>계정 상태</span><strong>{{ user.status }}</strong></div>
                <div><span>지역</span><strong>{{ user.region || "-" }}</strong></div>
                <div><span>열린 신고</span><strong>{{ user.openReportCount }}건</strong></div>
              </div>

              <p class="detail-copy"><strong>최근 탐지</strong><br />{{ user.latestRiskDetail }}</p>
              <p class="detail-copy"><strong>사용자 안내</strong><br />{{ user.reviewComment || "없음" }}</p>
              <p class="detail-copy"><strong>운영 메모</strong><br />{{ user.adminMemo || "없음" }}</p>

              <label class="field"><span>재검토 메모</span><textarea v-model.trim="reviewNotes[user.userId]" rows="3" placeholder="재검토 근거를 남겨주세요."></textarea></label>

              <div class="action-grid">
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'MARK_NORMAL')">정상화</button>
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'MARK_WATCH')">주의 유지</button>
                <button class="ghost-button compact danger" @click="reviewRiskUser(user.userId, 'SUSPEND_ACCOUNT')">이용 제한</button>
                <button class="ghost-button compact" @click="reviewRiskUser(user.userId, 'RELEASE_ACCOUNT')">제한 해제</button>
                <button class="ghost-button compact" @click="toggleLogs(user.userId)">{{ expandedUserId === user.userId ? "위험 로그 닫기" : "위험 로그 보기" }}</button>
              </div>

              <div v-if="expandedUserId === user.userId" class="logs-box">
                <article v-for="log in riskLogs[user.userId] || []" :key="log.id" class="log-item">
                  <div class="history-head"><strong>{{ log.riskType }}</strong><span>{{ formatDateTime(log.createdAt) }}</span></div>
                  <p>{{ log.detail }}</p>
                  <small>점수 {{ log.score }}</small>
                </article>
                <p v-if="!(riskLogs[user.userId] || []).length" class="muted-copy">표시할 위험 로그가 없습니다.</p>
              </div>
            </article>
          </div>

          <p v-else class="empty-state">조건에 맞는 위험 회원이 없습니다.</p>
        </div>
      </section>

      <section :id="sectionIds.report" class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Report Desk</p>
            <h2>신고 처리</h2>
            <p class="sub-copy">사용자 신고를 검토하고 운영 메모를 남긴 뒤 처리 완료 상태로 전환합니다.</p>
          </div>
          <div class="section-tools">
            <span class="pill-count">현재 {{ reports.length }}건</span>
            <button class="ghost-button compact" @click="toggleSection('report')">{{ collapsed.report ? "펼치기" : "접기" }}</button>
          </div>
        </div>

        <div v-show="!collapsed.report" class="section-body">
          <div class="toolbar report-toolbar">
            <label class="field">
              <span>신고 상태</span>
              <select v-model="reportStatus">
                <option value="OPEN">처리 대기</option>
                <option value="RESOLVED">처리 완료</option>
              </select>
            </label>
          </div>

          <div v-if="reports.length" class="report-list">
            <article v-for="report in reports" :key="report.id" class="report-card">
              <div class="report-head">
                <strong>#{{ report.id }} · {{ reasonLabel(report.reasonCode) }}</strong>
                <span class="report-status">{{ report.status }}</span>
              </div>
              <p class="report-meta">신고자 {{ report.reporterUserId }} → 대상 {{ report.reportedUserId }}</p>
              <p class="detail-copy">{{ report.detail }}</p>
              <label class="field"><span>처리 메모</span><textarea v-model.trim="resolveNotes[report.id]" rows="3" placeholder="처리 결과를 남겨주세요."></textarea></label>
              <button class="primary-button compact" @click="resolveReport(report.id)" :disabled="report.status === 'RESOLVED'">처리 완료</button>
            </article>
          </div>

          <p v-else class="empty-state">조건에 맞는 신고가 없습니다.</p>
        </div>
      </section>

      <section :id="sectionIds.support" class="section-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Support Desk</p>
            <h2>고객 문의</h2>
            <p class="sub-copy">고객센터에서 접수된 문의를 확인하고 답변 상태를 관리합니다.</p>
          </div>
          <div class="section-tools">
            <span class="pill-count">현재 {{ inquiries.length }}건</span>
            <button class="ghost-button compact" @click="toggleSection('support')">{{ collapsed.support ? "펼치기" : "접기" }}</button>
          </div>
        </div>

        <div class="mini-summary-grid">
          <article class="mini-summary warm"><span>접수 대기</span><strong>{{ openInquiryCount }}건</strong></article>
          <article class="mini-summary amber"><span>확인 중</span><strong>{{ inProgressInquiryCount }}건</strong></article>
          <article class="mini-summary mint"><span>답변 완료</span><strong>{{ resolvedInquiryCount }}건</strong></article>
          <article class="mini-summary sand"><span>종료</span><strong>{{ closedInquiryCount }}건</strong></article>
        </div>

        <div v-show="!collapsed.support" class="section-body">
          <div class="toolbar report-toolbar">
            <label class="field">
              <span>문의 상태</span>
              <select v-model="inquiryStatus">
                <option value="OPEN">접수 대기</option>
                <option value="IN_PROGRESS">확인 중</option>
                <option value="RESOLVED">답변 완료</option>
                <option value="CLOSED">종료</option>
                <option value="ALL">전체</option>
              </select>
            </label>
            <label class="field search-wide">
              <span>검색</span>
              <input v-model.trim="inquirySearchKeyword" type="text" placeholder="이메일, 닉네임, 문의 내용, 답변" />
            </label>
          </div>

          <div v-if="inquiries.length" class="report-list">
            <article v-for="inquiry in inquiries" :key="inquiry.id" class="report-card">
              <div class="report-head">
                <strong>#{{ inquiry.id }} · {{ supportCategoryLabel(inquiry.category) }}</strong>
                <span class="report-status">{{ supportStatusLabel(inquiry.status) }}</span>
              </div>
              <p class="report-meta">{{ inquiry.nickname }} · {{ inquiry.email }} · {{ formatDateTime(inquiry.createdAt) }}</p>
              <p class="detail-copy">{{ inquiry.question }}</p>
              <label class="field">
                <span>처리 상태</span>
                <select v-model="inquiryStatuses[inquiry.id]">
                  <option value="OPEN">접수 대기</option>
                  <option value="IN_PROGRESS">확인 중</option>
                  <option value="RESOLVED">답변 완료</option>
                  <option value="CLOSED">종료</option>
                </select>
              </label>
              <label class="field">
                <span>운영자 답변</span>
                <textarea v-model.trim="inquiryReplies[inquiry.id]" rows="3" placeholder="고객에게 전달할 답변을 입력해주세요."></textarea>
              </label>
              <button class="primary-button compact" @click="saveInquiry(inquiry.id)">문의 상태 저장</button>
            </article>
          </div>

          <p v-else class="empty-state">조건에 맞는 고객 문의가 없습니다.</p>
        </div>
      </section>
    </div>

    <div v-if="selectedCandidate" class="modal-backdrop" @click.self="selectedCandidate = null">
      <div class="modal-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Member Profile</p>
            <h2>{{ selectedCandidate.nickname }} 상세 검토</h2>
          </div>
          <button class="ghost-button compact" @click="selectedCandidate = null">닫기</button>
        </div>

        <div class="detail-grid">
          <article class="detail-card">
            <h3>기본 정보</h3>
            <dl>
              <div><dt>이메일</dt><dd>{{ selectedCandidate.email }}</dd></div>
              <div><dt>가입 방식</dt><dd>{{ selectedCandidate.provider }}</dd></div>
              <div><dt>위험 상태</dt><dd>{{ selectedCandidate.fraudReviewStatus }} / {{ selectedCandidate.fraudRiskScore ?? 0 }}점</dd></div>
              <div><dt>가입일</dt><dd>{{ formatDateTime(selectedCandidate.createdAt) }}</dd></div>
            </dl>
          </article>

          <article class="detail-card">
            <h3>프로필 내용</h3>
            <p><strong>자기소개</strong><br />{{ selectedCandidate.introduction || "미입력" }}</p>
            <p><strong>성격</strong><br />{{ selectedCandidate.personality || "미입력" }}</p>
            <p><strong>이상형</strong><br />{{ selectedCandidate.idealType || "미입력" }}</p>
          </article>
        </div>

        <div class="image-grid" v-if="selectedCandidate.images.length">
          <article v-for="image in selectedCandidate.images" :key="image.id" class="image-card">
            <img :src="toAbsoluteImageUrl(image.imageUrl)" alt="review image" />
            <span>{{ image.imageOrder }}번 사진{{ image.isMain ? " · 대표" : "" }}</span>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import api from "../../api/api";

const apiBaseURL = String(api.defaults.baseURL || "");
const assetBaseURL = apiBaseURL.endsWith("/api") ? apiBaseURL.slice(0, -4) : apiBaseURL;
const templateStorageKey = "adminRejectTemplates";

const defaultRejectTemplates = {
  face: "대표 사진에서 얼굴 확인이 어려워 반려되었습니다. 밝고 선명한 정면 사진으로 다시 등록해주세요.",
  count: "심사 기준에 필요한 사진 수가 부족해 반려되었습니다. 대표 사진 포함 최소 2장을 등록해주세요.",
  profile: "프로필 필수 항목이 충분히 작성되지 않아 반려되었습니다. 직업, MBTI, 성격, 이상형, 자기소개를 보완해주세요.",
  policy: "운영 정책에 맞지 않는 사진 또는 정보가 포함되어 반려되었습니다. 정책에 맞게 수정 후 다시 등록해주세요.",
};

const templateConfig = [
  { key: "face", label: "얼굴 식별 어려움" },
  { key: "count", label: "사진 수 부족" },
  { key: "profile", label: "프로필 내용 부족" },
  { key: "policy", label: "운영 정책 위반" },
];

const sectionIds = {
  review: "review-section",
  risk: "risk-section",
  report: "report-section",
  support: "support-section",
};

const adminKey = ref(localStorage.getItem("adminReviewKey") || "");
const statusFilter = ref("PENDING_REVIEW");
const genderFilter = ref("ALL");
const periodFilter = ref("LATEST");
const searchKeyword = ref("");
const dueSoonOnly = ref(false);
const profileCompleteOnly = ref(false);
const riskStatus = ref("WATCH");
const riskSearchKeyword = ref("");
const reportStatus = ref("OPEN");
const inquiryStatus = ref("OPEN");
const inquirySearchKeyword = ref("");
const candidates = ref([]);
const reviewSummary = ref(null);
const safetySummary = ref(null);
const supportSummary = ref(null);
const riskUsers = ref([]);
const reports = ref([]);
const inquiries = ref([]);
const rejectComments = ref({});
const adminMemos = ref({});
const histories = ref({});
const expandedHistoryUserId = ref(null);
const historyLoading = ref(false);
const rejectTemplates = ref(loadRejectTemplates());
const selectedCandidate = ref(null);
const riskLogs = ref({});
const resolveNotes = ref({});
const reviewNotes = ref({});
const inquiryReplies = ref({});
const inquiryStatuses = ref({});
const expandedUserId = ref(null);
const loading = ref(false);
const loaded = ref(false);
const message = ref("");
const errorMessage = ref("");
const activeSection = ref("review");
const collapsed = ref({ review: false, risk: false, report: false, support: false });
const periodOptions = [
  { value: "LATEST", label: "최신순" },
  { value: "MONTH", label: "1개월" },
  { value: "YEAR", label: "1년" },
];

function loadRejectTemplates() {
  try {
    const stored = localStorage.getItem(templateStorageKey);
    return stored ? { ...defaultRejectTemplates, ...JSON.parse(stored) } : { ...defaultRejectTemplates };
  } catch {
    return { ...defaultRejectTemplates };
  }
}

const getHeaders = () => ({ "X-Admin-Key": adminKey.value });
const persistAdminKey = () => localStorage.setItem("adminReviewKey", adminKey.value);

const selectPeriod = async (period) => {
  periodFilter.value = period;
  await loadDashboard();
};

const reviewPendingCount = computed(() => reviewSummary.value?.pendingReviewCount ?? 0);
const reviewRejectedCount = computed(() => reviewSummary.value?.rejectedCount ?? 0);
const reviewActiveCount = computed(() => reviewSummary.value?.activeCount ?? 0);
const reviewDueSoonCount = computed(() => reviewSummary.value?.dueSoonCount ?? 0);
const highRiskCount = computed(() => safetySummary.value?.highRiskUserCount ?? 0);
const watchCount = computed(() => safetySummary.value?.watchUserCount ?? 0);
const openReportCount = computed(() => safetySummary.value?.openReportCount ?? 0);
const resolvedReportCount = computed(() => safetySummary.value?.resolvedReportCount ?? 0);
const openInquiryCount = computed(() => supportSummary.value?.openCount ?? 0);
const inProgressInquiryCount = computed(() => supportSummary.value?.inProgressCount ?? 0);
const resolvedInquiryCount = computed(() => supportSummary.value?.resolvedCount ?? 0);
const closedInquiryCount = computed(() => supportSummary.value?.closedCount ?? 0);

// Top category cards mirror the three main admin workflows on one page.
const categoryCards = computed(() => [
  { key: "review", label: "가입 심사", count: `${reviewPendingCount.value}명`, description: "사진과 프로필 심사를 우선 처리합니다.", theme: "warm" },
  { key: "risk", label: "위험 계정", count: `${highRiskCount.value + watchCount.value}명`, description: "스캠 의심 계정을 재검토하고 제한 상태를 관리합니다.", theme: "amber" },
  { key: "report", label: "신고 처리", count: `${openReportCount.value}건`, description: "접수된 사용자 신고를 확인하고 처리 완료로 전환합니다.", theme: "mint" },
  { key: "support", label: "고객 문의", count: `${openInquiryCount.value + inProgressInquiryCount.value}건`, description: "고객센터에서 접수된 문의에 답변을 남깁니다.", theme: "soft" },
]);

// Load review, safety, and report data together so the admin page stays in sync.
const loadDashboard = async () => {
  loading.value = true;
  message.value = "";
  errorMessage.value = "";

  try {
    persistAdminKey();
    const [reviewSummaryRes, candidatesRes, safetySummaryRes, usersRes, reportsRes, supportSummaryRes, inquiriesRes] = await Promise.all([
      api.get(`/admin/reviews/summary`, { headers: getHeaders() }),
      api.get(`/admin/reviews`, {
        headers: getHeaders(),
        params: {
          status: statusFilter.value,
          dueSoonOnly: dueSoonOnly.value,
          q: searchKeyword.value,
          gender: genderFilter.value,
          profileCompleteOnly: profileCompleteOnly.value,
          period: periodFilter.value,
        },
      }),
      api.get(`/admin/safety/summary`, { headers: getHeaders() }),
      api.get(`/admin/safety/users`, { headers: getHeaders(), params: { riskStatus: riskStatus.value, q: riskSearchKeyword.value } }),
      api.get(`/admin/safety/reports`, { headers: getHeaders(), params: { status: reportStatus.value } }),
      api.get(`/admin/support/summary`, { headers: getHeaders() }),
      api.get(`/admin/support/inquiries`, { headers: getHeaders(), params: { status: inquiryStatus.value, q: inquirySearchKeyword.value } }),
    ]);

    reviewSummary.value = reviewSummaryRes.data;
    candidates.value = candidatesRes.data;
    safetySummary.value = safetySummaryRes.data;
    supportSummary.value = supportSummaryRes.data;
    riskUsers.value = usersRes.data;
    reports.value = reportsRes.data;
    inquiries.value = inquiriesRes.data;
    rejectComments.value = Object.fromEntries(candidates.value.map((candidate) => [candidate.userId, candidate.reviewComment || ""]));
    adminMemos.value = Object.fromEntries(candidates.value.map((candidate) => [candidate.userId, candidate.adminMemo || ""]));
    resolveNotes.value = Object.fromEntries(reports.value.map((report) => [report.id, report.adminNote || ""]));
    reviewNotes.value = Object.fromEntries(riskUsers.value.map((user) => [user.userId, user.adminMemo || ""]));
    inquiryReplies.value = Object.fromEntries(inquiries.value.map((inquiry) => [inquiry.id, inquiry.adminReply || ""]));
    inquiryStatuses.value = Object.fromEntries(inquiries.value.map((inquiry) => [inquiry.id, inquiry.status || "OPEN"]));
    loaded.value = true;
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "관리자 대시보드를 불러오지 못했습니다.";
  } finally {
    loading.value = false;
  }
};

const toggleSection = (key) => {
  collapsed.value[key] = !collapsed.value[key];
};

const scrollToSection = (key) => {
  activeSection.value = key;
  document.getElementById(sectionIds[key])?.scrollIntoView({ behavior: "smooth", block: "start" });
};

const toggleHistory = async (userId) => {
  if (expandedHistoryUserId.value === userId) {
    expandedHistoryUserId.value = null;
    return;
  }
  expandedHistoryUserId.value = userId;
  if (histories.value[userId]) return;

  historyLoading.value = true;
  try {
    const { data } = await api.get(`/admin/reviews/${userId}/history`, { headers: getHeaders() });
    histories.value = { ...histories.value, [userId]: data };
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "처리 이력을 불러오지 못했습니다.";
  } finally {
    historyLoading.value = false;
  }
};

const approveCandidate = async (userId) => {
  try {
    const { data } = await api.post(`/admin/reviews/${userId}/approve`, null, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "승인 처리에 실패했습니다.";
  }
};

const rejectCandidate = async (userId) => {
  const reviewComment = rejectComments.value[userId];
  if (!reviewComment) {
    alert("반려 사유를 먼저 입력해주세요.");
    return;
  }

  try {
    const { data } = await api.post(`/admin/reviews/${userId}/reject`, { reviewComment }, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "반려 처리에 실패했습니다.";
  }
};

const saveMemo = async (userId) => {
  const adminMemo = adminMemos.value[userId];
  if (!adminMemo) {
    alert("운영자 메모를 먼저 입력해주세요.");
    return;
  }

  try {
    const { data } = await api.put(`/admin/reviews/${userId}/memo`, { adminMemo }, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "운영자 메모 저장에 실패했습니다.";
  }
};

const saveTemplates = () => {
  localStorage.setItem(templateStorageKey, JSON.stringify(rejectTemplates.value));
  message.value = "반려 사유 템플릿을 저장했습니다.";
};

const resetTemplates = () => {
  rejectTemplates.value = { ...defaultRejectTemplates };
  localStorage.removeItem(templateStorageKey);
  message.value = "반려 사유 템플릿을 기본값으로 되돌렸습니다.";
};

const fillRejectComment = (userId, text) => {
  rejectComments.value[userId] = text;
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

const saveInquiry = async (inquiryId) => {
  const status = inquiryStatuses.value[inquiryId];
  const adminReply = inquiryReplies.value[inquiryId];

  try {
    const { data } = await api.put(`/admin/support/inquiries/${inquiryId}`, { status, adminReply }, { headers: getHeaders() });
    message.value = data.message;
    await loadDashboard();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "고객 문의 저장에 실패했습니다.";
  }
};

const deadlineRuleClass = (candidate) => {
  if (candidate.remainingDays === 0) return "danger";
  if (candidate.remainingDays === 1) return "warning";
  return "";
};

const riskClass = (value) => {
  if (value === "HIGH_RISK") return "high";
  if (value === "WATCH") return "watch";
  return "normal";
};

const reasonLabel = (value) => ({ INVESTMENT: "투자·금전 유도", EXTERNAL_CONTACT: "외부 메신저 유도", IMPERSONATION: "사칭 의심", HARASSMENT: "부적절한 접근", OTHER: "기타" }[value] || value);
const supportCategoryLabel = (value) => ({ GENERAL: "일반 문의", LOGIN: "로그인/회원가입", REVIEW: "사진 심사", PROFILE: "프로필 수정", SAFETY: "스캠/신고" }[value] || value);
const supportStatusLabel = (value) => ({ OPEN: "접수 대기", IN_PROGRESS: "확인 중", RESOLVED: "답변 완료", CLOSED: "종료" }[value] || value);

const toAbsoluteImageUrl = (path) => {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${assetBaseURL}${path}`;
};

const formatDate = (value) => {
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
  padding: 34px 18px 72px;
  background:
    radial-gradient(circle at 14% 0%, rgba(255, 224, 205, 0.94) 0%, rgba(255, 224, 205, 0) 26%),
    radial-gradient(circle at 88% 10%, rgba(246, 162, 130, 0.42) 0%, rgba(246, 162, 130, 0) 30%),
    linear-gradient(180deg, #fff8f3 0%, #fbe9df 42%, #f3d4c2 100%);
  font-family: "SUIT Variable", "Pretendard", sans-serif;
}

.dashboard-shell {
  width: min(1320px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.hero-card,
.section-card,
.modal-card {
  border-radius: 32px;
  border: 1px solid rgba(236, 209, 196, 0.82);
  background: rgba(255, 252, 249, 0.82);
  box-shadow: 0 22px 60px rgba(109, 57, 41, 0.12);
  backdrop-filter: blur(16px);
}

.hero-card {
  padding: 28px;
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
}

.hero-copy h1,
.section-head h2,
.modal-card h2,
.detail-card h3,
strong {
  color: #2f211d;
}

.eyebrow {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.sub-copy,
.empty-state,
.muted-copy,
.detail-copy,
.report-meta,
.candidate-head p,
.risk-head p {
  color: #6f564d;
  line-height: 1.65;
  margin: 0;
}

.hero-actions,
.field,
.toolbar,
.template-editor-grid,
.candidate-list,
.risk-list,
.report-list,
.info-grid,
.notice-grid,
.detail-grid,
.image-grid,
.mini-summary-grid {
  display: grid;
  gap: 14px;
}

.field span,
.checkbox-field span {
  color: #6f5148;
  font-size: 0.92rem;
  font-weight: 700;
}

.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(223, 194, 183, 0.95);
  background: rgba(255, 255, 255, 0.96);
  color: #3e2a25;
  font: inherit;
}

.field textarea {
  resize: vertical;
}

.hero-buttons,
.section-head,
.section-tools,
.candidate-head,
.card-actions,
.decision-row,
.history-head,
.risk-head,
.action-grid,
.report-head,
.template-row,
.badge-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.hero-buttons,
.template-row,
.action-grid,
.card-actions,
.decision-row,
.badge-row {
  flex-wrap: wrap;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.category-card,
.mini-summary {
  text-align: left;
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(235, 209, 199, 0.84);
  background: rgba(255, 255, 255, 0.88);
}

.category-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.category-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(122, 70, 49, 0.12);
}

.category-card span,
.mini-summary span {
  display: block;
  color: #916456;
  font-size: 13px;
  font-weight: 800;
}

.category-card strong,
.mini-summary strong {
  display: block;
  margin-top: 8px;
  font-size: 1.8rem;
}

.category-card small {
  display: block;
  margin-top: 10px;
  color: #6e5950;
  line-height: 1.55;
}

.sticky-tabs {
  position: sticky;
  top: 14px;
  z-index: 20;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px;
  border-radius: 999px;
  background: rgba(255, 249, 245, 0.9);
  border: 1px solid rgba(232, 204, 191, 0.9);
  box-shadow: 0 10px 28px rgba(100, 56, 39, 0.1);
  backdrop-filter: blur(12px);
}

.tab-chip {
  border: none;
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.86);
  color: #6d4a40;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.tab-chip em {
  margin-left: 8px;
  font-style: normal;
  color: #b96b4f;
}

.tab-chip.active {
  background: linear-gradient(135deg, #df7c43 0%, #d8653f 100%);
  color: #fff9f4;
}

.tab-chip.active em {
  color: #fff5ef;
}

.section-card {
  padding: 24px;
}

.section-body {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.pill-count {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 240, 232, 0.92);
  color: #975840;
  font-size: 0.92rem;
  font-weight: 800;
}

.toolbar {
  grid-template-columns: repeat(12, minmax(0, 1fr));
  align-items: end;
}

.review-toolbar .field,
.review-toolbar .search-wide,
.risk-toolbar .field,
.report-toolbar .field {
  grid-column: span 2;
}

.review-toolbar .search-wide,
.risk-toolbar .search-wide {
  grid-column: span 4;
}

.checkbox-field {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
}

.checkbox-field input {
  width: 18px;
  height: 18px;
}

.mini-summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 16px;
}

.template-box,
.candidate-card,
.risk-card,
.report-card,
.detail-card,
.image-card {
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(235, 209, 199, 0.85);
  background: rgba(255, 255, 255, 0.9);
}

.template-editor-grid {
  margin-top: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.candidate-list,
.risk-list,
.report-list {
  grid-template-columns: 1fr;
}

.info-grid,
.notice-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.info-grid span,
.notice-grid span {
  display: block;
  color: #8b675b;
  font-size: 0.84rem;
  margin-bottom: 6px;
}

.review-rule {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 244, 238, 0.9);
}

.review-rule.incomplete,
.review-rule.warning,
.review-rule.danger {
  background: rgba(255, 233, 223, 0.96);
}

.review-rule.danger {
  border: 1px solid rgba(218, 92, 58, 0.4);
}

.review-rule.warning {
  border: 1px solid rgba(228, 145, 67, 0.36);
}

.history-box,
.logs-box {
  display: grid;
  gap: 10px;
  padding-top: 6px;
}

.history-item,
.log-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(251, 245, 241, 0.95);
}

.detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.detail-card dl {
  display: grid;
  gap: 12px;
}

.detail-card dl div {
  display: grid;
  gap: 4px;
}

.image-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-top: 18px;
}

.image-card {
  text-align: center;
}

.image-card img {
  width: 100%;
  aspect-ratio: 1 / 1.15;
  object-fit: cover;
  border-radius: 18px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(37, 20, 18, 0.42);
  z-index: 50;
}

.modal-card {
  width: min(980px, 100%);
  max-height: 88vh;
  overflow: auto;
  padding: 26px;
}

.status-badge,
.risk-badge,
.score-badge,
.report-status {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 0.84rem;
  font-weight: 800;
}

.status-badge,
.score-badge,
.report-status,
.risk-badge.normal {
  background: rgba(255, 239, 230, 0.9);
  color: #9d5a43;
}

.risk-badge.watch,
.status-badge.watch {
  background: rgba(255, 232, 213, 0.94);
  color: #bd6a35;
}

.risk-badge.high,
.status-badge.high_risk {
  background: rgba(255, 220, 214, 0.95);
  color: #bd4738;
}

.primary-button,
.ghost-button,
.approve-button,
.reject-button {
  border: none;
  border-radius: 999px;
  padding: 13px 20px;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.primary-button,
.approve-button {
  background: linear-gradient(135deg, #df7c43 0%, #d8653f 100%);
  color: #fff9f4;
}

.reject-button {
  background: #f4d3c8;
  color: #8b4938;
}

.ghost-button {
  background: rgba(255, 247, 242, 0.94);
  color: #855444;
  text-decoration: none;
}

.ghost-button.compact,
.primary-button.compact {
  padding: 10px 16px;
}

.ghost-button.danger {
  background: rgba(255, 225, 220, 0.96);
  color: #b1473f;
}

.message {
  margin: 0;
  padding: 14px 18px;
  border-radius: 18px;
  font-weight: 700;
}

.message.success {
  background: rgba(230, 244, 235, 0.94);
  color: #2f6b48;
}

.message.error {
  background: rgba(255, 229, 225, 0.95);
  color: #b74d40;
}

.warm { background: linear-gradient(180deg, rgba(255,245,238,0.94) 0%, rgba(255,235,225,0.98) 100%); }
.blush { background: linear-gradient(180deg, rgba(255,240,235,0.94) 0%, rgba(250,224,215,0.98) 100%); }
.sand { background: linear-gradient(180deg, rgba(255,249,240,0.94) 0%, rgba(247,230,214,0.98) 100%); }
.alert { background: linear-gradient(180deg, rgba(255,239,228,0.98) 0%, rgba(255,223,205,0.98) 100%); }
.amber { background: linear-gradient(180deg, rgba(255,245,231,0.96) 0%, rgba(251,229,200,0.98) 100%); }
.soft { background: linear-gradient(180deg, rgba(255,246,241,0.96) 0%, rgba(247,229,221,0.98) 100%); }
.mint { background: linear-gradient(180deg, rgba(240,252,247,0.96) 0%, rgba(223,243,234,0.98) 100%); }

@media (max-width: 1100px) {
  .hero-card,
  .detail-grid,
  .category-grid,
  .mini-summary-grid,
  .info-grid,
  .notice-grid,
  .template-editor-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }

  .review-toolbar .field,
  .review-toolbar .search-wide,
  .risk-toolbar .field,
  .risk-toolbar .search-wide,
  .report-toolbar .field {
    grid-column: span 3;
  }
}

@media (max-width: 760px) {
  .admin-page {
    padding: 20px 12px 48px;
  }

  .hero-card,
  .category-grid,
  .mini-summary-grid,
  .detail-grid,
  .info-grid,
  .notice-grid,
  .template-editor-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }

  .review-toolbar .field,
  .review-toolbar .search-wide,
  .risk-toolbar .field,
  .risk-toolbar .search-wide,
  .report-toolbar .field {
    grid-column: auto;
  }

  .section-head,
  .hero-buttons,
  .candidate-head,
  .risk-head,
  .report-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .sticky-tabs {
    top: 10px;
    border-radius: 24px;
  }
}
</style>
