<template>
  <section class="review-page">
    <div class="review-card">
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
          <p v-else class="sub-text">최소 조건을 충족했습니다. 원하면 최대 5장까지 더 등록할 수 있습니다.</p>
        </div>
      </div>

      <div class="status-box progress-box">
        <strong>업로드 진행률</strong>
        <div class="progress-track" aria-hidden="true">
          <div class="progress-fill" :style="{ width: `${uploadProgressPercent}%` }"></div>
        </div>
        <p class="sub-text">현재 {{ images.length }} / 5장 등록됨</p>
      </div>

      <form class="upload-form" @submit.prevent="saveImage">
        <label>
          <span>심사용 프로필 사진 파일</span>
          <input
            ref="fileInputRef"
            type="file"
            accept="image/png,image/jpeg,image/webp"
            @change="handleFileChange"
            :disabled="!canUploadMore || reviewStatus.status === 'DELETED'"
            required
          />
        </label>

        <label>
          <span>사진 순서</span>
          <select v-model.number="form.imageOrder" :disabled="reviewStatus.status === 'DELETED'">
            <option v-for="order in [1, 2, 3, 4, 5]" :key="order" :value="order">{{ order }}번</option>
          </select>
        </label>

        <label class="checkbox-row">
          <input v-model="form.isMain" type="checkbox" :disabled="!canUploadMore || reviewStatus.status === 'DELETED'" />
          <span>대표 사진으로 지정</span>
        </label>

        <button class="primary-button" :disabled="loading || !canUploadMore || reviewStatus.status === 'DELETED'">
          {{ loading ? '등록 중...' : '사진 등록' }}
        </button>
      </form>

      <p v-if="!canUploadMore" class="message success">사진 5장을 모두 등록했습니다. 다른 순서를 선택하면 기존 사진을 새 사진으로 교체할 수 있습니다.</p>
      <p v-if="message" class="message success">{{ message }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="image-list" v-if="images.length">
        <div v-for="image in images" :key="image.id" class="image-item">
          <img :src="toAbsoluteImageUrl(image.imageUrl)" alt="uploaded profile" />
          <strong>{{ image.imageOrder }}번 사진{{ image.isMain ? ' · 대표' : '' }}</strong>
          <p>{{ image.imageUrl }}</p>
        </div>
      </div>

      <div class="helper-row">
        <button class="secondary-button" @click="refreshStatus" type="button">상태 새로고침</button>
        <button class="secondary-button" @click="logout" type="button">로그아웃</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";
import {
  clearToken,
  hasShownSignupApprovalNotice,
  markSignupApprovalNoticeShown,
} from "../utils/auth";

const router = useRouter();
const loading = ref(false);
const message = ref("");
const errorMessage = ref("");
const images = ref([]);
const fileInputRef = ref(null);
const maxUploadFileSize = 10 * 1024 * 1024;
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
const canUploadMore = computed(() => images.value.length < 5);
const uploadProgressPercent = computed(() => (images.value.length / 5) * 100);
const showDeadlineWarning = computed(() => ["PENDING_REVIEW", "REJECTED"].includes(reviewStatus.value.status));
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
    message.value = data.length >= 2
      ? "사진을 등록했습니다. 심사 대기 상태를 유지하면서 추가 사진도 계속 등록할 수 있습니다."
      : `사진을 등록했습니다. 승인 심사를 위해 사진 ${Math.max(0, 2 - data.length)}장을 더 등록해주세요.`;
    form.file = null;
    form.imageOrder = nextAvailableOrder.value;
    form.isMain = false;
    if (fileInputRef.value) {
      fileInputRef.value.value = "";
    }
    await refreshStatus();
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
  return `http://${window.location.hostname}:8082${path}`;
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
  min-height: 100vh;
  padding: 24px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #fff5e9 0%, #ffd7c2 100%);
}

.review-card {
  width: min(100%, 720px);
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 251, 247, 0.94);
  box-shadow: 0 18px 45px rgba(98, 49, 34, 0.14);
}

.eyebrow {
  margin: 0 0 10px;
  color: #a94f31;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #35231a;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.status-box,
.image-item {
  margin-top: 14px;
  padding: 16px;
  border-radius: 16px;
  background: #fff;
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
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
}

.checkbox-row {
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 12px;
}

.primary-button,
.secondary-button {
  border: none;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button {
  background: #cd6d2d;
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
}

@media (max-width: 640px) {
  .helper-row,
  .status-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
