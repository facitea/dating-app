import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
const root=fileURLToPath(new URL('../',import.meta.url));
process.chdir(root);
const api=process.env.ANDROID_API_URL;
if(!api)throw Error('Set ANDROID_API_URL to the backend /api address (HTTPS for release).');
const url=new URL(api);
const local=/^(localhost|127\.0\.0\.1|10\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+)$/.test(url.hostname);
if(url.username||url.password||url.search||url.hash||!url.pathname.endsWith('/api'))throw Error('Use an API base URL ending in /api without credentials/query.');
if(url.protocol!=='https:' && !(url.protocol==='http:'&&local&&process.env.ANDROID_DEBUG_HTTP==='1'))throw Error('HTTPS is required; local HTTP needs ANDROID_DEBUG_HTTP=1.');
const google=existsSync('android/app/google-services.json');
if(google){const config=JSON.parse(readFileSync('android/app/google-services.json','utf8'));if(!config.client?.some(c=>c.client_info?.android_client_info?.package_name==='com.facitea.geureomuri'))throw Error('Firebase Android package must be com.facitea.geureomuri');}
const env={...process.env,VITE_API_BASE_URL:api,VITE_NATIVE_FIREBASE:google?'true':'false'};
function run(script,args){const p=spawnSync(process.execPath,[resolve('node_modules',script),...args],{stdio:'inherit',env});if(p.status!==0)process.exit(p.status||1);}
run('vite/bin/vite.js',['build','--mode','android']);
run('@capacitor/cli/bin/capacitor',['sync','android']);
writeFileSync('android/app/src/main/assets/android-build.json',JSON.stringify({apiBaseUrl:api,firebaseConfigured:google}));
// Without Firebase Android config, do not load the native auth plugin at startup.
const plugins='android/app/src/main/assets/capacitor.plugins.json';
if(!google){writeFileSync(plugins,JSON.stringify(JSON.parse(readFileSync(plugins,'utf8')).filter(p=>!p.pkg?.startsWith('@capacitor-firebase/')),null,2));}
mkdirSync('android/app/src/debug/res/xml',{recursive:true});
writeFileSync('android/app/src/debug/res/xml/network_security_config.xml',`<?xml version="1.0" encoding="utf-8"?><network-security-config><base-config cleartextTrafficPermitted="false"/>${url.protocol==='http:'?`<domain-config cleartextTrafficPermitted="true"><domain includeSubdomains="false">${url.hostname}</domain></domain-config>`:''}</network-security-config>`);
console.log(`Android assets ready. Firebase Android: ${google?'configured':'not configured (email login available)'}.`);
