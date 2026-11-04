import {readFileSync} from 'node:fs';
import vm from 'node:vm';import assert from 'node:assert/strict';import axios from 'axios';
const source=readFileSync(new URL('../src/api/api.js',import.meta.url),'utf8');
let token='expired',clears=0,refreshes=0,mode='ok';const instances=[];
const context=vm.createContext({window:{location:{origin:'https://localhost',pathname:'/today'}},console});
const dependencies={
 '@capacitor/core':{Capacitor:{isNativePlatform:()=>true}},
 'axios':{default:{create(config){const instance=axios.create(config);instances.push(instance);return instance;}}},
 '../adminSession':{adminCsrf:{value:null},clearAdmin(){}},
 '../utils/auth':{getToken:()=>token,clearToken(){clears++;token=null;},setAuthTokens(data){token=data.token;}}
};
const module=new vm.SourceTextModule(source,{context,initializeImportMeta(meta){meta.env={VITE_API_BASE_URL:'https://example.test/api'};}});
await module.link(specifier=>{const exports=dependencies[specifier];return new vm.SyntheticModule(Object.keys(exports),function(){for(const [k,v] of Object.entries(exports))this.setExport(k,v);},{context});});await module.evaluate();
const [api,refresh]=instances;const reject=(config,status)=>Promise.reject(new axios.AxiosError('test',null,config,null,status?{status,data:{}}:undefined));
api.defaults.adapter=async config=>config.headers.Authorization==='Bearer valid'?{config,status:200,data:{ok:true},headers:{}}:reject(config,401);
refresh.defaults.adapter=async config=>{refreshes++;await new Promise(r=>setTimeout(r,25));if(mode==='offline')return reject(config);if(mode==='invalid')return reject(config,401);return {config,status:200,data:{token:'valid'},headers:{}};};
await Promise.all(Array.from({length:12},()=>api.get('/profile/me')));assert.equal(refreshes,1);assert.equal(clears,0);assert.equal(token,'valid');
token='expired';mode='offline';await assert.rejects(api.get('/profile/me'));assert.equal(token,'expired');assert.equal(clears,0);
mode='invalid';await assert.rejects(api.get('/profile/me'));assert.equal(token,null);assert.equal(clears,1);
const before=refreshes;await assert.rejects(api.post('/auth/firebase/email'));assert.equal(refreshes,before);
console.log('PASS: concurrent refresh, offline session retention, revoked session cleanup, no auth retry loop');
