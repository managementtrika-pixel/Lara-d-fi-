# ZeubiCardGames — reconstruction Android native

Le nouveau projet est une application Android native en Kotlin + Jetpack Compose. L’ancien prototype HTML/WebView reste uniquement une source d’assets et de données ; il n’est plus l’interface principale.

## Pipeline

Le workflow **Android natif ZeubiCardGames** :

1. extrait `ZeubiCardGames_Native_Source.zip` dans `native/` ;
2. audite le HTML V8.5 ;
3. extrait cartes, variantes, boosters et UI sans Base64 dans Kotlin ;
4. génère le catalogue dynamique ;
5. lance le lint et les tests ;
6. compile l’APK debug ;
7. publie l’artifact `ZeubiCardGames-debug-apk` et `downloads/ZeubiCardGames-Native-debug.apk`.

## Audit V8.5 actuel

- 99 cartes canoniques détectées depuis les données réelles ;
- 113 variantes d’illustration ;
- 4 boosters officiels ;
- Roobkaruto : 20 cartes détectées ;
- Green Bafo : 19 cartes détectées ;
- Call of Trikuss : 30 cartes détectées ;
- Trika Ball Z : 30 cartes détectées.

Le nombre n’est jamais forcé : il est calculé depuis le catalogue et les assets présents.
