<template>
  <div :class="{ 'mobile-client': !isAdmin }">
    <router-view />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import './mobile.css';

const route = useRoute();
const isAdmin = computed(() => route.path.startsWith('/admin/'));
</script>
