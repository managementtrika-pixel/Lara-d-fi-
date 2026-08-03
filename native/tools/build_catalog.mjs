#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const args = Object.fromEntries(process.argv.slice(2).reduce((a,v,i,arr)=>{if(v.startsWith('--')) a.push([v.slice(2),arr[i+1]]); return a},[]));
if(!args.html || !args.project) throw new Error('Usage: --html FILE --project DIR');
const text = fs.readFileSync(args.html,'utf8');
const project = path.resolve(args.project);
const assets = path.join(project,'app/src/main/assets');
const docs = path.join(project,'docs');
for(const d of ['catalog','cards/full','cards/thumb','boosters','backgrounds','ui','audio']) fs.mkdirSync(path.join(assets,d),{recursive:true});
fs.mkdirSync(docs,{recursive:true});

function balanced(start){
  const opening=text[start], closing=opening==='['?']':'}'; let depth=0, quote=null, esc=false;
  for(let i=start;i<text.length;i++){
    const c=text[i];
    if(quote){ if(esc) esc=false; else if(c==='\\') esc=true; else if(c===quote) quote=null; continue; }
    if(c==='"'||c==="'"||c==='`'){ quote=c; continue; }
    if(c===opening) depth++; else if(c===closing && --depth===0) return text.slice(start,i+1);
  }
  throw new Error('Unbalanced expression');
}
function readConst(name){
  const re=new RegExp(`const\\s+${name}\\s*=\\s*`,'g'); const m=re.exec(text); if(!m) return null;
  let i=m.index+m[0].length; while(/\s/.test(text[i])) i++;
  const expr=balanced(i); return vm.runInNewContext(`(${expr})`,Object.create(null),{timeout:10000});
}
function slug(s){ return String(s).normalize('NFKD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'_').replace(/^_+|_+$/g,''); }
function decode(uri){ const m=/^data:([^;]+);base64,(.*)$/s.exec(uri||''); if(!m) return null; const ext={'image/webp':'webp','image/png':'png','image/jpeg':'jpg','audio/mpeg':'mp3','audio/wav':'wav'}[m[1]]||'bin'; return {ext,data:Buffer.from(m[2],'base64')}; }
function saveData(uri,relBase){ const d=decode(uri); if(!d) return ''; const rel=`${relBase}.${d.ext}`; const p=path.join(assets,rel); fs.mkdirSync(path.dirname(p),{recursive:true}); fs.writeFileSync(p,d.data); return rel; }
function attacks(c){ return ['a1','a2'].flatMap(k=>Array.isArray(c[k])?[{name:String(c[k][0]||''),damage:Number(c[k][1]||0),cost:Number(c[k][2]||0)}]:[]); }

const base=readConst('CARD_DB')||[];
const added=readConst('NEW_CARDS')||[];
const visuals=readConst('VISUALS')||{};
const variants=readConst('NEW_VARIANTS')||{};
const packs=readConst('OFFICIAL_PACKS')||{};
const ui=readConst('UI')||{};
const merged=new Map();
for(const c of [...base,...added]){ const id=`${c.set||'unknown'}:${c.num||c.id||'000'}:${slug(c.name||'card')}`; merged.set(id,c); }
const cards=[];
for(const [canonical,c] of [...merged.entries()].sort((a,b)=>`${a[1].set}${a[1].num}`.localeCompare(`${b[1].set}${b[1].num}`))){
  const imgs=variants[c.name] || (visuals[c.name]?[visuals[c.name]]:[]); const vars=[];
  imgs.forEach((uri,index)=>{ if(typeof uri!=='string'||!uri.startsWith('data:')) return; const baseName=`${slug(canonical)}__v${index+1}`; const full=saveData(uri,`cards/full/${baseName}`); const thumb=saveData(uri,`cards/thumb/${baseName}`); if(full) vars.push({variantId:`${canonical}:v${index+1}`,fullPath:full,thumbPath:thumb}); });
  cards.push({canonicalId:canonical,setId:c.set||'unknown',number:String(c.num||''),name:c.name||'Carte',kind:c.kind||'pokemon',stage:c.stage||'base',evolvesFrom:c.from||null,hp:Number(c.hp||0),retreat:Number(c.retreat||0),rarity:c.rarity||'C',attacks:attacks(c),effect:c.effect||null,variants:vars});
}
const setMeta={
  ninja:['Roobkaruto','L’Ombre des Ninjas',0xFFE07826], emerald:['Green Bafo','Volonté Émeraude',0xFF23A75C],
  cod:['Call of Trikuss','Opérations tactiques',0xFFB99038], dbz:['Trika Ball Z','Puissance sans limite',0xFFE56B25]
};
const extensions=Object.entries(setMeta).map(([id,[name,subtitle,accent]])=>({id,name,subtitle,accent,boosterPath:saveData(packs[id],`boosters/${id}`),cardCount:cards.filter(c=>c.setId===id).length}));
for(const [k,v] of Object.entries(ui)) if(typeof v==='string'&&v.startsWith('data:image')) saveData(v,`ui/${slug(k)}`);
fs.writeFileSync(path.join(assets,'catalog/cards.json'),JSON.stringify(cards,null,2));
fs.writeFileSync(path.join(assets,'catalog/extensions.json'),JSON.stringify(extensions,null,2));
const inventory=['# Asset Inventory','',`- Cartes canoniques : **${cards.length}**`,`- Variantes illustrées extraites : **${cards.reduce((n,c)=>n+c.variants.length,0)}**`,`- Boosters officiels : **${extensions.filter(e=>e.boosterPath).length}**`,'','## Extensions',...extensions.map(e=>`- ${e.name}: ${e.cardCount} cartes — \`${e.boosterPath||'booster manquant'}\``),'','## Règle de canonicalisation','`extension:numéro:nom_normalisé` ; chaque illustration possède son propre `variantId`.'];
fs.writeFileSync(path.join(docs,'ASSET_INVENTORY.md'),inventory.join('\n'));
fs.writeFileSync(path.join(docs,'CARD_CATALOG.md'),'# Card Catalog\n\n'+cards.map(c=>`- \`${c.canonicalId}\` — ${c.name} — ${c.rarity} — ${c.variants.length} variante(s)`).join('\n'));
console.log(JSON.stringify({cards:cards.length,variants:cards.reduce((n,c)=>n+c.variants.length,0),extensions},null,2));
