<template>
  <section class="profile-page">
    <div class="profile-shell">
      <header class="page-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Edit Profile</p>
            <h1>프로필 수정</h1>
            <p class="description">프로필 필수 항목을 수정하고 심사 진행에 필요한 정보를 채울 수 있습니다.</p>
          </div>
          <button class="ghost-button" type="button" @click="goBack">돌아가기</button>
        </div>
      </header>

      <p class="review-hint">승인된 회원의 프로필·사진 변경은 재심사 후 공개됩니다. 승인 전에는 기존 내용이 보입니다.</p><PhotoEditor/>
      <section class="page-card">
        <form class="profile-form" @submit.prevent="saveProfile">
          <label>
            <span>닉네임</span>
            <input v-model.trim="form.nickname" type="text" maxlength="30" required />
          </label>

          <label>
            <span>생년월일</span>
            <input v-model="form.birthDate" type="date" required />
          </label>

          <label>
            <span>성별</span>
            <select v-model="form.gender" required>
              <option value="MALE">남성</option>
              <option value="FEMALE">여성</option>
            </select>
          </label>

          <div class="region-grid">
            <label>
              <span>거주 시/도</span>
              <select v-model="selectedRegionCity" @change="handleRegionCityChange" required>
                <option value="">시/도 선택</option>
                <option v-for="city in regionCities" :key="city" :value="city">{{ city }}</option>
              </select>
            </label>

            <label>
              <span>거주 구/군</span>
              <select v-model="selectedRegionDistrict" @change="updateRegion" :disabled="!selectedRegionCity" required>
                <option value="">구/군 선택</option>
                <option v-for="district in availableDistricts" :key="district" :value="district">{{ district }}</option>
              </select>
            </label>
          </div>

          <label>
            <span>직업</span>
            <input v-model.trim="form.job" type="text" maxlength="100" required />
          </label>

          <label>
            <span>MBTI</span>
            <select v-model="form.mbti" required>
              <option value="">MBTI 선택</option>
              <option v-for="item in mbtiOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>

          <label>
            <span>내 성격</span>
            <textarea v-model.trim="form.personality" rows="3" required></textarea>
          </label>

          <label>
            <span>이상형</span>
            <textarea v-model.trim="form.idealType" rows="3" required></textarea>
          </label>

          <label>
            <span>자기소개</span>
            <textarea v-model.trim="form.introduction" rows="4" required></textarea>
          </label>

          <div class="triple-grid">
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
                <option value="NONE">비음주</option>
                <option value="SOMETIMES">가끔 음주</option>
                <option value="OFTEN">자주 음주</option>
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
          </div>

          <button class="primary-button" :disabled="saving">{{ saving ? "저장 중..." : "프로필 저장" }}</button>
        </form>

        <p v-if="saveMessage" class="message success">{{ saveMessage }}</p>
        <p v-if="saveError" class="message error">{{ saveError }}</p>
      </section>
    </div>
  </section>
</template>

<script setup>
import PhotoEditor from "../../components/PhotoEditor.vue";
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const router = useRouter();
const saving = ref(false);
const saveMessage = ref("");
const saveError = ref("");
const selectedRegionCity = ref("");
const selectedRegionDistrict = ref("");

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

const regionCities = Object.keys(regionOptions);
const availableDistricts = computed(() => regionOptions[selectedRegionCity.value] || []);

const form = reactive({
  nickname: "",
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
});

function updateRegion() {
  form.region = selectedRegionCity.value && selectedRegionDistrict.value
    ? `${selectedRegionCity.value} ${selectedRegionDistrict.value}`
    : "";
}

function handleRegionCityChange() {
  selectedRegionDistrict.value = "";
  updateRegion();
}

function hydrateForm(data) {
  form.nickname = data.nickname || "";
  form.birthDate = data.birthDate || "";
  form.gender = data.gender || "MALE";
  form.region = data.region || "";
  form.job = data.job || "";
  form.mbti = data.mbti || "";
  form.personality = data.personality || "";
  form.idealType = data.idealType || "";
  form.introduction = data.introduction || "";
  form.smokingStatus = data.smokingStatus || "NON_SMOKER";
  form.drinkingStatus = data.drinkingStatus || "NONE";
  form.religion = data.religion || "NONE";

  const [city = "", ...districtParts] = (data.region || "").split(" ");
  selectedRegionCity.value = regionOptions[city] ? city : "";
  selectedRegionDistrict.value = selectedRegionCity.value ? districtParts.join(" ") : "";
  updateRegion();
}

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
    hydrateForm(await fetchProfileWithFallback());
  } catch (error) {
    clearToken();
    router.replace("/");
  }
}

function validateProfile() {
  if (!form.nickname.trim()) return "닉네임을 입력해 주세요.";
  if (!form.birthDate) return "생년월일을 입력해 주세요.";
  if (!form.gender) return "성별을 선택해 주세요.";
  if (!selectedRegionCity.value) return "거주 시/도를 선택해 주세요.";
  if (!selectedRegionDistrict.value) return "거주 구/군을 선택해 주세요.";
  if (!form.job.trim()) return "직업을 입력해 주세요.";
  if (!form.mbti.trim()) return "MBTI를 입력해 주세요.";
  if (!form.personality.trim()) return "성격을 입력해 주세요.";
  if (!form.idealType.trim()) return "이상형을 입력해 주세요.";
  if (!form.introduction.trim()) return "자기소개를 입력해 주세요.";
  if (!form.smokingStatus) return "흡연 여부를 선택해 주세요.";
  if (!form.drinkingStatus) return "음주 여부를 선택해 주세요.";
  if (!form.religion) return "종교를 선택해 주세요.";
  return "";
}

async function saveProfile() {
  saveMessage.value = "";
  saveError.value = "";

  const validationMessage = validateProfile();
  if (validationMessage) {
    saveError.value = validationMessage;
    return;
  }

  saving.value = true;

  try {
    const payload = {
      ...form,
      nickname: form.nickname.trim(),
      region: form.region.trim(),
      job: form.job.trim(),
      mbti: form.mbti.trim(),
      personality: form.personality.trim(),
      idealType: form.idealType.trim(),
      introduction: form.introduction.trim(),
    };

    const { data } = await api.put("/profile/me", payload);
    hydrateForm(data);
    saveMessage.value = "프로필을 저장했어요.";
    setTimeout(() => router.push("/profile"), 500);
  } catch (error) {
    saveError.value = error.response?.data?.message || "프로필 저장에 실패했습니다.";
  } finally {
    saving.value = false;
  }
}

function goBack() {
  router.push("/profile");
}

onMounted(loadProfile);
</script>

<style scoped>
.profile-page { min-height: 100vh; padding: max(16px, env(safe-area-inset-top)) 16px 24px; display: flex; justify-content: center; }
.profile-shell { width: min(100%, 720px); display: grid; gap: 16px; }
.page-card {
  padding: 24px 20px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
}
.eyebrow { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1 { margin: 0; color: #31211d; }
.description { margin: 12px 0 0; color: #6f564d; line-height: 1.7; }
.section-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.profile-form { display: grid; gap: 14px; }
label { display: grid; gap: 8px; color: #543932; font-weight: 600; }
input, select, textarea { border: 1px solid #e5c2b2; border-radius: 16px; padding: 14px 16px; font-size: 15px; background: rgba(255,255,255,0.92); font-family: inherit; }
textarea { resize: vertical; }
.region-grid { display: grid; gap: 14px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.triple-grid { display: grid; gap: 14px; grid-template-columns: repeat(3, minmax(0, 1fr)); }
.primary-button, .ghost-button { border-radius: 999px; padding: 12px 18px; font-size: 14px; font-weight: 700; cursor: pointer; }
.primary-button { border: none; background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%); color: #fff; }
.ghost-button { border: 1px solid rgba(207, 150, 128, 0.36); background: rgba(255,248,243,0.82); color: #6a4137; }
.message { margin-top: 14px; font-weight: 700; }
.success { color: #1f7a43; }
.error { color: #b72f2f; }
@media (max-width: 760px) {
  .region-grid, .triple-grid { grid-template-columns: 1fr; }
  .section-head { flex-direction: column; align-items: flex-start; }
}
</style>
