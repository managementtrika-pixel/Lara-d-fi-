# METAHUMAN LEGACY

Simulateur Android solo de destinée de super-être, en français. Le joueur commence à 8 ans, traverse dix années formatives de 8 à 17 ans, puis connaît sa première manifestation potentielle à 18 ans. Sa carrière métahumaine peut ensuite rester locale ou atteindre une portée mondiale.

## Principes

- Moralité, prestige, opinion publique, peur, puissance et portée sont indépendants.
- RNG déterministe par seed.
- 12 familles d'origines et 26 familles de pouvoirs.
- Espace procédural de 660 événements identifiables avec quatre décisions contextuelles chacun.
- Progression Rue → Quartier → Ville → Région → Pays → Monde.
- Sauvegarde locale hors ligne et Hall of Legacies.
- Interface Jetpack Compose sombre, contemporaine et inspirée du langage des cases de comics sans reprendre de licence existante.

## Build

Stack vérifiée en août 2026 : AGP 9.3.0, Gradle 9.5.0, API 37, JDK 17, Compose BOM 2026.08.00.

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

APK : `app/build/outputs/apk/debug/app-debug.apk`

## CI

Chaque push sur `main` exécute les tests, Android Lint et produit l'APK debug comme artifact GitHub Actions.

## Copyright

Copyright © 2026. Tous droits réservés. Aucun droit n'est concédé sur l'univers, les textes ou les éléments originaux de METAHUMAN LEGACY au-delà de ce qui est nécessaire au fonctionnement des dépendances tierces.
