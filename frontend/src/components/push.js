import api from '../api/api';
export const supported=()=>window.isSecureContext && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
async function bounded(promise){let timer;try{return await Promise.race([promise,new Promise((_,reject)=>{timer=setTimeout(()=>reject(Error('알림 권한 요청이 대기 중입니다. 브라우저에서 허용한 뒤 다시 눌러 주세요.')),15000);})]);}finally{clearTimeout(timer);}}
export async function enablePush(){
 if(!supported())throw Error('이 브라우저에서는 푸시를 지원하지 않아요. HTTPS 주소의 Chrome 또는 설치한 앱에서 이용해 주세요.');
 const permission=await bounded(Notification.requestPermission());if(permission!=='granted')throw Error('브라우저 설정에서 알림을 허용해 주세요.');
 const {data}=await api.get('/social/push/config');if(!data.enabled)throw Error('푸시 서버 설정이 필요합니다. 알림함은 계속 이용할 수 있어요.');
 const registration=await navigator.serviceWorker.register('/sw.js');await bounded(navigator.serviceWorker.ready);
 let subscription=await registration.pushManager.getSubscription();if(!subscription){const key=data.publicKey.replace(/-/g,'+').replace(/_/g,'/');const bytes=Uint8Array.from(atob(key),c=>c.charCodeAt(0));subscription=await bounded(registration.pushManager.subscribe({userVisibleOnly:true,applicationServerKey:bytes}));}
 await api.post('/social/push/subscription',subscription.toJSON());return true;
}
export async function disablePush(){const registration=await navigator.serviceWorker.getRegistration();const sub=await registration?.pushManager.getSubscription();if(sub){await api.delete('/social/push/subscription',{data:{endpoint:sub.endpoint}});await sub.unsubscribe();}}
export async function hasPush(){if(!supported())return false;const reg=await navigator.serviceWorker.getRegistration();return !!(await reg?.pushManager.getSubscription());}
export async function detachPush(token){try{const reg=await navigator.serviceWorker?.getRegistration();const sub=await reg?.pushManager.getSubscription();if(sub){await fetch(api.defaults.baseURL+'/social/push/subscription',{method:'DELETE',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify({endpoint:sub.endpoint}),keepalive:true});await sub.unsubscribe();}}catch{}}
