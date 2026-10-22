// Cancel on scrolling; swallow the release click after a long press.
export const vLongpress={
 mounted(el,binding){
  let timer,x,y,fired=false,callback=binding.value;
  const cancel=()=>{clearTimeout(timer);timer=null;};
  const down=e=>{if(e.button!==0)return;cancel();fired=false;x=e.clientX;y=e.clientY;timer=setTimeout(()=>{fired=true;callback();},550);};
  const move=e=>{if(Math.hypot(e.clientX-x,e.clientY-y)>10)cancel();};
  const click=e=>{if(fired){e.preventDefault();e.stopImmediatePropagation();fired=false;}};
  const context=e=>{e.preventDefault();cancel();if(!fired)callback();fired=true;};
  const key=e=>{if(e.key==='ContextMenu'||(e.shiftKey&&e.key==='F10')){e.preventDefault();callback();}};
  const listeners={pointerdown:down,pointermove:move,pointerup:cancel,pointercancel:cancel,pointerleave:cancel,click,contextmenu:context,keydown:key};
  for(const [name,fn] of Object.entries(listeners))el.addEventListener(name,fn,name==='click');
  el._press={update:fn=>callback=fn,dispose:()=>{cancel();for(const [name,fn] of Object.entries(listeners))el.removeEventListener(name,fn,name==='click');}};
 },updated(el,binding){el._press.update(binding.value);},unmounted(el){el._press.dispose();}
};
