import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
export function installNativeNavigation(router) {
 if(!Capacitor.isNativePlatform())return;
 document.documentElement.classList.add('native-app');
 App.addListener('backButton',()=>{
  const dialog=[...document.querySelectorAll('dialog[open]')].pop();
  if(dialog){const event=new Event('cancel',{cancelable:true});if(dialog.dispatchEvent(event))dialog.close();return;}
  const path=router.currentRoute.value.path;
  if(['/today','/matches','/chats','/profile','/'].includes(path)){App.minimizeApp();return;}
  if(window.history.state?.back)router.back();else router.replace('/today');
 });
 App.addListener('appStateChange',({isActive})=>{if(isActive)window.dispatchEvent(new Event('focus'));});
}
