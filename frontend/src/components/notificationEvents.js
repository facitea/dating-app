import api from '../api/api';

// Successful acknowledgements are cached only for this mounted app/account.
const seen = new Set();
let account = '';
export function resetNotificationCache(id) { if(account!==String(id)){seen.clear();account=String(id);} }
export function refreshBadges() { window.dispatchEvent(new Event('notifications-changed')); }
export async function acknowledgeNotifications(keys) {
  if(document.hidden)return;
  const pending=[...new Set(keys.filter(k=>k&&!seen.has(k)))];
  if(!pending.length)return;
  try {
    for(let i=0;i<pending.length;i+=200){const batch=pending.slice(i,i+200);await api.post('/social/notifications/seen',{keys:batch});batch.forEach(k=>seen.add(k));}
    refreshBadges();
  } catch { /* A failed read acknowledgement keeps the badge and retries on the next successful refresh. */ }
}
