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

        <label class="checkbox-row">
          <input ref="termsCheckbox" v-model="form.agreedToTerms" type="checkbox" />
          <span>만 19세 이상이며 서비스 이용약관과 개인정보 처리에 동의합니다.</span>
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
import { computed, nextTick, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../api/api";
import { resetSignupApprovalNotice, setToken } from "../utils/auth";

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
const errorMessage = ref("");
const nicknameInput = ref(null);
const emailInput = ref(null);
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
});

const regionCities = Object.keys(regionOptions);
const availableDistricts = computed(() => regionOptions[selectedRegionCity.value] || []);

const updateRegion = () => {
  form.region = selectedRegionCity.value && selectedRegionDistrict.value
    ? `${selectedRegionCity.value} ${selectedRegionDistrict.value}`
    : "";
};

const handleRegionCityChange = () => {
  selectedRegionDistrict.value = "";
  updateRegion();
};

const focusField = async (field) => {
  await nextTick();

  const focusTargets = {
    nickname: nicknameInput,
    email: emailInput,
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

  if (!form.agreedToTerms) {
    return {
      field: "agreedToTerms",
      message: "약관 동의는 필수입니다.",
    };
  }

  return null;
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

    const { data } = await api.post("/auth/signup", form);
    resetSignupApprovalNotice();
    setToken(data.token);
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
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #fff7e7 0%, #ffe0b8 45%, #ffd2b0 100%);
}

.auth-card {
  width: min(100%, 560px);
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.94);
  box-shadow: 0 18px 45px rgba(109, 70, 36, 0.14);
}

.eyebrow {
  margin: 0 0 10px;
  color: #b35a28;
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
  color: #6a5246;
  line-height: 1.5;
}

.section-title {
  margin-top: 4px;
  color: #8d4b27;
  font-size: 14px;
  font-weight: 800;
}

.auth-form {
  display: grid;
  gap: 16px;
}

label {
  display: grid;
  gap: 8px;
  color: #4d382d;
  font-weight: 600;
}

.region-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

input,
select,
textarea {
  border: 1px solid #ebc7a9;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  font-family: inherit;
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
}

.checkbox-row input {
  margin-top: 3px;
}

.primary-button {
  margin-top: 8px;
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  background: #cd6d2d;
  color: #fff;
}

.message {
  margin-top: 16px;
}

.error {
  color: #b72f2f;
}

.policy-box {
  margin-top: 20px;
  padding: 16px;
  border-radius: 16px;
  background: #fff7ed;
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
}

.helper-text a {
  color: #a3461a;
  font-weight: 700;
}

@media (max-width: 640px) {
  .region-grid {
    grid-template-columns: 1fr;
  }
}
</style>