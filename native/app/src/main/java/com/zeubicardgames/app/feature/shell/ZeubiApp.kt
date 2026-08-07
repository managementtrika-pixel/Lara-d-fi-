package com.zeubicardgames.app.feature.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zeubicardgames.app.feature.screens.*

@Composable
fun ZeubiApp(vm: GameViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val duel = state.overlay == OverlayScreen.Duel
    BackHandler(state.overlay != OverlayScreen.None || state.selectedCard != null) { vm.dismissOverlay() }
    Scaffold(
        topBar = { if (!duel) ZeubiHeader(state) },
        bottomBar = { if (!duel && state.overlay == OverlayScreen.None) ZeubiBottomBar(state.tab, vm::tab) },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.overlay) {
                OverlayScreen.None -> when (state.tab) {
                    MainTab.HOME -> HomeScreen(state, vm)
                    MainTab.COLLECTION -> CollectionScreen(state, vm)
                    MainTab.OPEN -> PackOpeningScreen(state, vm)
                    MainTab.BATTLE -> BattleHubScreen(state, vm)
                    MainTab.MENU -> MenuScreen(vm)
                }
                OverlayScreen.Decks -> DecksScreen(state, vm)
                OverlayScreen.Missions -> MissionsScreen(state)
                OverlayScreen.Settings -> SettingsScreen(state, vm)
                OverlayScreen.Campaign -> CampaignScreen(state, vm)
                OverlayScreen.Duel -> DuelScreen(state, vm)
            }
            state.selectedCard?.let { card ->
                CardDetailDialog(
                    card = card,
                    owned = state.owned[card.canonicalId],
                    favorite = card.canonicalId in state.preferences.favoriteCardIds,
                    dismiss = vm::inspect,
                    select = vm::selectVariant,
                    toggleFavorite = vm::toggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun ZeubiHeader(state: GameUiState) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("ZEUBICARDGAMES", style = MaterialTheme.typography.titleMedium)
                Text("PLAYER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
                Text(
                    "◈ ${state.owned.values.sumOf { it.quantity }} cartes",
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ZeubiBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar {
        listOf(
            MainTab.HOME to "Accueil",
            MainTab.COLLECTION to "Collection",
            MainTab.OPEN to "Ouvrir",
            MainTab.BATTLE to "Combat",
            MainTab.MENU to "Menu",
        ).forEach { (tab, label) ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Text(if (tab == MainTab.OPEN) "✦" else "•") },
                label = { Text(label) },
            )
        }
    }
}
