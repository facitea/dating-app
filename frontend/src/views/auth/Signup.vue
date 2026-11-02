<template>
 <section class="signup-wizard">
  <form class="wizard-shell" novalidate @submit.prevent="advance">
   <header class="wizard-header"><RouterLink to="/" aria-label="가입 나가기">‹</RouterLink><span>그럼우리</span><small>{{step+1}} / {{titles.length}}</small></header>
   <div class="wizard-progress" role="progressbar" aria-label="가입 진행" :aria-valuenow="step+1" :aria-valuemax="titles.length" aria-valuemin="1"><span :style="{width:`${(step+1)/titles.length*100}%`}"/></div>
   <div class="wizard-stage" ref="stepPanel">
    <Transition :name="direction==='next'?'step-next':'step-back'" mode="out-in">
     <section :key="step" class="wizard-step"><h1>{{titles[step]}}</h1>
      <template v-if="step===0"><label>닉네임<input ref="nicknameInput" v-model.trim="form.nickname" maxlength="30" autocomplete="nickname" placeholder="서비스에 표시될 이름"/></label><label>이메일<input ref="emailInput" v-model.trim="form.email" :readonly="googleSignup" type="email" autocomplete="email" placeholder="you@example.com"/></label><p v-if="googleSignup" class="step-note">구글 이메일 인증 완료</p></template>
      <template v-else-if="step===1"><p v-if="googleSignup" class="step-note">구글 계정으로 인증됐어요. 별도 비밀번호 없이 이용할 수 있어요.</p><template v-else><button type="button" class="secondary" :disabled="verificationLoading" @click="requestEmailVerification">{{verificationLoading?'발급 중…':'이메일 인증번호 받기'}}</button><label>인증번호<input ref="emailCodeInput" v-model.trim="emailVerificationCode" inputmode="numeric" maxlength="6" placeholder="6자리 인증번호"/></label><button type="button" class="secondary" :disabled="verificationLoading||!emailVerificationCode" @click="confirmEmailVerification">{{emailVerified?'인증 완료':'인증번호 확인'}}</button><p v-if="verificationMessage" class="step-note">{{verificationMessage}}</p><label>비밀번호<input ref="passwordInput" v-model="form.password" type="password" autocomplete="new-password" minlength="8" maxlength="100" placeholder="8자 이상"/></label></template></template>
      <template v-else-if="step===2"><label>생년월일<input ref="birthDateInput" v-model="form.birthDate" type="date"/></label><label>성별<select ref="genderSelect" v-model="form.gender"><option value="MALE">남성</option><option value="FEMALE">여성</option></select></label><p class="step-note">만 19세 이상 이용할 수 있어요.</p></template>
      <template v-else-if="step===3"><label>거주 시/도<select ref="regionCitySelect" v-model="selectedRegionCity" @change="handleRegionCityChange"><option value="">시/도 선택</option><option v-for="city in regionCities" :key="city">{{city}}</option></select></label><label>거주 구/군<select ref="regionDistrictSelect" v-model="selectedRegionDistrict" @change="updateRegion" :disabled="!selectedRegionCity"><option value="">구/군 선택</option><option v-for="district in availableDistricts" :key="district">{{district}}</option></select></label></template>
      <template v-else-if="step===4"><label>직업<input v-model.trim="form.job" maxlength="100" placeholder="예: 개발자"/></label><label>MBTI<select v-model="form.mbti"><option value="">선택해 주세요</option><option v-for="item in mbtiOptions" :key="item">{{item}}</option></select></label></template>
      <template v-else-if="step===5"><label>흡연 여부<select v-model="form.smokingStatus"><option value="NON_SMOKER">비흡연</option><option value="OCCASIONAL">가끔 흡연</option><option value="SMOKER">흡연</option></select></label></template>
      <template v-else-if="step===6"><label>음주 여부<select v-model="form.drinkingStatus"><option value="NONE">안 함</option><option value="SOMETIMES">가끔</option><option value="OFTEN">자주</option></select></label><label>종교<select v-model="form.religion"><option value="NONE">없음</option><option value="CHRISTIAN">기독교</option><option value="CATHOLIC">천주교</option><option value="BUDDHIST">불교</option><option value="OTHER">기타</option></select></label></template>
      <TraitSelector v-else-if="step===7" v-model="form.traitsJson" scope="personality"/>
      <TraitSelector v-else-if="step===8" v-model="form.traitsJson" scope="more"/>
      <TraitSelector v-else-if="step===9" v-model="form.traitsJson" scope="personality" mode="preferences"/>
      <TraitSelector v-else-if="step===10" v-model="form.traitsJson" scope="more" mode="preferences"/>
      <template v-else-if="step===11"><label>자기소개<textarea v-model.trim="form.introduction" maxlength="2000" rows="5" placeholder="함께하고 싶은 일이나 나다운 이야기를 적어주세요."/></label><p class="step-note">성향 키워드는 가입 후 프로필 편집에서도 바꿀 수 있어요.</p></template>
      <template v-else-if="step===12"><div class="terms-scroll" tabindex="0" role="region" aria-label="서비스 이용약관 본문"><TermsText type="terms"/></div><label class="consent-check"><input ref="termsCheckbox" v-model="form.agreedToTerms" type="checkbox"/><span>[필수] 서비스 이용약관에 동의합니다.</span></label></template>
      <template v-else-if="step===13"><div class="terms-scroll privacy-scroll" tabindex="0" role="region" aria-label="개인정보 수집 이용 안내 본문"><TermsText type="privacy"/></div><label class="consent-check"><input v-model="form.agreedToPrivacy" type="checkbox"/><span>[필수] 개인정보 수집·이용에 동의합니다.</span></label><label class="consent-check"><input v-model="form.adultConfirmed" type="checkbox"/><span>[필수] 만 19세 이상입니다.</span></label><label class="consent-check"><input v-model="form.marketingConsent" type="checkbox"/><span>[선택] 이벤트·마케팅 안내 수신</span></label></template>
     </section>
    </Transition>
   </div>
   <footer class="wizard-footer"><p v-if="errorMessage" class="wizard-error" role="alert">{{errorMessage}}</p><div><button v-if="step>0" type="button" class="secondary" :disabled="loading" @click="moveStep(step-1)">이전</button><button class="primary" :disabled="loading||verificationLoading">{{loading?'저장 중…':step===titles.length-1?'동의하고 사진 등록':'다음'}}</button></div><p v-if="step===titles.length-1" class="step-note">사진 등록 후 운영자 심사를 거쳐 가입이 완료돼요.</p></footer>
   <label class="honeypot-field" aria-hidden="true"><span>웹사이트</span><input v-model="form.website" tabindex="-1" autocomplete="off"/></label>
  </form>
 </section>
</template>
<script setup>
import { pendingGoogle, googleToken, clearGoogle } from '../../auth-client/googleAuth';
import TraitSelector from '../../components/TraitSelector.vue';
import {serializeTraits,traitSummary} from '../../components/traits';
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
const googleSignup=ref(Boolean(pendingGoogle.value));
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
  email: googleSignup.value ? pendingGoogle.value.email : "",
  emailVerificationToken: "",
  password: "",
  birthDate: "",
  gender: "MALE",
  region: "",
  job: "",
  mbti: "",
  traitsJson: "",
  personality: "",
  idealType: "",
  introduction: "",
  smokingStatus: "NON_SMOKER",
  drinkingStatus: "NONE",
  religion: "NONE",
  agreedToTerms: false,
  agreedToPrivacy:false,adultConfirmed:false,marketingConsent:false,termsVersion:"2026-11-02-traits-test",
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
  const fieldSteps={nickname:0,email:0,emailCode:1,password:1,birthDate:2,gender:2,regionCity:3,regionDistrict:3,agreedToTerms:12};
  if(fieldSteps[field]!==undefined)step.value=fieldSteps[field];
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

  if (!googleSignup.value && !form.emailVerificationToken) {
    return {
      field: "emailCode",
      message: "이메일 인증을 먼저 완료해주세요.",
    };
  }

  if (!googleSignup.value && form.password.length < 8) {
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
      errorMessage.value=validationResult.message;
      await focusField(validationResult.field);
      return;
    }

    form.traitsJson=serializeTraits(form.traitsJson);
    form.personality=traitSummary(form.traitsJson);
    form.idealType=traitSummary(form.traitsJson,'preferences');
    const payload=googleSignup.value?{...form,password:null,emailVerificationToken:null,firebaseIdToken:await googleToken()}:form;
    const data = await authClient.signup(payload);
    resetSignupApprovalNotice();
    setAuthTokens(data);
    router.push("/review-pending");
    if(googleSignup.value)await clearGoogle();
  } catch (error) {
    const message = error.response?.data?.message || "회원가입에 실패했습니다. 입력값을 다시 확인해주세요.";
    errorMessage.value = message;


    if (message.includes("닉네임")) {
      await focusField("nickname");
    } else if (message.includes("이메일")) {
      await focusField("email");
    }
  } finally {
    loading.value = false;
  }
};
const step=ref(0),direction=ref('next'),stepPanel=ref();
const titles=['어떤 이름으로 불러드릴까요?','계정을 안전하게 만들어요','기본 정보를 알려주세요','어디에 살고 계신가요?','일과 MBTI를 알려주세요','흡연 습관을 알려주세요','음주와 종교를 알려주세요','나는 어떤 사람인가','나의 소통·연애·생활','어떤 성격에 끌리나요?','함께할 때 바라는 성향','나를 소개해 주세요','서비스 이용약관','개인정보와 가입 동의'];
async function moveStep(next){direction.value=next>step.value?'next':'back';step.value=next;errorMessage.value='';await nextTick();stepPanel.value?.scrollTo({top:0});}
async function advance(){
 if(loading.value||verificationLoading.value)return;
 errorMessage.value='';
 if(step.value===0&&(form.nickname.length<2||form.nickname.length>30||!/^\S+@\S+\.\S+$/.test(form.email))){errorMessage.value='닉네임 2~30자와 올바른 이메일을 입력해 주세요.';return;}
 if(step.value===1&&!googleSignup.value&&(!form.emailVerificationToken||form.password.length<8)){errorMessage.value='이메일 인증을 완료하고 8자 이상 비밀번호를 입력해 주세요.';return;}
 if(step.value===2){const v=validateForm();if(v&&['birthDate','gender'].includes(v.field)){errorMessage.value=v.message;return;}}
 if(step.value===3&&(!selectedRegionCity.value||!selectedRegionDistrict.value)){errorMessage.value='거주 시/도와 구/군을 선택해 주세요.';return;}
 if(step.value===4&&(!form.job.trim()||!form.mbti)){errorMessage.value='직업과 MBTI를 선택해 주세요.';return;}
 if(step.value===11&&!form.introduction.trim()){errorMessage.value='자기소개를 입력해 주세요.';return;}
 if(step.value===12&&!form.agreedToTerms){errorMessage.value='이용약관을 읽고 동의해 주세요.';return;}
 if(step.value===titles.length-1){await signup();return;}
 await moveStep(step.value+1);
}
</script>

<style scoped>
.signup-wizard{height:100dvh;box-sizing:border-box;background:#fff9f4;color:#50382c;padding:env(safe-area-inset-top) 0 env(safe-area-inset-bottom);overflow:hidden}.wizard-shell{max-width:480px;height:100%;margin:auto;display:flex;flex-direction:column;min-height:0}.wizard-header{display:flex;align-items:center;gap:12px;padding:12px 20px;flex:none}.wizard-header a{font-size:30px;color:#a65b40;text-decoration:none;width:32px}.wizard-header span{font-weight:750;font-size:16px}.wizard-header small{margin-left:auto;font-size:12px;color:#987d6c}.wizard-progress{height:3px;background:#efdfd3;flex:none;margin:0 22px}.wizard-progress span{display:block;height:100%;background:#d56f4e;transition:width .2s}.wizard-stage{flex:1;min-height:0;overflow-y:auto;overflow-x:hidden;padding:22px 22px 8px;overscroll-behavior:contain}.wizard-step h1{font-size:24px!important;letter-spacing:-.8px;margin:0 0 24px;line-height:1.35}.wizard-step>label{display:grid;gap:8px;margin:18px 0;font-size:13px;font-weight:600}.wizard-step input,.wizard-step select,.wizard-step textarea{box-sizing:border-box;width:100%;font:inherit;font-size:16px;border:1px solid #e9d1c1;border-radius:14px;background:#fff;color:#50382c;padding:13px;min-height:48px}.wizard-step textarea{resize:none}.step-note{font-size:12px;color:#947568;line-height:1.6}.wizard-footer{flex:none;padding:12px 22px 16px;background:#fff9f4;border-top:1px solid #f1e4da}.wizard-footer>div{display:flex;gap:10px}.wizard-footer .primary{flex:1}.primary,.secondary{font:inherit;font-size:14px;min-height:48px;padding:12px 18px;border-radius:14px;cursor:pointer}.primary{background:#bd6546;color:#fff;border:0;font-weight:700}.secondary{background:#fff;border:1px solid #e9d1c1;color:#916248}.primary:disabled,.secondary:disabled{opacity:.5;cursor:default}.wizard-error{color:#b44334;font-size:12px;line-height:1.5;margin:0 0 10px}.terms-scroll{max-height:calc(100dvh - 310px);overflow-y:auto;border:1px solid #efdfd3;border-radius:14px;background:#fff;padding:12px}.privacy-scroll{max-height:calc(100dvh - 420px)}.wizard-step .consent-check{display:flex;align-items:center;gap:10px;font-size:12px;margin:14px 0}.consent-check input{width:20px;height:20px;min-height:20px;padding:0;accent-color:#bd6546;flex:none}.honeypot-field{position:absolute;left:-10000px;width:1px;height:1px;overflow:hidden}.step-next-enter-active,.step-next-leave-active,.step-back-enter-active,.step-back-leave-active{transition:transform .18s ease,opacity .18s ease}.step-next-enter-from{transform:translateX(36px);opacity:0}.step-next-leave-to{transform:translateX(-36px);opacity:0}.step-back-enter-from{transform:translateX(-36px);opacity:0}.step-back-leave-to{transform:translateX(36px);opacity:0}@media(prefers-reduced-motion:reduce){.step-next-enter-active,.step-next-leave-active,.step-back-enter-active,.step-back-leave-active,.wizard-progress span{transition:none}}@media(max-height:650px){.wizard-header{padding-block:6px}.wizard-stage{padding-top:14px}.wizard-step h1{font-size:21px!important;margin-bottom:16px}.wizard-footer{padding-top:8px;padding-bottom:8px}.wizard-step>label{margin:12px 0}}
</style>