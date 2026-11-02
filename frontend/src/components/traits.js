import catalog from '../data/traitCatalog.json';
export {catalog};
export function parseTraits(raw){try{const d=typeof raw==='string'?JSON.parse(raw):raw;return {answers:d?.answers||{},preferences:d?.preferences||{}};}catch{return {answers:{},preferences:{}};}}
export function serializeTraits(raw){const d=parseTraits(raw);for(const [k,v] of Object.entries(d.preferences))if(v.mode==='SPECIFIC'&&!v.values?.length)d.preferences[k]={mode:'ANY',values:[]};return JSON.stringify(d);}
export function traitSummary(raw,mode='answers'){const d=parseTraits(raw);return Object.values(d[mode]).flatMap(v=>Array.isArray(v)?v:v.mode==='SPECIFIC'?v.values:[]).slice(0,12).join(' · ');}
