import api from '../api/api';
export async function safetyPost(url,payload){
 try{return await api.post(url,payload);}catch(e){
  const message=e.response?.data?.message||'';
  if(e.response?.status!==409||!message.startsWith('SAFETY_WARNING:'))throw e;
  if(!window.confirm(message.replace('SAFETY_WARNING: ','')+'\n\n그래도 전송하시겠어요?'))return null;
  return api.post(url,{...payload,safetyAcknowledged:true});
 }
}
