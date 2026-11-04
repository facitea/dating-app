<template>
  <div :class="{ 'mobile-client': !isAdmin }">
    <router-view />
    <CreditShortageDialog /><MatchCelebration v-if="!isAdmin"/>
    <AdminPhotoZoom v-if="isAdmin"/>
  </div>
</template>

<script setup>
import MatchCelebration from './components/MatchCelebration.vue';
import { computed,onMounted } from 'vue';
import {Capacitor} from '@capacitor/core';
import {useRouter} from 'vue-router';
import {initNativePush,hasNativePush,enableNativePush} from './components/nativePush';
const router=useRouter();onMounted(async()=>{if(Capacitor.isNativePlatform()){await initNativePush(router);try{if(await hasNativePush())await enableNativePush();}catch{}}});
import AdminPhotoZoom from "./components/AdminPhotoZoom.vue";
import CreditShortageDialog from './components/CreditShortageDialog.vue';
import { useRoute } from 'vue-router';
import './mobile.css';

const route = useRoute();
const isAdmin = computed(() => route.path.startsWith('/admin/'));
</script>
