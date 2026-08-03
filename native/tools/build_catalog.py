#!/usr/bin/env python3
from __future__ import annotations
import argparse, base64, hashlib, io, json, re, shutil, unicodedata
from pathlib import Path
import json5
from PIL import Image

SETS = {
    "ninja": ("Roobkaruto", "L’Ombre des Ninjas", 0xFFE07826),
    "emerald": ("Green Bafo", "Volonté Émeraude", 0xFF23A75C),
    "cod": ("Call of Trikuss", "Opérations tactiques", 0xFFB99038),
    "dbz": ("Trika Ball Z", "Puissance sans limite", 0xFFE56B25),
}

def balanced(text: str, start: int) -> str:
    opening=text[start]; closing={"[":"]","{":"}"}[opening]; depth=0; quote=None; esc=False
    for i in range(start,len(text)):
        c=text[i]
        if quote:
            if esc: esc=False
            elif c=="\\": esc=True
            elif c==quote: quote=None
            continue
        if c in "'\"`": quote=c; continue
        if c==opening: depth+=1
        elif c==closing:
            depth-=1
            if depth==0: return text[start:i+1]
    raise ValueError("Unbalanced expression")

def const_expr(text: str, name: str):
    m=re.search(rf"const\s+{re.escape(name)}\s*=\s*", text)
    if not m: return None
    i=m.end()
    while text[i].isspace(): i+=1
    return json5.loads(balanced(text,i))

def slug(s):
    s=unicodedata.normalize("NFKD",s).encode("ascii","ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+","_",s).strip("_")

def decode_data(uri):
    m=re.match(r"data:([^;]+);base64,(.*)",uri,re.S)
    if not m: return None
    mime, raw=m.groups(); ext={"image/webp":"webp","image/png":"png","image/jpeg":"jpg","audio/mpeg":"mp3","audio/wav":"wav"}.get(mime,"bin")
    return ext, base64.b64decode(raw)

def save_image(uri, full: Path, thumb: Path):
    dec=decode_data(uri)
    if not dec: return False
    ext,data=dec; full=full.with_suffix('.'+ext); thumb=thumb.with_suffix('.webp')
    full.parent.mkdir(parents=True,exist_ok=True); thumb.parent.mkdir(parents=True,exist_ok=True)
    full.write_bytes(data)
    try:
        im=Image.open(io.BytesIO(data)).convert('RGB'); im.thumbnail((360,504),Image.Resampling.LANCZOS); im.save(thumb,'WEBP',quality=82,method=6)
    except Exception: thumb.write_bytes(data)
    return full.name,thumb.name

def attacks(c):
    out=[]
    for key in ('a1','a2'):
        a=c.get(key)
        if isinstance(a,list) and a: out.append({"name":str(a[0]),"damage":int(a[1] or 0) if len(a)>1 else 0,"cost":int(a[2] or 0) if len(a)>2 else 0})
    return out

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--html',required=True); ap.add_argument('--project',required=True); args=ap.parse_args()
    text=Path(args.html).read_text('utf-8',errors='ignore'); project=Path(args.project)
    assets=project/'app/src/main/assets'; docs=project/'docs'; docs.mkdir(parents=True,exist_ok=True)
    for d in ['catalog','cards/full','cards/thumb','boosters','backgrounds','ui','audio']:
        (assets/d).mkdir(parents=True,exist_ok=True)
    base=const_expr(text,'CARD_DB') or []; new=const_expr(text,'NEW_CARDS') or []; visuals=const_expr(text,'VISUALS') or {}; variants=const_expr(text,'NEW_VARIANTS') or {}; packs=const_expr(text,'OFFICIAL_PACKS') or {}; ui=const_expr(text,'UI') or {}
    merged={}
    for c in base+new:
        key=f"{c.get('set','unknown')}:{c.get('num',c.get('id','000'))}:{slug(c.get('name','card'))}"
        merged[key]=c
    cards=[]; asset_rows=[]
    for canonical,c in sorted(merged.items(),key=lambda kv:(kv[1].get('set',''),str(kv[1].get('num','')))):
        name=c.get('name','Carte'); images=variants.get(name) or ([visuals[name]] if visuals.get(name) else [])
        var_rows=[]
        for idx,uri in enumerate(images):
            if not isinstance(uri,str) or not uri.startswith('data:'): continue
            base_name=f"{slug(canonical)}__v{idx+1}"
            result=save_image(uri,assets/'cards/full'/base_name,assets/'cards/thumb'/base_name)
            if result:
                full_name,thumb_name=result; var_rows.append({"variantId":f"{canonical}:v{idx+1}","fullPath":f"cards/full/{full_name}","thumbPath":f"cards/thumb/{thumb_name}"})
        cards.append({"canonicalId":canonical,"setId":c.get('set','unknown'),"number":str(c.get('num','')),"name":name,"kind":c.get('kind','pokemon'),"stage":c.get('stage','base'),"evolvesFrom":c.get('from'),"hp":int(c.get('hp',0) or 0),"retreat":int(c.get('retreat',0) or 0),"rarity":c.get('rarity','C'),"attacks":attacks(c),"effect":c.get('effect'),"variants":var_rows})
        asset_rows.append((canonical,name,len(var_rows)))
    extensions=[]
    for sid,(name,subtitle,accent) in SETS.items():
        uri=packs.get(sid); booster=""
        if isinstance(uri,str) and uri.startswith('data:'):
            dec=decode_data(uri)
            if dec:
                ext,data=dec; p=assets/'boosters'/f'{sid}.{ext}'; p.write_bytes(data); booster=f'boosters/{p.name}'
        extensions.append({"id":sid,"name":name,"subtitle":subtitle,"accent":accent,"boosterPath":booster,"cardCount":sum(1 for c in cards if c['setId']==sid)})
    for key,value in ui.items():
        if isinstance(value,str) and value.startswith('data:image'):
            dec=decode_data(value)
            if dec:
                ext,data=dec; (assets/'ui'/f'{slug(key)}.{ext}').write_bytes(data)
    (assets/'catalog/cards.json').write_text(json.dumps(cards,ensure_ascii=False,indent=2),'utf-8')
    (assets/'catalog/extensions.json').write_text(json.dumps(extensions,ensure_ascii=False,indent=2),'utf-8')
    inventory=['# Asset Inventory','',f'- Cartes canoniques : **{len(cards)}**',f'- Variantes illustrées extraites : **{sum(x[2] for x in asset_rows)}**',f'- Boosters officiels : **{sum(1 for e in extensions if e["boosterPath"])}**','', '## Extensions']
    inventory += [f'- {e["name"]}: {e["cardCount"]} cartes — `{e["boosterPath"] or "booster manquant"}`' for e in extensions]
    inventory += ['', '## Règle de canonicalisation', '`extension:numéro:nom_normalisé` ; chaque illustration possède son propre `variantId`.']
    (docs/'ASSET_INVENTORY.md').write_text('\n'.join(inventory),'utf-8')
    (docs/'CARD_CATALOG.md').write_text('# Card Catalog\n\n'+ '\n'.join(f'- `{c["canonicalId"]}` — {c["name"]} — {c["rarity"]} — {len(c["variants"])} variante(s)' for c in cards),'utf-8')
    print(json.dumps({"cards":len(cards),"variants":sum(len(c['variants']) for c in cards),"extensions":extensions},ensure_ascii=False,indent=2))
if __name__=='__main__': main()
