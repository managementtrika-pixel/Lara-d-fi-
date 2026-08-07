#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const CATALOG_SCHEMA_VERSION = 1;
const VALID_RARITIES = new Set(['C', 'U', 'R', 'SR', 'UR']);
const MAX_ASSET_BYTES = 8 * 1024 * 1024;

const args = Object.fromEntries(process.argv.slice(2).reduce((a, v, i, arr) => {
  if (v.startsWith('--')) a.push([v.slice(2), arr[i + 1]]);
  return a;
}, []));
if (!args.html || !args.project) throw new Error('Usage: --html FILE --project DIR');

const text = fs.readFileSync(args.html, 'utf8');
const project = path.resolve(args.project);
const assets = path.join(project, 'app/src/main/assets');
const catalogDir = path.join(assets, 'catalog');
const docs = path.join(project, 'docs');
for (const d of ['catalog', 'cards/full', 'cards/thumb', 'boosters', 'backgrounds', 'ui', 'audio']) {
  fs.mkdirSync(path.join(assets, d), { recursive: true });
}
fs.mkdirSync(docs, { recursive: true });

const errors = [];
const warnings = [];
const error = (code, message, context = {}) => errors.push({ severity: 'error', code, message, context });
const warn = (code, message, context = {}) => warnings.push({ severity: 'warning', code, message, context });

function balanced(start) {
  const opening = text[start], closing = opening === '[' ? ']' : '}';
  let depth = 0, quote = null, esc = false;
  for (let i = start; i < text.length; i++) {
    const c = text[i];
    if (quote) {
      if (esc) esc = false;
      else if (c === '\\') esc = true;
      else if (c === quote) quote = null;
      continue;
    }
    if (c === '"' || c === "'" || c === '`') { quote = c; continue; }
    if (c === opening) depth++;
    else if (c === closing && --depth === 0) return text.slice(start, i + 1);
  }
  throw new Error('Unbalanced expression');
}

function readConst(name) {
  const re = new RegExp(`const\\s+${name}\\s*=\\s*`, 'g');
  const m = re.exec(text);
  if (!m) return null;
  let i = m.index + m[0].length;
  while (/\s/.test(text[i])) i++;
  return vm.runInNewContext(`(${balanced(i)})`, Object.create(null), { timeout: 10000 });
}

function readJsonIfExists(file, fallback) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); }
  catch { return fallback; }
}

function slug(s) {
  return String(s).normalize('NFKD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '');
}

function decode(uri) {
  const m = /^data:([^;]+);base64,(.*)$/s.exec(uri || '');
  if (!m) return null;
  const ext = {
    'image/webp': 'webp', 'image/png': 'png', 'image/jpeg': 'jpg',
    'audio/mpeg': 'mp3', 'audio/wav': 'wav',
  }[m[1]] || 'bin';
  return { ext, data: Buffer.from(m[2], 'base64') };
}

function saveData(uri, relBase) {
  const d = decode(uri);
  if (!d) return '';
  const rel = `${relBase}.${d.ext}`;
  const p = path.join(assets, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, d.data);
  return rel;
}

function attacks(c) {
  return ['a1', 'a2'].flatMap(k => Array.isArray(c[k]) ? [{
    name: String(c[k][0] || ''),
    damage: Number(c[k][1] || 0),
    cost: Number(c[k][2] || 0),
  }] : []);
}

function identityKey(setId, number) {
  return `${String(setId || '').trim()}:${String(number || '').trim()}`;
}

function newPermanentId(setId, number) {
  const set = slug(setId || 'unknown').toUpperCase() || 'UNKNOWN';
  const num = slug(number || '000').toUpperCase() || '000';
  return `CARD_${set}_${num}`;
}

const previouslyPublishedCards = readJsonIfExists(path.join(catalogDir, 'cards.json'), []);
const frozenIds = new Map();
for (const card of previouslyPublishedCards) {
  if (!card?.canonicalId || !card?.setId || !card?.number) continue;
  frozenIds.set(identityKey(card.setId, card.number), card.canonicalId);
}

const base = readConst('CARD_DB') || [];
const added = readConst('NEW_CARDS') || [];
const visuals = readConst('VISUALS') || {};
const variants = readConst('NEW_VARIANTS') || {};
const packs = readConst('OFFICIAL_PACKS') || {};
const ui = readConst('UI') || {};
const sourceCards = [...base, ...added];

const sourceByIdentity = new Map();
for (const c of sourceCards) {
  const setId = String(c.set || '').trim();
  const number = String(c.num || c.id || '').trim();
  const name = String(c.name || '').trim();
  const key = identityKey(setId, number);

  if (!setId) error('MISSING_SET_ID', 'Carte sans extension.', { name, number });
  if (!number) error('MISSING_CARD_NUMBER', 'Carte sans numéro.', { setId, name });
  if (!name) error('MISSING_CARD_NAME', 'Carte sans nom.', { setId, number });
  if (sourceByIdentity.has(key)) {
    error('DUPLICATE_CARD_NUMBER', 'Deux cartes utilisent le même numéro dans la même extension.', {
      key,
      first: sourceByIdentity.get(key)?.name,
      second: name,
    });
  } else {
    sourceByIdentity.set(key, c);
  }
}

const cards = [];
const canonicalIds = new Set();
for (const c of sourceCards.sort((a, b) => `${a.set}${a.num}`.localeCompare(`${b.set}${b.num}`))) {
  const setId = String(c.set || 'unknown');
  const number = String(c.num || c.id || '');
  const name = String(c.name || 'Carte');
  const key = identityKey(setId, number);
  const canonicalId = frozenIds.get(key) || newPermanentId(setId, number);

  if (canonicalIds.has(canonicalId)) {
    error('DUPLICATE_CARD_ID', 'CARD_ID dupliqué.', { canonicalId, setId, number, name });
  }
  canonicalIds.add(canonicalId);

  const rarity = String(c.rarity || 'C').toUpperCase();
  if (!VALID_RARITIES.has(rarity)) {
    error('UNKNOWN_RARITY', 'Rareté inconnue.', { canonicalId, rarity });
  }

  const hp = Number(c.hp || 0);
  const retreat = Number(c.retreat || 0);
  if (!Number.isFinite(hp) || hp < 0) error('INVALID_HP', 'PV invalides.', { canonicalId, hp: c.hp });
  if (!Number.isFinite(retreat) || retreat < 0) error('INVALID_RETREAT', 'Coût de retraite invalide.', { canonicalId, retreat: c.retreat });

  const cardAttacks = attacks(c);
  cardAttacks.forEach((attack, index) => {
    if (!attack.name.trim()) error('INVALID_ATTACK', 'Attaque sans nom.', { canonicalId, index });
    if (!Number.isFinite(attack.damage) || attack.damage < 0) error('INVALID_ATTACK_DAMAGE', 'Dégâts invalides.', { canonicalId, attack });
    if (!Number.isFinite(attack.cost) || attack.cost < 0) error('INVALID_ATTACK_COST', 'Coût d’attaque invalide.', { canonicalId, attack });
  });

  const imgs = variants[c.name] || (visuals[c.name] ? [visuals[c.name]] : []);
  const vars = [];
  imgs.forEach((uri, index) => {
    if (typeof uri !== 'string' || !uri.startsWith('data:')) return;
    const baseName = `${slug(canonicalId)}__v${index + 1}`;
    const full = saveData(uri, `cards/full/${baseName}`);
    const thumb = saveData(uri, `cards/thumb/${baseName}`);
    if (full) vars.push({ variantId: `${canonicalId}:v${index + 1}`, fullPath: full, thumbPath: thumb });
  });
  if (vars.length === 0) warn('MISSING_ILLUSTRATION', 'Aucune illustration intégrée pour cette carte.', { canonicalId, name });

  cards.push({
    canonicalId,
    setId,
    number,
    name,
    kind: c.kind || 'pokemon',
    stage: c.stage || 'base',
    evolvesFrom: c.from || null,
    hp,
    retreat,
    rarity,
    attacks: cardAttacks,
    effect: c.effect || null,
    variants: vars,
  });
}

const setMeta = {
  ninja: ['Roobkaruto', 'L’Ombre des Ninjas', 0xFFE07826],
  emerald: ['Green Bafo', 'Volonté Émeraude', 0xFF23A75C],
  cod: ['Call of Trikuss', 'Opérations tactiques', 0xFFB99038],
  dbz: ['Trika Ball Z', 'Puissance sans limite', 0xFFE56B25],
};

for (const card of cards) {
  if (!setMeta[card.setId]) {
    error('CARD_WITHOUT_EXTENSION', 'Carte non intégrée à une extension connue.', {
      canonicalId: card.canonicalId,
      setId: card.setId,
    });
  }
}

const extensions = Object.entries(setMeta).map(([id, [name, subtitle, accent]]) => {
  const boosterPath = saveData(packs[id], `boosters/${id}`);
  if (!boosterPath) warn('MISSING_BOOSTER_ASSET', 'Booster sans illustration.', { setId: id });
  return { id, name, subtitle, accent, boosterPath, cardCount: cards.filter(c => c.setId === id).length };
});

const namesBySet = new Map();
for (const card of cards) {
  const setNames = namesBySet.get(card.setId) || new Map();
  setNames.set(card.name, card);
  namesBySet.set(card.setId, setNames);
}
for (const card of cards) {
  if (card.evolvesFrom && !namesBySet.get(card.setId)?.has(card.evolvesFrom)) {
    warn('MISSING_EVOLUTION_SOURCE', 'Évolution référencée mais carte source introuvable dans la même extension.', {
      canonicalId: card.canonicalId,
      evolvesFrom: card.evolvesFrom,
    });
  }
}

for (const [setId, setNames] of namesBySet) {
  for (const card of setNames.values()) {
    const visited = new Set();
    let current = card;
    while (current?.evolvesFrom) {
      if (visited.has(current.name)) {
        error('CYCLIC_EVOLUTION', 'Cycle détecté dans une lignée d’évolution.', {
          setId,
          card: card.name,
          cycleAt: current.name,
        });
        break;
      }
      visited.add(current.name);
      current = setNames.get(current.evolvesFrom);
    }
  }
}

function walkFiles(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const p = path.join(dir, entry.name);
    return entry.isDirectory() ? walkFiles(p) : [p];
  });
}
for (const file of [
  ...walkFiles(path.join(assets, 'cards/full')),
  ...walkFiles(path.join(assets, 'cards/thumb')),
  ...walkFiles(path.join(assets, 'boosters')),
]) {
  const size = fs.statSync(file).size;
  if (size > MAX_ASSET_BYTES) {
    warn('ASSET_TOO_HEAVY', 'Asset supérieur à 8 Mio.', {
      path: path.relative(assets, file).replaceAll(path.sep, '/'),
      bytes: size,
    });
  }
}

for (const [k, v] of Object.entries(ui)) {
  if (typeof v === 'string' && v.startsWith('data:image')) saveData(v, `ui/${slug(k)}`);
}

const idRegistry = Object.fromEntries(cards.map(card => [identityKey(card.setId, card.number), card.canonicalId]));
const validation = {
  schemaVersion: CATALOG_SCHEMA_VERSION,
  valid: errors.length === 0,
  summary: { cards: cards.length, extensions: extensions.length, errors: errors.length, warnings: warnings.length },
  errors,
  warnings,
};
const manifest = {
  schemaVersion: CATALOG_SCHEMA_VERSION,
  cardCount: cards.length,
  variantCount: cards.reduce((n, c) => n + c.variants.length, 0),
  extensionIds: extensions.map(e => e.id),
};

fs.writeFileSync(path.join(catalogDir, 'cards.json'), JSON.stringify(cards, null, 2));
fs.writeFileSync(path.join(catalogDir, 'extensions.json'), JSON.stringify(extensions, null, 2));
fs.writeFileSync(path.join(catalogDir, 'manifest.json'), JSON.stringify(manifest, null, 2));
fs.writeFileSync(path.join(catalogDir, 'id_registry.json'), JSON.stringify(idRegistry, null, 2));
fs.writeFileSync(path.join(catalogDir, 'validation_report.json'), JSON.stringify(validation, null, 2));

const inventory = [
  '# Asset Inventory', '',
  `- Schéma catalogue : **v${CATALOG_SCHEMA_VERSION}**`,
  `- Cartes canoniques : **${cards.length}**`,
  `- Variantes illustrées extraites : **${cards.reduce((n, c) => n + c.variants.length, 0)}**`,
  `- Boosters officiels : **${extensions.filter(e => e.boosterPath).length}**`,
  `- Erreurs de validation : **${errors.length}**`,
  `- Avertissements : **${warnings.length}**`,
  '', '## Extensions',
  ...extensions.map(e => `- ${e.name}: ${e.cardCount} cartes — \`${e.boosterPath || 'booster manquant'}\``),
  '', '## Identité technique',
  'Les IDs déjà publiés sont figés par `(extension, numéro)` afin qu’un renommage visible ne casse pas les sauvegardes. Les nouvelles cartes reçoivent un ID permanent de type `CARD_SET_NUMERO`.',
];
fs.writeFileSync(path.join(docs, 'ASSET_INVENTORY.md'), inventory.join('\n'));
fs.writeFileSync(path.join(docs, 'CARD_CATALOG.md'), '# Card Catalog\n\n' + cards.map(c => `- \`${c.canonicalId}\` — ${c.name} — ${c.rarity} — ${c.variants.length} variante(s)`).join('\n'));
fs.writeFileSync(path.join(docs, 'CONTENT_VALIDATION.md'), [
  '# Validation du contenu', '',
  `Statut : **${validation.valid ? 'VALIDE' : 'INVALIDE'}**`,
  `Erreurs : **${errors.length}** · Avertissements : **${warnings.length}**`, '',
  '## Erreurs',
  ...(errors.length ? errors.map(x => `- **${x.code}** — ${x.message} — \`${JSON.stringify(x.context)}\``) : ['- Aucune']),
  '', '## Avertissements',
  ...(warnings.length ? warnings.map(x => `- **${x.code}** — ${x.message} — \`${JSON.stringify(x.context)}\``) : ['- Aucun']),
].join('\n'));

console.log(JSON.stringify({
  schemaVersion: CATALOG_SCHEMA_VERSION,
  cards: cards.length,
  variants: cards.reduce((n, c) => n + c.variants.length, 0),
  extensions,
  validation: validation.summary,
}, null, 2));

if (errors.length) {
  console.error(`Catalogue invalide: ${errors.length} erreur(s), ${warnings.length} avertissement(s).`);
  process.exit(1);
}
