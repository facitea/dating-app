import {ref} from 'vue';
export const adminAccount=ref(null);
export const adminCsrf=ref('');
export function setAdmin(data){adminAccount.value=data.account||data;adminCsrf.value=data.csrf||'';}
export function clearAdmin(){adminAccount.value=null;adminCsrf.value='';sessionStorage.removeItem('dating-admin-key');localStorage.removeItem('adminReviewKey');}
