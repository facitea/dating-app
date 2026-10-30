import api from './api/api';
const state = new WeakMap();
function clear(el) { const s=state.get(el); if(s){s.cancel.abort();if(s.url)URL.revokeObjectURL(s.url);} state.delete(el);el.removeAttribute('src'); }
async function update(el,binding){
 if(binding.value===binding.oldValue&&state.has(el))return;
 clear(el);if(!binding.value)return;
 const s={cancel:new AbortController(),url:null};state.set(el,s);
 try {
  const url=new URL(binding.value,window.location.origin);
  if(url.protocol==='blob:'){el.src=url.href;return;}
  if(!url.pathname.startsWith('/uploads/'))throw new Error('Unsupported image');
  const endpoint=(binding.modifiers.admin?'/admin/media/':'/media/')+url.pathname.slice('/uploads/'.length);
  const {data}=await api.get(endpoint,{responseType:'blob',signal:s.cancel.signal});
  if(state.get(el)!==s)return;s.url=URL.createObjectURL(data);el.src=s.url;
 }catch(e){if(!s.cancel.signal.aborted)el.dispatchEvent(new Event('error'));}
}
export default {mounted:update,updated:update,beforeUnmount:clear};
