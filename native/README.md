# ZeubiCardGames Android Native

Reconstruction native Kotlin + Jetpack Compose. Aucune WebView principale.

## Générer le catalogue

```bash
node tools/build_catalog.mjs --html ../ZeubiCardGames_V8_5_CombatModes_BoosterDisplay.html --project .
```

## Compiler

```bash
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Dans GitHub, ouvrir **Actions → Android natif ZeubiCardGames**, puis télécharger l’artifact `ZeubiCardGames-debug-apk`.
