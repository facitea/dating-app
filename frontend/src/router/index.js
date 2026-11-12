import { createRouter, createWebHistory } from "vue-router";
import api from "../api/api";
import { hasToken } from "../utils/auth";
import AdminReviews from "../views/admin/AdminReviews.vue";
import ScamMonitor from "../views/admin/ScamMonitor.vue";
import Login from "../views/auth/Login.vue";
import ResetPassword from "../views/auth/ResetPassword.vue";
import Signup from "../views/auth/Signup.vue";
import SocialLogin from "../views/auth/SocialLogin.vue";
import Chats from "../views/user/Chats.vue";
import MatchStatus from "../views/user/MatchStatus.vue";
import ChatRoom from "../views/user/ChatRoom.vue";
import Profile from "../views/user/Profile.vue";
import ProfileEdit from "../views/user/ProfileEdit.vue";
import ProfilePhoneVerify from "../views/user/ProfilePhoneVerify.vue";
import ReviewPending from "../views/user/ReviewPending.vue";
import SignupComplete from "../views/user/SignupComplete.vue";
import SupportCenter from "../views/user/SupportCenter.vue";
import Today from "../views/user/Today.vue";
import UserLayout from "../views/user/UserLayout.vue";

import MemberProfile from '../views/user/MemberProfile.vue';
import Notifications from '../views/user/Notifications.vue';
import Credits from '../views/user/Credits.vue';
import Photos from '../views/user/Photos.vue';
import Settings from '../views/user/Settings.vue';
import AdminConsole from '../views/admin/AdminConsole.vue';
const routes = [
  { path: "/legal", component: () => import("../views/Legal.vue") },
  { path: "/", component: Login, meta: { guestOnly: true } },
  { path: "/admin/reviews", redirect: "/admin/signup" },
  { path: "/admin/safety", redirect: "/admin/support" },
  { path: "/admin", redirect: "/admin/overview" },
  { path: "/admin/login", redirect: "/admin/overview" },
  { path: "/admin/:section", component: AdminConsole },
  { path: "/signup", component: Signup, meta: { guestOnly: true } },
  { path: "/reset-password", component: ResetPassword },
  { path: "/review-pending", component: ReviewPending, meta: { requiresAuth: true } },
  { path: "/signup-complete", redirect: "/today" },
  { path: "/selfie-required", component: () => import("../views/user/SelfieRequired.vue"), meta: { requiresAuth: true } },
  { path: "/social-login", component: SocialLogin },
  {
    path: "/app",
    component: UserLayout,
    meta: { requiresAuth: true, userApp: true },
    children: [
      { path: "", redirect: "/today" },
      { path: "/home", redirect: "/today" },
      { path: "/today", component: Today },
      { path: "/matches", component: MatchStatus },
      { path: "/community", component: () => import("../views/user/Community.vue") },
      { path: "/chats", component: Chats },
      { path: "/chats/:id", component: ChatRoom },
      { path: "/members/:id", component: MemberProfile },
      { path: "/notifications", component: Notifications },
      { path: "/credits", component: Credits },
      { path: "/profile/photos", component: Photos },
      { path: "/settings", component: Settings },
      { path: "/profile/selfie", redirect: "/selfie-required" },
      { path: "/profile", component: Profile },
      { path: "/profile/edit", component: ProfileEdit },
      { path: "/profile/phone", component: ProfilePhoneVerify },
      { path: "/support", component: SupportCenter },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async (to) => {
  if (hasToken() && !to.path.startsWith("/admin") && !["/selfie-required", "/support", "/legal", "/settings", "/profile/photos"].includes(to.path)) { try { if ((await api.get("/selfie/request")).data.required) return "/selfie-required"; } catch {/* Existing auth guard handles expired sessions. */ } }
  if (to.meta.requiresAuth && !hasToken()) {
    return "/";
  }

  if (to.meta.guestOnly && hasToken()) {
    return "/today";
  }

  return true;
});

export default router;
