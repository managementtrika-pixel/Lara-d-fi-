package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.feature.shell.*

@Composable fun MenuScreen(vm: GameViewModel) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Menu", style = MaterialTheme.typography.headlineMedium); listOf("Decks" to OverlayScreen.Decks, "Missions" to OverlayScreen.Missions, "Paramètres" to OverlayScreen.Settings).forEach { (label, screen) -> PremiumCard(Modifier.fillMaxWidth().clickable { vm.overlay(screen) }) { Text(label, style = MaterialTheme.typography.titleLarge); Text("Ouvrir") } } } }

@Composable fun DecksScreen(state: GameUiState, vm: GameViewModel) {
    var selected by remember { mutableStateOf(emptyList<String>()) }
    val owned = state.cards.filter { (state.owned[it.canonicalId]?.quantity ?: 0) > 0 }
    Column(Modifier.fillMaxSize().padding(12.dp)) { Text("Deck ${selected.size}/20", style = MaterialTheme.typography.headlineSmall); Text("Maximum 2 exemplaires par carte canonique"); LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(owned, key = { it.canonicalId }) { c -> CardTile(c, selected.count { it == c.canonicalId }, { val count = selected.count { it == c.canonicalId }; if (selected.size < 20 && count < 2) selected = selected + c.canonicalId else if (count > 0) selected = selected.toMutableList().also { it.remove(c.canonicalId) } }) } }; Button(onClick = {}, enabled = selected.size == 20, modifier = Modifier.fillMaxWidth()) { Text(if (selected.size == 20) "SAUVEGARDER" else "${selected.size}/20") } }
}

@Composable fun MissionsScreen(state: GameUiState) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Missions", style = MaterialTheme.typography.headlineSmall); listOf("Ouvrir le premier booster", "Construire un deck de 20 cartes", "Gagner un combat hors ligne", "Découvrir une variante").forEach { PremiumCard(Modifier.fillMaxWidth()) { Text(it); Text("En cours", color = MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator(progress = { 0.25f }, Modifier.fillMaxWidth()) } } } }

@Composable fun SettingsScreen(state: GameUiState, vm: GameViewModel) { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Paramètres", style = MaterialTheme.typography.headlineSmall); Setting("Sons", state.preferences.sound, vm::toggleSound); Setting("Haptique", state.preferences.haptics, vm::toggleHaptics); Setting("Animations réduites", state.preferences.reducedMotion, vm::toggleReduced) } }
@Composable private fun Setting(label: String, checked: Boolean, toggle: (Boolean) -> Unit) { PremiumCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Switch(checked, toggle) } } }
