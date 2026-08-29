# METAHUMAN LEGACY — Visual Integration Report

## Résultat de récupération

- Planches retrouvées : **50 / 60**
- FOUND_EXACT : **01–10, 21–40, 51–60**
- FOUND_PROBABLE : **41–50**
- MISSING : **11–20**

Les planches 11–20 ne sont pas remplacées silencieusement par des doublons. Le resolver conserve un fallback neutre/catégoriel.

## Runtime

- 50 représentants visuels enregistrés dans le resolver.
- Chaque planche réellement récupérée alimente une cellule du nouvel atlas.
- Atlas : 360×720, cellules 72×72, 10 fragments Base64, chargement unique puis cache mémoire.
- SHA-256 décodé : `e9e7dbcc4a11ff61a2f8841a0b960454f9ef6e768f8d45d2cb470c23cd5f25eb`.

## Écrans raccordés

La branche comic existante raccorde le resolver à :

- Home ;
- création civile / nouvelle vie ;
- période personne ordinaire ;
- Destin / événements ;
- choix ;
- constats ;
- éveil ;
- Personnage ;
- Monde ;
- Liens ;
- Chronique ;
- Hall of Legacies / fin ;
- réglages.

## Garanties de gameplay

Cette intégration ne modifie pas le moteur narratif, le RNG, les arcs, les constats, les conséquences, la génération du pouvoir ou le format des sauvegardes. La période sans pouvoir et les dix choix formateurs restent inchangés.

## Validation

La validation finale doit passer par GitHub Actions : tests unitaires, lint et `assembleDebug`. L’APK n’est considérée finale qu’après récupération et contrôle de l’artefact issu de `main`.
