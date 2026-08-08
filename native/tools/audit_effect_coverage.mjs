#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const args = Object.fromEntries(
  process.argv.slice(2).reduce((entries, value, index, all) => {
    if (value.startsWith('--')) entries.push([value.slice(2), all[index + 1]]);
    return entries;
  }, []),
);

if (!args.project) throw new Error('Usage: audit_effect_coverage.mjs --project DIR');

const project = path.resolve(args.project);
const cardsPath = path.join(project, 'app/src/main/assets/catalog/cards.json');
const registryPath = path.join(project, 'app/src/main/java/com/zeubicardgames/app/core/effects/SemanticEffectSpecs.kt');
const outputPath = path.join(project, 'app/src/main/assets/catalog/effect_coverage.json');

const cards = JSON.parse(fs.readFileSync(cardsPath, 'utf8'));
const source = fs.readFileSync(registryPath, 'utf8');
const conflictStart = source.indexOf('private val conflicts');
if (conflictStart < 0) throw new Error('Section conflicts introuvable dans SemanticEffectSpecs.kt');

const beforeConflicts = source.slice(0, conflictStart);
const conflictSection = source.slice(conflictStart);
const keyRegex = /cardKey\s*=\s*"([^"]+)"/g;

function collectKeys(text) {
  const keys = new Set();
  for (const match of text.matchAll(keyRegex)) keys.add(match[1]);
  return keys;
}

const verified = collectKeys(beforeConflicts);
const conflicts = collectKeys(conflictSection);
const supports = cards.filter((card) => card.type === 'action' || card.type === 'replique');
const entries = supports
  .map((card) => {
    const key = `${card.setId}:${String(card.number).padStart(3, '0')}`;
    const status = verified.has(key)
      ? 'VERIFIED'
      : conflicts.has(key)
        ? 'RULE_CONFLICT'
        : 'UNVERIFIED';
    return {
      key,
      canonicalId: card.canonicalId,
      setId: card.setId,
      number: card.number,
      name: card.name,
      type: card.type,
      wireCode: card.effect || null,
      status,
    };
  })
  .sort((a, b) => a.setId.localeCompare(b.setId) || a.number.localeCompare(b.number));

const counts = entries.reduce((acc, entry) => {
  acc[entry.status] = (acc[entry.status] || 0) + 1;
  return acc;
}, { VERIFIED: 0, RULE_CONFLICT: 0, UNVERIFIED: 0 });

const report = {
  schemaVersion: 1,
  supportCardCount: entries.length,
  counts,
  executableCoveragePercent: entries.length === 0 ? 100 : Math.round((counts.VERIFIED / entries.length) * 1000) / 10,
  entries,
};

fs.writeFileSync(outputPath, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
