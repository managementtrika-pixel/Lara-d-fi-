#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const args = Object.fromEntries(
  process.argv.slice(2).reduce((entries, value, index, all) => {
    if (value.startsWith('--')) entries.push([value.slice(2), all[index + 1]]);
    return entries;
  }, []),
);

if (!args.project) throw new Error('Usage: normalize_catalog.mjs --project DIR');

const SCHEMA_VERSION = 1;
const project = path.resolve(args.project);
const catalogDir = path.join(project, 'app/src/main/assets/catalog');
const cardsPath = path.join(catalogDir, 'cards.json');
const extensionsPath = path.join(catalogDir, 'extensions.json');
const manifestPath = path.join(catalogDir, 'manifest.json');
const validationPath = path.join(catalogDir, 'validation_report.json');

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function writeJson(file, value) {
  fs.writeFileSync(file, JSON.stringify(value, null, 2));
}

function normalizeType(raw) {
  switch (String(raw || '').trim().toLowerCase()) {
    case 'pokemon':
    case 'personnage':
    case 'character':
    case 'fighter':
      return 'personnage';
    case 'action':
    case 'trainer':
    case 'support':
      return 'action';
    case 'replique':
    case 'réplique':
    case 'reply':
    case 'trap':
      return 'replique';
    case 'ressource':
    case 'resource':
    case 'energy':
    case 'energie':
    case 'énergie':
      return 'ressource';
    default:
      return 'inconnu';
  }
}

function normalizeStage(raw, type) {
  if (type !== 'personnage') return 'aucune';
  switch (String(raw || '').trim().toLowerCase()) {
    case 'base':
    case 'basic':
      return 'base';
    case 'evo1':
    case 'stage1':
    case 'evolution':
    case 'évolution':
      return 'evolution';
    case 'evo2':
    case 'stage2':
    case 'over':
    case 'over_evolution':
    case 'surevolution':
    case 'surévolution':
      return 'surevolution';
    default:
      return 'aucune';
  }
}

const cards = readJson(cardsPath);
const extensions = readJson(extensionsPath);
const manifest = fs.existsSync(manifestPath) ? readJson(manifestPath) : {};
const validation = fs.existsSync(validationPath) ? readJson(validationPath) : {};

const canonicalIds = new Set();
for (const card of cards) {
  if (!card.canonicalId) throw new Error(`Carte sans canonicalId: ${card.name || 'inconnue'}`);
  if (canonicalIds.has(card.canonicalId)) throw new Error(`canonicalId dupliqué: ${card.canonicalId}`);
  canonicalIds.add(card.canonicalId);
}

const bySetAndName = new Map();
for (const card of cards) {
  const setNames = bySetAndName.get(card.setId) || new Map();
  setNames.set(card.name, card.canonicalId);
  bySetAndName.set(card.setId, setNames);
}

const normalizedCards = cards.map((card) => {
  const type = normalizeType(card.type || card.kind);
  const evolutionStage = normalizeStage(card.evolutionStage || card.stage, type);
  const evolvesFromId = card.evolvesFromId ||
    (card.evolvesFrom ? bySetAndName.get(card.setId)?.get(card.evolvesFrom) || null : null);

  if (card.evolvesFrom && !evolvesFromId) {
    throw new Error(`Évolution introuvable pour ${card.canonicalId}: ${card.evolvesFrom}`);
  }
  if (evolvesFromId && !canonicalIds.has(evolvesFromId)) {
    throw new Error(`evolvesFromId invalide pour ${card.canonicalId}: ${evolvesFromId}`);
  }

  return {
    schemaVersion: SCHEMA_VERSION,
    canonicalId: card.canonicalId,
    setId: card.setId,
    number: card.number,
    name: card.name,
    type,
    evolutionStage,
    evolvesFrom: card.evolvesFrom || null,
    evolvesFromId,
    hp: Number(card.hp || 0),
    retreat: Number(card.retreat || 0),
    rarity: card.rarity,
    attacks: Array.isArray(card.attacks) ? card.attacks : [],
    effect: card.effect || null,
    variants: Array.isArray(card.variants) ? card.variants : [],
    // Champs hérités conservés pendant la migration des anciens outils.
    kind: card.kind || type,
    stage: card.stage || evolutionStage,
  };
});

const extensionIds = new Set();
const normalizedExtensions = extensions.map((extension, index) => {
  if (!extension.id) throw new Error(`Extension sans identifiant à l’index ${index}`);
  if (extensionIds.has(extension.id)) throw new Error(`Extension dupliquée: ${extension.id}`);
  extensionIds.add(extension.id);

  const code = String(extension.code || extension.id).trim().toUpperCase();
  return {
    schemaVersion: SCHEMA_VERSION,
    id: extension.id,
    code,
    order: Number.isInteger(extension.order) ? extension.order : index,
    name: extension.name,
    subtitle: extension.subtitle || '',
    accent: extension.accent,
    boosterPath: extension.boosterPath || '',
    cardCount: normalizedCards.filter((card) => card.setId === extension.id).length,
    status: String(extension.status || 'ACTIVE').toUpperCase(),
  };
});

for (const card of normalizedCards) {
  if (!extensionIds.has(card.setId)) {
    throw new Error(`Carte ${card.canonicalId} rattachée à une extension inexistante: ${card.setId}`);
  }
}

writeJson(cardsPath, normalizedCards);
writeJson(extensionsPath, normalizedExtensions);
writeJson(manifestPath, {
  ...manifest,
  schemaVersion: SCHEMA_VERSION,
  normalized: true,
  cardCount: normalizedCards.length,
  variantCount: normalizedCards.reduce((count, card) => count + card.variants.length, 0),
  extensionIds: normalizedExtensions.map((extension) => extension.id),
});
writeJson(validationPath, {
  ...validation,
  schemaVersion: SCHEMA_VERSION,
  catalogNormalized: true,
});

console.log(JSON.stringify({
  schemaVersion: SCHEMA_VERSION,
  cards: normalizedCards.length,
  extensions: normalizedExtensions.length,
  evolutionsResolved: normalizedCards.filter((card) => card.evolvesFromId).length,
}, null, 2));
