# METAHUMAN LEGACY — Visual Asset Manifest

## Recovery

- **FOUND_EXACT:** 01–10, 21–40, 51–60
- **FOUND_PROBABLE:** 41–50
- **MISSING:** 11–20
- **Recovered:** 50 / 60 sheets
- **Runtime representatives:** 50

Les planches 11–20 ne sont pas rebaptisées à partir de doublons : elles restent explicitement `MISSING` et utilisent la chaîne de fallback du resolver.

## Runtime atlas

- 50 cellules sémantiques
- 5 colonnes × 10 lignes
- 72 × 72 px par cellule
- 360 × 720 px décodé
- JPEG optimisé Android, encodé en 10 fragments Base64
- SHA-256 décodé : `e9e7dbcc4a11ff61a2f8841a0b960454f9ef6e768f8d45d2cb470c23cd5f25eb`

Le catalogue machine-readable complet se trouve dans `app/src/main/assets/visual_asset_manifest.json`.

## Mapping principal

| Runtime | Sheet | Usage |
|---|---:|---|
| brand_hero | 01 | Home / identité |
| brand_villain | 02 | Legacy sombre |
| morality_hero | 08 | Moralité |
| rank_bronze | 09 | Prestige faible |
| rank_gold | 10 | Prestige élevé |
| scope_street | 31 | Rue |
| scope_district | 32 | Quartier |
| scope_city | 33 | Ville |
| scope_region | 39 | Région |
| scope_country | 40 | Pays |
| scope_world | 60 | Monde / Legacy |
| danger_low | 51 | Constat civil |
| danger_high | 34 | Crise |
| danger_extreme | 56 | Climax |
| power_energy | 41 | Éveil |
| power_fire | 42 | Aura |
| power_strength | 43 | Attaque |
| power_forcefield | 44 | Défense |
| power_speed | 45 | Mouvement |
| power_water | 46 | Impact |
| alt_09 | 47 | Perte de contrôle |
| power_wind | 48 | Coût / faiblesse |
| alt_10 | 49 | Transformation |
| power_time | 50 | Pouvoir légendaire |
| relation_family | 21 | PNJ / relation |
| origin_unknown | 22 | Évolution joueur |
| morality_neutral | 23 | Expression |
| relation_rival | 24 | Vieillissement |
| alt_01 | 25 | Blessure |
| alt_02 | 26 | Costume improvisé |
| alt_03 | 27 | Costume affirmé |
| route_ascend | 28 | Traitement sombre |
| alt_04 | 29 | Identité / masque |
| alt_05 | 30 | Accessoires |
| alt_06 | 36 | Crime / clandestin |
| alt_07 | 37 | Faction |
| power_cosmic | 38 | Mystique / cosmique |
| relation_media | 52 | Médias |
| morality_villain | 53 | Opinion hostile |
| route_care | 54 | Relations |
| alt_08 | 55 | Chapter card |
| public_fear | 57 | Aftermath / peur |
| prestige_hero | 58 | Fin de carrière |
| rank_legend | 59 | Hall of Legacies |

Les autres clés du resolver sont documentées dans le JSON machine-readable.
