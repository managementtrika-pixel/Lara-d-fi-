# METAHUMAN LEGACY

Simulateur Android solo de destinée métahumaine, en français. Le joueur commence à 8 ans, traverse exactement dix années formatives de 8 à 17 ans, puis découvre à 18 ans un pouvoir façonné par ses décisions précédentes plutôt que sélectionné dans un menu. La carrière peut ensuite rester locale, devenir nationale ou mondiale, vieillir, transmettre et finir dans le Hall of Legacies.

## Version

**2.0.0 — Deep Life Simulation** (`versionCode 5`)

La 2.0 transforme la simulation de statistiques en simulation de vie : mémoire narrative, relations qui changent de nature, personnalité évolutive, perceptions multiples, blessures persistantes, identité secrète fondée sur des indices, opportunités qui expirent, conséquences différées, économie civile, districts vivants, némésis adaptatif, vieillissement, retraite, succession et héritage entre parties.

## Piliers

- Chronologie canonique : 8→17 ans formatifs, éveil à 18 ans, carrière puis Legacy.
- Pouvoir non choisi directement : les dix années formatives alimentent affinités, expression et coût avant la manifestation.
- Pixel DNA persistant : le même personnage visuel traverse créateur, gameplay, costume et vieillissement.
- 266 nœuds narratifs canoniques, 1 064 décisions et 1 064 constats associés, plus 200 fins cataloguées.
- CARE / ORDER / TRUTH / ASCEND restent des moteurs internes ; les décisions sont présentées comme des conséquences concrètes plutôt que comme une morale à quatre boutons.
- Portée Rue → Quartier → Ville → Région → Pays → Monde fondée sur la portée sociale active (influence, prestige, médias, institutions, opinion et peur), pas uniquement sur une jauge d'XP.
- 9 architectures mécaniques de pouvoirs : projection, mental, corps, mobilité, matière, technologie, occulte, cosmique et adaptatif.
- Faiblesses actives : surcharge, fatigue, concentration, visibilité, environnement, ressources, instabilité et récupération changent le contexte des choix.
- Mémoire longue : les décisions importantes créent des souvenirs pondérés qui peuvent revenir des années plus tard.
- Relations profondes : valeurs, peurs, ambitions, limites, secrets, confiance, affection, rancune et phases relationnelles.
- Perceptions segmentées : quartier, ville, nation, gouvernement, police, civils, métahumains et jeunesse ; la peur criminelle n'est plus confondue avec la peur civile.
- Ville vivante : dégâts, crime, reconstruction, sentiment local, factions, lois, médias et statut légal évoluent avec la carrière.
- Temps limité : trois actions annuelles et des opportunités à expiration rendent ce qui n'est pas fait aussi important que ce qui est choisi.
- Échec persistant : blessures, dettes et séquelles peuvent prolonger une histoire ; une survie critique unique évite qu'un mauvais moment efface automatiquement une vie entière.
- Vieillissement et transmission : maîtrise accrue, récupération plus difficile, protégé, successeur, préparation de retraite et départ volontaire.
- Hall of Legacies profond : personnalité, relation majeure, némésis, blessure durable, techniques et souvenirs fondateurs sont archivés ; les anciens héros peuvent laisser un écho dans de nouvelles vies.

## Sauvegarde

Le socle historique de campagne reste migrable. La couche Deep Life 2.0 utilise une persistance JSON structurée et versionnée (`schemaVersion`) stockée localement, afin de permettre les migrations futures sans casser les parties 1.x.

## Qualité

La CI Android exécute :

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
gradle connectedDebugAndroidTest
```

Elle vérifie aussi signature/alignement de l'APK et produit son SHA-256. Des simulations multi-seeds couvrent le moteur narratif ; la 2.0 ajoute des tests spécifiques sur mémoire, perceptions, architectures de pouvoirs, survie critique et persistance structurée.

## Stack

- Kotlin / Jetpack Compose
- Android API 36, minSdk 23
- JDK 17
- ProfileInstaller
- RNG déterministe par seed pour les systèmes simulés
- Sauvegarde locale hors ligne

APK debug : `app/build/outputs/apk/debug/app-debug.apk`

## Direction artistique

La direction principale est désormais le pixel-art moderne premium. Le renderer `PixelAvatar` sert d'identité visuelle persistante du créateur au gameplay. Les anciennes expérimentations comic ne constituent plus la direction produit.

## Copyright

Copyright © 2026. Tous droits réservés. Aucun droit n'est concédé sur l'univers, les textes ou les éléments originaux de METAHUMAN LEGACY au-delà de ce qui est nécessaire au fonctionnement des dépendances tierces.
