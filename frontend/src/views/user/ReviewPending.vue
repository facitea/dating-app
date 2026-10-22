<template>
  <section class="review-page">
    <div class="review-card" :class="{ 'review-card--locked': showReviewLock }">
      <p class="eyebrow">Review Process</p>
      <h1>가입 심사 대기</h1>
      <p class="description">{{ reviewStatus.message }}</p>

      <div class="step-box">
        <div class="step-item active">1. 기본 계정 정보 입력 완료</div>
        <div class="step-item" :class="{ active: reviewStatus.profileComplete }">2. 프로필 필수 항목 완료</div>
        <div class="step-item" :class="{ active: images.length > 0 }">3. 사진 업로드 진행 중</div>
        <div class="step-item" :class="{ active: reviewStatus.readyForReview }">4. 운영자 심사 대기</div>
        <div class="step-item" :class="{ active: reviewStatus.status === 'ACTIVE' }">5. 승인 후 가입 완료</div>
      </div>

      <div class="status-box emphasis" :class="deadlineClass" v-if="showDeadlineWarning">
        <strong>{{ deadlineTitle }}</strong>
        <p>{{ deadlineMessage }}</p>
        <p class="sub-text">기한 내에 사진 2장 이상 등록과 프로필 필수 항목 입력이 완료되지 않으면 계정이 자동 정리될 수 있습니다.</p>
      </div>

      <div class="status-box">
        <strong>현재 상태</strong>
        <p>{{ statusLabel }}</p>
        <p v-if="reviewStatus.reviewComment" class="sub-text">{{ reviewStatus.reviewComment }}</p>
      </div>

      <div class="status-grid">
        <div class="status-box compact-box">
          <strong>프로필 필수 항목</strong>
          <p>{{ reviewStatus.profileComplete ? '완료' : '미완료' }}</p>
          <p class="sub-text">직업, MBTI, 성격, 이상형, 자기소개까지 채워져야 심사에 올릴 수 있습니다.</p>
        </div>

        <div class="status-box compact-box">
          <strong>등록된 사진</strong>
          <p>{{ images.length }}장 / 최소 2장 필요</p>
          <p v-if="!minimumRequirementMet" class="sub-text">
            심사 승인 조건까지 사진 {{ remainingRequiredImages }}장을 더 등록해주세요.
          </p>
          <p v-else class="sub-text">최소 조건을 충족했습니다. 원하면 최대 6장까지 더 등록할 수 있습니다.</p>
        </div>
      </div>

      <PhotoEditor @changed="photoChanged"/>
      <p v-if="message" class="message success">{{message}}</p><p v-if="errorMessage" class="message error">{{errorMessage}}</p>
      <div class="helper-row">
        <button class="secondary-button" @click="submitReviewRequest" type="button" :disabled="loading || reviewStatus.status === 'DELETED'">
          {{ reviewActionLabel }}
        </button>
        <button class="secondary-button" @click="logout" type="button">로그아웃</button>
      </div>
    </div>

    <div v-if="showReviewLock" class="review-lock-overlay" role="status" aria-live="polite">
      <div class="review-lock-modal">
        <span class="lock-kicker">Review</span>
        <strong>심사 중 입니다.</strong>
        <p>{{ reviewLockMessage }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import PhotoEditor from "../../components/PhotoEditor.vue";
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import {
  clearToken,
  hasShownSignupApprovalNotice,
  markSignupApprovalNoticeShown,
} from "../../utils/auth";

const router = useRouter();
const loading = ref(false);
const message = ref("");
const errorMessage = ref("");
const images = ref([]);
const fileInputRef = ref(null);
const reviewSubmitted = ref(false);
const maxUploadFileSize = 10 * 1024 * 1024;
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || `http://${window.location.hostname}:8082/api`;
const assetBaseUrl = apiBaseUrl.replace(/\/api\/?$/, "");
const reviewStatus = ref({
  status: "PENDING_REVIEW",
  imageCount: 0,
  readyForReview: false,
  profileComplete: false,
  reviewComment: "",
  message: "",
  reviewDeadlineAt: null,
  remainingDays: -1,
});

const form = reactive({
  file: null,
  imageOrder: 1,
  isMain: false,
});

let refreshTimerId = null;

const statusLabel = computed(() => {
  if (reviewStatus.value.status === "ACTIVE") return "승인 완료";
  if (reviewStatus.value.status === "REJECTED") return "반려";
  if (reviewStatus.value.status === "PENDING_REVIEW") return "심사 대기";
  if (reviewStatus.value.status === "DELETED") return "자동 정리됨";
  return reviewStatus.value.status;
});

const minimumRequirementMet = computed(() => images.value.length >= 2);
const remainingRequiredImages = computed(() => Math.max(0, 2 - images.value.length));
const canUploadMore = computed(() => images.value.length < 6);
const uploadProgressPercent = computed(() => (images.value.length / 6) * 100);
const showDeadlineWarning = computed(() => ["PENDING_REVIEW", "REJECTED"].includes(reviewStatus.value.status));
const canSubmitReview = computed(() => (
  ["PENDING_REVIEW", "REJECTED"].includes(reviewStatus.value.status)
  && reviewStatus.value.profileComplete
  && reviewStatus.value.readyForReview
));
const showReviewLock = computed(() => reviewSubmitted.value && reviewStatus.value.status === "PENDING_REVIEW");
const reviewActionLabel = computed(() => (reviewStatus.value.status === "REJECTED" ? "재심사 신청" : "가입 신청"));
const reviewLockMessage = computed(() => (
  reviewStatus.value.status === "REJECTED"
    ? "재심사 신청이 접수되었습니다. 운영자 확인이 끝날 때까지 잠시 기다려주세요."
    : "가입 신청이 접수되었습니다. 운영자 확인이 끝날 때까지 잠시 기다려주세요."
));
const projectedImageCountAfterSave = computed(() => {
  const replacesExistingOrder = images.value.some((image) => image.imageOrder === form.imageOrder);
  return replacesExistingOrder ? images.value.length : images.value.length + 1;
});
const uploadButtonLabel = computed(() => {
  const willMeetMinimum = projectedImageCountAfterSave.value >= 2;
  if (reviewStatus.value.status === "REJECTED" && willMeetMinimum) return "사진 저장 후 재심사 신청";
  if (willMeetMinimum) return "사진 저장 후 가입 신청";
  return "사진 저장";
});
const deadlineClass = computed(() => {
  if (reviewStatus.value.remainingDays === 0) return "danger-box";
  if (reviewStatus.value.remainingDays === 1) return "warning-box";
  return "info-box";
});
const deadlineTitle = computed(() => {
  if (reviewStatus.value.remainingDays === 0) return "오늘 마감";
  if (reviewStatus.value.remainingDays === 1) return "마감 임박";
  return "주의 안내";
});
const deadlineMessage = computed(() => {
  if (!showDeadlineWarning.value) return "";
  if (reviewStatus.value.remainingDays === 0) return "오늘 안에 프로필과 사진 등록을 완료하지 않으면 계정이 자동 정리될 수 있습니다.";
  if (reviewStatus.value.remainingDays === 1) return "마감이 하루 남았습니다. 지금 프로필 필수 항목과 사진 등록을 마무리해주세요.";
  if (reviewStatus.value.remainingDays > 1) return `${reviewStatus.value.remainingDays}일 안에 프로필과 사진 등록을 완료해야 합니다.`;
  return "기한 내에 완료하지 않으면 계정이 자동 정리될 수 있습니다.";
});
const nextAvailableOrder = computed(() => {
  for (let order = 1; order <= 5; order += 1) {
    if (!images.value.some((image) => image.imageOrder === order)) {
      return order;
    }
  }
  return 5;
});

const handleApprovedState = () => {
  if (!hasShownSignupApprovalNotice()) {
    window.alert("사진 심사가 승인되었습니다. 가입이 완료되어 다음 화면으로 이동합니다.");
    markSignupApprovalNoticeShown();
  }
  router.replace("/signup-complete");
};

const refreshStatus = async () => {
  try {
    const [statusRes, imagesRes] = await Promise.all([
      api.get("/profile-images/review-status"),
      api.get("/profile-images/me"),
    ]);
    reviewStatus.value = statusRes.data;
    images.value = imagesRes.data;

    form.imageOrder = nextAvailableOrder.value;

    if (reviewStatus.value.status === "ACTIVE") {
      handleApprovedState();
    }
  } catch (error) {
    clearToken();
    router.replace("/");
  }
};

const photoChanged = async (data) => { images.value=data; await refreshStatus(); };
const submitReviewRequest = async () => {
  message.value = "";
  errorMessage.value = "";

  if (!reviewStatus.value.profileComplete) {
    errorMessage.value = "프로필 필수 항목을 먼저 모두 입력해주세요.";
    window.alert(errorMessage.value);
    router.push("/profile");
    return;
  }

  if (!reviewStatus.value.readyForReview) {
    errorMessage.value = "가입 신청을 위해 대표 사진 포함 최소 2장을 등록해주세요.";
    window.alert(errorMessage.value);
    return;
  }

  loading.value = true;
  try {
    const wasRejected = reviewStatus.value.status === "REJECTED";
    const { data } = await api.post("/profile-images/review-submit");
    reviewStatus.value = data;
    reviewSubmitted.value = true;
    message.value = wasRejected
      ? "재심사 신청이 접수되었습니다."
      : "가입 신청이 접수되었습니다.";
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "심사 신청에 실패했습니다.";
    window.alert(errorMessage.value);
  } finally {
    loading.value = false;
  }
};

const saveImage = async () => {
  loading.value = true;
  message.value = "";
  errorMessage.value = "";

  try {
    if (!form.file) {
      throw new Error("파일을 먼저 선택해주세요.");
    }

    const formData = new FormData();
    formData.append("file", form.file);
    formData.append("imageOrder", String(form.imageOrder));
    formData.append("isMain", String(form.isMain));

    const { data } = await api.post("/profile-images/me/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
    images.value = data;
    const meetsMinimumImages = data.length >= 2;
    message.value = meetsMinimumImages
      ? "사진을 저장했습니다. 심사 신청 조건을 확인하는 중입니다."
      : `사진을 등록했습니다. 승인 심사를 위해 사진 ${Math.max(0, 2 - data.length)}장을 더 등록해주세요.`;
    form.file = null;
    form.imageOrder = nextAvailableOrder.value;
    form.isMain = false;
    if (fileInputRef.value) {
      fileInputRef.value.value = "";
    }
    await refreshStatus();
    if (meetsMinimumImages) {
      await submitReviewRequest();
    }
  } catch (error) {
    errorMessage.value = error.response?.data?.message || error.message || "사진 등록에 실패했습니다.";
  } finally {
    loading.value = false;
  }
};

const handleFileChange = (event) => {
  const selectedFile = event.target.files?.[0] || null;
  errorMessage.value = "";

  if (selectedFile && selectedFile.size > maxUploadFileSize) {
    form.file = null;
    errorMessage.value = "사진 파일은 10MB 이하로 선택해 주세요.";
    if (fileInputRef.value) {
      fileInputRef.value.value = "";
    }
    return;
  }

  form.file = selectedFile;
};

const logout = () => {
  clearToken();
  router.replace("/");
};

const toAbsoluteImageUrl = (path) => {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  return `${assetBaseUrl}${path}`;
};

onMounted(async () => {
  await refreshStatus();
  refreshTimerId = window.setInterval(refreshStatus, 10000);
});

onBeforeUnmount(() => {
  if (refreshTimerId) {
    window.clearInterval(refreshTimerId);
  }
});
</script>

<style scoped>
.review-page {
  position: relative;
  min-height: 100vh;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  display: flex;
  justify-content: center;
  background: transparent;
}

.review-card {
  width: min(100%, 720px);
  margin: auto 0;
  padding: 28px 20px 22px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 251, 247, 0.97)),
    rgba(255, 251, 247, 0.94);
  border: 1px solid rgba(240, 206, 193, 0.9);
  box-shadow: 0 24px 50px rgba(98, 49, 34, 0.14);
  backdrop-filter: blur(18px);
  transition: filter 0.24s ease, transform 0.24s ease, opacity 0.24s ease;
}

.review-card--locked {
  filter: blur(8px);
  opacity: 0.46;
  pointer-events: none;
  user-select: none;
}

.review-lock-overlay {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(50, 29, 22, 0.18);
}

.review-lock-modal {
  width: min(100%, 320px);
  padding: 22px 20px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 189, 173, 0.96);
  box-shadow: 0 20px 46px rgba(55, 28, 20, 0.18);
  text-align: center;
}

.lock-kicker {
  display: block;
  margin-bottom: 8px;
  color: #a94f31;
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.review-lock-modal strong {
  display: block;
  color: #35231a;
  font-size: 22px;
  line-height: 1.25;
}

.review-lock-modal p {
  margin: 10px 0 18px;
  color: #735448;
  line-height: 1.55;
}

.eyebrow {
  margin: 0 0 10px;
  color: #a94f31;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #35231a;
  font-size: clamp(1.9rem, 7vw, 2.4rem);
  line-height: 1.08;
}

.description {
  margin: 12px 0 24px;
  color: #6d5348;
  line-height: 1.6;
}

.step-box {
  display: grid;
  gap: 10px;
  margin-bottom: 16px;
}

.step-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #fff5ef;
  border: 1px solid #f2d6ca;
  color: #7b574b;
  font-weight: 600;
}

.step-item.active {
  background: #fff;
  color: #4d3129;
  border-color: #e4b89f;
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.status-box,
.image-item {
  margin-top: 14px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #efd5ca;
}

.compact-box {
  margin-top: 0;
}

.emphasis {
  border-width: 2px;
}

.info-box {
  background: #fff3ed;
  border-color: #efc0ae;
}

.warning-box {
  background: #fff0dc;
  border-color: #eba35f;
  box-shadow: 0 0 0 4px rgba(235, 163, 95, 0.12);
}

.danger-box {
  background: #ffe7e2;
  border-color: #e48072;
  box-shadow: 0 0 0 4px rgba(228, 128, 114, 0.14);
}

.sub-text {
  margin-top: 8px;
  color: #8d6255;
  line-height: 1.5;
}

.progress-box {
  display: grid;
  gap: 10px;
}

.progress-track {
  height: 12px;
  border-radius: 999px;
  background: #f6ddd2;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #cd6d2d 0%, #f1a35d 100%);
  transition: width 0.2s ease;
}

.upload-form {
  margin-top: 22px;
  display: grid;
  gap: 16px;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input,
select {
  border: 1px solid #e6c1b4;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.94);
}

.checkbox-row {
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 12px;
}

.primary-button,
.secondary-button {
  border: none;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  background: linear-gradient(135deg, #cd6d2d 0%, #ec9456 100%);
  color: #fff;
}

.primary-button:disabled {
  cursor: not-allowed;
  background: #d9c5b8;
  color: #7c665a;
}

.secondary-button {
  background: #fff;
  color: #4d3129;
  border: 1px solid #e6c1b4;
}

.secondary-button:disabled {
  cursor: not-allowed;
  background: #eee0d8;
  color: #96796d;
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

.image-list {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.image-item img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
  border: 1px solid #efd5ca;
  background: #fff6f1;
  margin-bottom: 12px;
}

.image-item p {
  margin: 8px 0 0;
  color: #6e534c;
  overflow-wrap: anywhere;
}

.helper-row {
  margin-top: 20px;
  display: flex;
  gap: 12px;
  flex-direction: column;
}

@media (min-width: 768px) {
  .review-page {
    padding: 28px;
  }

  .review-card {
    padding: 36px 34px 30px;
  }

  .status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .helper-row {
    flex-direction: row;
  }
}
</style>
