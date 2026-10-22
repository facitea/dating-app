<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Create Account</p>
      <h1>회원가입</h1>
      <p class="description">
        기본 정보 입력 후 사진 심사 단계로 이동합니다. 가입 승인은 운영자 심사가
        끝난 뒤 확정됩니다.
      </p>

      <form class="auth-form" @submit.prevent="signup">
        <div class="section-title">기본 계정 정보</div>

        <label>
          <span>닉네임</span>
          <input
            ref="nicknameInput"
            v-model.trim="form.nickname"
            type="text"
            placeholder="서비스에 표시될 이름"
            required
          />
        </label>

        <label>
          <span>이메일</span>
          <input
            ref="emailInput"
            v-model.trim="form.email"
            type="email"
            placeholder="you@example.com"
            required
          />
        </label>

        <div class="verification-box">
          <button class="ghost-button" type="button" :disabled="loading || verificationLoading" @click="requestEmailVerification">
            {{ verificationLoading ? "인증번호 발급 중..." : "이메일 인증번호 받기" }}
          </button>

          <label>
            <span>이메일 인증번호</span>
            <input
              ref="emailCodeInput"
              v-model.trim="emailVerificationCode"
              inputmode="numeric"
              maxlength="6"
              placeholder="서버 로그의 6자리 번호"
            />
          </label>

          <button class="ghost-button" type="button" :disabled="loading || verificationLoading || !emailVerificationCode" @click="confirmEmailVerification">
            {{ emailVerified ? "이메일 인증 완료" : "인증번호 확인" }}
          </button>
          <p v-if="verificationMessage" class="message notice">{{ verificationMessage }}</p>
        </div>

        <label>
          <span>비밀번호</span>
          <input
            ref="passwordInput"
            v-model="form.password"
            type="password"
            placeholder="8자 이상 입력"
            required
            minlength="8"
          />
        </label>

        <div class="section-title">기본 프로필 정보</div>

        <label>
          <span>생년월일</span>
          <input ref="birthDateInput" v-model="form.birthDate" type="date" required />
        </label>

        <label>
          <span>성별</span>
          <select ref="genderSelect" v-model="form.gender" required>
            <option value="MALE">남성</option>
            <option value="FEMALE">여성</option>
          </select>
        </label>

        <div class="region-grid">
          <label>
            <span>거주 시/도</span>
            <select ref="regionCitySelect" v-model="selectedRegionCity" @change="handleRegionCityChange" required>
              <option value="">시/도 선택</option>
              <option v-for="city in regionCities" :key="city" :value="city">{{ city }}</option>
            </select>
          </label>

          <label>
            <span>거주 구/군</span>
            <select
              ref="regionDistrictSelect"
              v-model="selectedRegionDistrict"
              @change="updateRegion"
              :disabled="!selectedRegionCity"
              required
            >
              <option value="">구/군 선택</option>
              <option v-for="district in availableDistricts" :key="district" :value="district">
                {{ district }}
              </option>
            </select>
          </label>
        </div>

        <label>
          <span>직업</span>
          <input v-model.trim="form.job" type="text" placeholder="예: 개발자" />
        </label>

        <label>
          <span>MBTI</span>
          <select v-model="form.mbti">
            <option value="">선택 안 함</option>
            <option v-for="item in mbtiOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>

        <label>
          <span>흡연 여부</span>
          <select v-model="form.smokingStatus" required>
            <option value="NON_SMOKER">비흡연</option>
            <option value="OCCASIONAL">가끔 흡연</option>
            <option value="SMOKER">흡연</option>
          </select>
        </label>

        <label>
          <span>음주 여부</span>
          <select v-model="form.drinkingStatus" required>
            <option value="NONE">안 함</option>
            <option value="SOMETIMES">가끔</option>
            <option value="OFTEN">자주</option>
          </select>
        </label>

        <label>
          <span>종교</span>
          <select v-model="form.religion" required>
            <option value="NONE">없음</option>
            <option value="CHRISTIAN">기독교</option>
            <option value="CATHOLIC">천주교</option>
            <option value="BUDDHIST">불교</option>
            <option value="OTHER">기타</option>
          </select>
        </label>

        <label>
          <span>내 성격</span>
          <textarea v-model.trim="form.personality" rows="3" placeholder="성격을 간단히 적어주세요."></textarea>
        </label>

        <label>
          <span>이상형</span>
          <textarea v-model.trim="form.idealType" rows="3" placeholder="어떤 분을 만나고 싶은지 적어주세요."></textarea>
        </label>

        <label>
          <span>자기소개</span>
          <textarea v-model.trim="form.introduction" rows="4" placeholder="첫인상에 보일 자기소개를 적어주세요."></textarea>
        </label>

        <section class="signup-consents" aria-labelledby="consents-title">
          <h3 id="consents-title">약관 및 개인정보 동의</h3>
          <details class="consent-document" open>
            <summary>서비스 이용약관 <span>전문 보기</span></summary>
            <div class="consent-body" tabindex="0" role="region" aria-label="서비스 이용약관 본문"><TermsText type="terms"/></div>
          </details>
          <label class="consent-check"><input ref="termsCheckbox" v-model="form.agreedToTerms" type="checkbox" required/><span><b>[필수]</b> 서비스 이용약관에 동의합니다.</span></label>
          <details class="consent-document" open>
            <summary>개인정보 수집·이용 안내 <span>전문 보기</span></summary>
            <div class="consent-body" tabindex="0" role="region" aria-label="개인정보 수집 이용 안내 본문"><TermsText type="privacy"/></div>
          </details>
          <label class="consent-check"><input v-model="form.agreedToPrivacy" type="checkbox" required/><span><b>[필수]</b> 개인정보 수집·이용에 동의합니다.</span></label>
          <label class="consent-check"><input v-model="form.adultConfirmed" type="checkbox" required/><span><b>[필수]</b> 만 19세 이상입니다.</span></label>
          <label class="consent-check"><input v-model="form.marketingConsent" type="checkbox"/><span>[선택] 이벤트·마케팅 안내 수신</span></label>
          <p class="consent-note">선택 항목에 동의하지 않아도 가입할 수 있습니다.</p>
        </section>

        <label class="honeypot-field" aria-hidden="true">
          <span>웹사이트</span>
          <input v-model="form.website" type="text" tabindex="-1" autocomplete="off" />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? "다음 단계 준비 중..." : "다음: 사진 업로드" }}
        </button>
      </form>

      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="policy-box">
        <strong>가입 절차 안내</strong>
        <p>1. 기본 정보 입력</p>
        <p>2. 프로필 사진 업로드</p>
        <p>3. 운영자 사진 심사 대기</p>
        <p>4. 승인 알림 후 가입 완료</p>
      </div>

      <p class="helper-text">
        이미 계정이 있나요?
        <RouterLink to="/">로그인</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import TermsText from "../../components/TermsText.vue";
import "../../components/social.css";
import { computed, nextTick, reactive, ref, watch } from "vue";
import { RouterLink, useRouter } from "vue-router";
import { authClient } from "../../auth-client";
import { resetSignupApprovalNotice, setAuthTokens } from "../../utils/auth";

const mbtiOptions = [
  "INTJ", "INTP", "ENTJ", "ENTP",
  "INFJ", "INFP", "ENFJ", "ENFP",
  "ISTJ", "ISFJ", "ESTJ", "ESFJ",
  "ISTP", "ISFP", "ESTP", "ESFP",
];

const regionOptions = {
  서울: ["강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"],
  경기: ["고양시", "과천시", "광명시", "광주시", "구리시", "군포시", "김포시", "남양주시", "성남시", "수원시", "시흥시", "안산시", "안양시", "용인시", "의정부시", "파주시", "평택시", "하남시", "화성시"],
  인천: ["계양구", "남동구", "미추홀구", "부평구", "서구", "연수구", "중구"],
  부산: ["강서구", "금정구", "남구", "동래구", "부산진구", "북구", "사상구", "사하구", "서구", "수영구", "연제구", "영도구", "중구", "해운대구"],
  대구: ["남구", "달서구", "달성군", "동구", "북구", "서구", "수성구", "중구"],
  대전: ["대덕구", "동구", "서구", "유성구", "중구"],
  광주: ["광산구", "남구", "동구", "북구", "서구"],
  울산: ["남구", "동구", "북구", "중구", "울주군"],
  세종: ["세종시"],
  강원: ["강릉시", "동해시", "속초시", "원주시", "춘천시"],
  충북: ["제천시", "청주시", "충주시"],
  충남: ["공주시", "논산시", "당진시", "보령시", "서산시", "아산시", "천안시"],
  전북: ["군산시", "김제시", "남원시", "익산시", "전주시", "정읍시"],
  전남: ["광양시", "목포시", "순천시", "여수시"],
  경북: ["경산시", "경주시", "구미시", "김천시", "포항시"],
  경남: ["거제시", "김해시", "밀양시", "양산시", "진주시", "창원시", "통영시"],
  제주: ["제주시", "서귀포시"],
};

const router = useRouter();
const loading = ref(false);
const verificationLoading = ref(false);
const errorMessage = ref("");
const verificationMessage = ref("");
const emailVerificationCode = ref("");
const emailVerified = ref(false);
const nicknameInput = ref(null);
const emailInput = ref(null);
const emailCodeInput = ref(null);
const passwordInput = ref(null);
const birthDateInput = ref(null);
const genderSelect = ref(null);
const regionCitySelect = ref(null);
const regionDistrictSelect = ref(null);
const termsCheckbox = ref(null);
const selectedRegionCity = ref("");
const selectedRegionDistrict = ref("");

const form = reactive({
  nickname: "",
  email: "",
  emailVerificationToken: "",
  password: "",
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
  agreedToTerms: false,
  agreedToPrivacy:false,adultConfirmed:false,marketingConsent:false,termsVersion:"2026-10-21-draft",
  website: "",
  formStartedAt: Date.now(),
});

const regionCities = Object.keys(regionOptions);
const availableDistricts = computed(() => regionOptions[selectedRegionCity.value] || []);

const updateRegion = () => {
  form.region = selectedRegionCity.value && selectedRegionDistrict.value
    ? `${selectedRegionCity.value} ${selectedRegionDistrict.value}`
    : "";
};

const resetEmailVerification = () => {
  form.emailVerificationToken = "";
  emailVerificationCode.value = "";
  emailVerified.value = false;
  verificationMessage.value = "";
};

const handleRegionCityChange = () => {
  selectedRegionDistrict.value = "";
  updateRegion();
};

watch(() => form.email, () => {
  resetEmailVerification();
});

const focusField = async (field) => {
  await nextTick();

  const focusTargets = {
    nickname: nicknameInput,
    email: emailInput,
    emailCode: emailCodeInput,
    password: passwordInput,
    birthDate: birthDateInput,
    gender: genderSelect,
    regionCity: regionCitySelect,
    regionDistrict: regionDistrictSelect,
    agreedToTerms: termsCheckbox,
  };

  focusTargets[field]?.value?.focus?.();
};

const validateForm = () => {
  if (form.nickname.length < 2 || form.nickname.length > 30) {
    return {
      field: "nickname",
      message: "닉네임은 2자 이상 30자 이하로 입력해주세요.",
    };
  }

  if (!form.email.trim()) {
    return {
      field: "email",
      message: "이메일을 입력해주세요.",
    };
  }

  if (!form.emailVerificationToken) {
    return {
      field: "emailCode",
      message: "이메일 인증을 먼저 완료해주세요.",
    };
  }

  if (form.password.length < 8) {
    return {
      field: "password",
      message: "비밀번호는 8자 이상으로 입력해주세요.",
    };
  }

  if (!form.birthDate) {
    return {
      field: "birthDate",
      message: "생년월일을 입력해주세요.",
    };
  }

  const today = new Date();
  const adultThreshold = new Date(today.getFullYear() - 19, today.getMonth(), today.getDate());
  const birthDate = new Date(form.birthDate);
  if (birthDate > adultThreshold) {
    return {
      field: "birthDate",
      message: "만 19세 이상만 가입할 수 있습니다.",
    };
  }

  if (!selectedRegionCity.value) {
    return {
      field: "regionCity",
      message: "거주 시/도를 선택해주세요.",
    };
  }

  if (!selectedRegionDistrict.value) {
    return {
      field: "regionDistrict",
      message: "거주 구/군을 선택해주세요.",
    };
  }

  if (!form.agreedToTerms || !form.agreedToPrivacy || !form.adultConfirmed) {
    return {
      field: "agreedToTerms",
      message: "약관 동의는 필수입니다.",
    };
  }

  return null;
};

const requestEmailVerification = async () => {
  verificationLoading.value = true;
  errorMessage.value = "";
  verificationMessage.value = "";
  form.emailVerificationToken = "";
  emailVerified.value = false;

  try {
    if (!form.email.trim()) {
      window.alert("이메일을 먼저 입력해주세요.");
      await focusField("email");
      return;
    }

    const data = await authClient.requestEmailCode(form.email, "SIGNUP");

    verificationMessage.value = data.devCode
      ? `개발용 이메일 인증번호: ${data.devCode}`
      : data.message || "인증번호를 발급했습니다.";
    await focusField("emailCode");
  } catch (error) {
    const message = error.response?.data?.message || "이메일 인증번호 발급에 실패했습니다.";
    errorMessage.value = message;
    window.alert(message);
  } finally {
    verificationLoading.value = false;
  }
};

const confirmEmailVerification = async () => {
  verificationLoading.value = true;
  errorMessage.value = "";

  try {
    if (!emailVerificationCode.value) {
      window.alert("이메일 인증번호를 입력해주세요.");
      await focusField("emailCode");
      return;
    }

    const data = await authClient.confirmEmailCode(form.email, emailVerificationCode.value, "SIGNUP");

    form.emailVerificationToken = data.verificationToken;
    emailVerified.value = true;
    verificationMessage.value = data.message || "이메일 인증이 완료되었습니다.";
  } catch (error) {
    form.emailVerificationToken = "";
    emailVerified.value = false;
    const message = error.response?.data?.message || "이메일 인증번호 확인에 실패했습니다.";
    errorMessage.value = message;
    window.alert(message);
    await focusField("emailCode");
  } finally {
    verificationLoading.value = false;
  }
};

// 가입 정보 저장이 끝나면 바로 가입 완료가 아니라 사진 심사 단계로 보낸다.
const signup = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const validationResult = validateForm();
    if (validationResult) {
      window.alert(validationResult.message);
      await focusField(validationResult.field);
      return;
    }

    const data = await authClient.signup(form);
    resetSignupApprovalNotice();
    setAuthTokens(data);
    router.push("/review-pending");
  } catch (error) {
    const message = error.response?.data?.message || "회원가입에 실패했습니다. 입력값을 다시 확인해주세요.";
    errorMessage.value = message;
    window.alert(message);

    if (message.includes("닉네임")) {
      await focusField("nickname");
    } else if (message.includes("이메일")) {
      await focusField("email");
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
}

.auth-card {
  width: min(100%, 560px);
  min-height: min(920px, calc(100vh - 36px));
  margin: auto 0;
  padding: 28px 22px 26px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(255, 251, 247, 0.97)),
    rgba(255, 252, 247, 0.94);
  border: 1px solid rgba(240, 210, 193, 0.92);
  box-shadow:
    0 26px 52px rgba(109, 70, 36, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 10px;
  color: #b35a28;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #35231a;
  font-size: clamp(2rem, 7vw, 2.5rem);
  line-height: 1.08;
}

.description {
  margin: 12px 0 24px;
  color: #6a5246;
  line-height: 1.62;
}

.section-title {
  margin-top: 8px;
  color: #8d4b27;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.auth-form {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 8px;
  color: #4d382d;
  font-weight: 600;
}

.honeypot-field {
  display: none;
}

.region-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.verification-box {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 247, 239, 0.78);
  border: 1px solid #f0d8c4;
}

input,
select,
textarea {
  border: 1px solid #ebc7a9;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.94);
  font-family: inherit;
  box-shadow: inset 0 1px 2px rgba(77, 56, 45, 0.04);
}

select:disabled {
  cursor: not-allowed;
  background: #f7efe8;
  color: #9f8677;
}

textarea {
  resize: vertical;
}

.checkbox-row {
  grid-template-columns: auto 1fr;
  align-items: start;
  gap: 12px;
  padding: 6px 0 4px;
}

.checkbox-row input {
  margin-top: 3px;
}

.primary-button {
  margin-top: 8px;
  width: 100%;
  border: none;
  border-radius: 18px;
  padding: 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  background: linear-gradient(135deg, #cd6d2d 0%, #ec9456 100%);
  color: #fff;
  box-shadow: 0 16px 28px rgba(205, 109, 45, 0.24);
}

.ghost-button {
  width: 100%;
  border: 1px solid rgba(205, 109, 45, 0.32);
  border-radius: 18px;
  padding: 14px 16px;
  background: #fffaf5;
  color: #9a4e23;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.ghost-button:disabled,
.primary-button:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.message {
  margin-top: 16px;
}

.error {
  color: #b72f2f;
}

.notice {
  color: #276749;
}

.policy-box {
  margin-top: 22px;
  padding: 16px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff7ed 0%, #fff4eb 100%);
  border: 1px solid #f0d8c4;
  color: #6a5246;
}

.policy-box strong {
  display: block;
  margin-bottom: 8px;
  color: #3b281f;
}

.policy-box p {
  margin: 0;
  line-height: 1.6;
}

.helper-text {
  margin-top: 20px;
  color: #6a5246;
  text-align: center;
}

.helper-text a {
  color: #a3461a;
  font-weight: 700;
}

@media (min-width: 768px) {
  .auth-page {
    padding: 28px;
  }

  .auth-card {
    min-height: auto;
    padding: 36px 34px 32px;
  }

  .region-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .auth-card {
    border-radius: 28px;
    padding: 24px 18px 22px;
  }
}
</style>

<style scoped>
.signup-consents { padding-top: 12px; border-top: 1px solid #eddcd0; }
.signup-consents h3 { margin: 8px 0 18px; font-size: 17px; color: #3b281f; }
.consent-document { border: 1px solid #eadbd1; border-radius: 14px; background: #fff; overflow: hidden; }
.consent-document summary { display: flex; align-items: center; justify-content: space-between; gap: 8px; min-height: 52px; padding: 12px; color: #50382c; font-size: 13px; font-weight: 700; cursor: pointer; list-style: none; }
.consent-document summary::-webkit-details-marker { display: none; }
.consent-document summary span { flex-shrink: 0; color: #98715d; font-size: 11px; }
.consent-document summary::after { content: '+'; font-size: 18px; }
.consent-document[open] summary::after { content: '−'; }
.consent-body { max-height: 280px; overflow-y: auto; padding: 0 14px 14px; border-top: 1px solid #f0e6df; }
.signup-consents .consent-check { display: flex; align-items: flex-start; gap: 10px; padding: 14px 0; margin: 0; min-height: 48px; font-size: 13px; font-weight: 500; line-height: 1.6; cursor: pointer; }
.consent-check input { appearance: auto; flex: 0 0 20px; width: 20px; height: 20px; margin: 1px 0 0; padding: 0; border-radius: 4px; accent-color: #a95238; box-shadow: none; }
.consent-check b { color: #a95238; font-weight: 600; }
.consent-note { margin: 0; font-size: 12px; line-height: 1.6; color: #806c60; }
</style>
