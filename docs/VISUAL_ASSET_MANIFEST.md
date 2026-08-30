# METAHUMAN LEGACY — VISUAL ASSET MANIFEST

## Source

La refonte utilise les 20 planches graphiques fournies pour METAHUMAN LEGACY. Les planches restent les sources haute définition ; l'application embarque une sélection optimisée de 50 éléments afin d'éviter de charger les planches complètes sur mobile.

## Runtime atlas

| Runtime asset | Contenu | Taille | Usage principal | Statut |
|---|---:|---:|---|---|
| comic_atlas_01.webp | 5 symboles | 800×160 | branding / routes | intégré |
| comic_atlas_02.webp | 5 symboles | 800×160 | moralité / prestige | intégré |
| comic_atlas_03.webp | 5 symboles | 800×160 | prestige / portée | intégré |
| comic_atlas_04.webp | 5 symboles | 800×160 | portée / danger | intégré |
| comic_atlas_05.webp | 5 symboles | 800×160 | danger / physique | intégré |
| comic_atlas_06.webp | 5 symboles | 800×160 | pouvoirs élémentaires | intégré |
| comic_atlas_07.webp | 5 symboles | 800×160 | pouvoirs / origines | intégré |
| comic_atlas_08.webp | 5 symboles | 800×160 | relations / médias | intégré |
| comic_atlas_09.webp | 5 variantes | 800×160 | monde / factions / narration | intégré |
| comic_atlas_10.webp | 5 variantes | 800×160 | Legacy / événements majeurs | intégré |

Chaque cellule fait 160×160 px, conserve sa transparence et est décodée à la demande. Aucun sprite 4096×4096 n'est chargé à l'écran.

## Keys runtime

`brand_hero`, `brand_villain`, `route_care`, `route_order`, `route_truth`, `route_ascend`, `morality_hero`, `morality_neutral`, `morality_villain`, `rank_bronze`, `rank_gold`, `rank_legend`, `scope_street`, `scope_district`, `scope_city`, `scope_region`, `scope_country`, `scope_world`, `danger_low`, `danger_high`, `danger_extreme`, `power_strength`, `power_speed`, `power_senses`, `power_fire`, `power_ice`, `power_water`, `power_wind`, `power_energy`, `power_cosmic`, `power_forcefield`, `power_time`, `origin_psychic`, `origin_mystic`, `origin_unknown`, `relation_family`, `relation_rival`, `relation_media`, `public_fear`, `prestige_hero`, `alt_01` … `alt_10`.

## Règles

- période 18 → éveil : visuels civils/urbains, sans symbole de pouvoir révélé ;
- éveil : le symbole du pouvoir n'apparaît qu'au moment prévu par le moteur ;
- choix : les couleurs ne révèlent jamais CARE / ORDER / TRUTH / ASCEND ;
- moralité, opinion, peur, prestige, puissance et portée restent des axes visuels distincts ;
- fallback interne homogène si une key manque ; jamais de carré vide ou de texte `missing`.
