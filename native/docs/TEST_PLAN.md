# Test Plan

## Automatisés
- Validation deck : 20 cartes, limite de 2, personnage de base.
- BattleEngine : énergie, attaque, dégâts, K.O., victoire, seed déterministe.
- Smoke Compose : accueil visible.

## Matrices visuelles
360×640, 360×800, 390×844, 412×915 et 430×932. Vérifier safe areas, grille collection 3 colonnes, plateau sans superposition et cartes en `ContentScale.Fit`.

## Cas limites
Drag annulé, retour Android, Retraits pleins, main 10, absence de remplaçant, double attaque et reprise après relance.
