# Architecture

Application Android native single-activity en Kotlin/Jetpack Compose. Les frontières sont conservées en packages : `core/model`, `core/designsystem`, `core/data`, `core/database`, `core/gameengine`, `feature/shell` et `feature/screens`.

Flux principal : `UserAction → GameViewModel / BattleEngine → GameUiState immuable → Compose UI`.

Room conserve collection, decks, campagne et missions. DataStore conserve les préférences. Les assets et le catalogue sont générés depuis la dernière source V8.5, sans Base64 dans le Kotlin.
