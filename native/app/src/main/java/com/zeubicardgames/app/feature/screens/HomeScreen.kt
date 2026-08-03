package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.core.designsystem.AssetImage
import com.zeubicardgames.app.feature.shell.*

@Composable fun HomeScreen(state: GameUiState, vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PremiumCard(Modifier.fillMaxWidth()) {
            Text("Booster du moment", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            val ext = state.extensions.firstOrNull { it.id == state.selectedExtension } ?: state.extensions.firstOrNull()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text(ext?.name ?: "PLAYER", style = MaterialTheme.typography.headlineSmall); Text(ext?.subtitle ?: "Extension officielle"); Spacer(Modifier.height(10.dp)); Button(onClick = { vm.tab(MainTab.OPEN) }) { Text("OUVRIR") } }
                AssetImage(ext?.boosterPath, ext?.name, Modifier.width(120.dp).height(150.dp), ContentScale.Fit)
            }
        }
        Text("Extensions officielles", style = MaterialTheme.typography.titleLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            state.extensions.forEach { ext -> PremiumCard(Modifier.width(170.dp).clickable { vm.chooseExtension(ext.id); vm.tab(MainTab.OPEN) }) { AssetImage(ext.boosterPath, ext.name, Modifier.fillMaxWidth().height(120.dp)); Text(ext.name, style = MaterialTheme.typography.titleMedium); Text("${ext.cardCount} cartes") } }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.tab(MainTab.COLLECTION) }, modifier = Modifier.weight(1f)) { Text("Collection") }
            Button(onClick = { vm.overlay(OverlayScreen.Decks) }, modifier = Modifier.weight(1f)) { Text("Decks") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { vm.tab(MainTab.BATTLE) }, modifier = Modifier.weight(1f)) { Text("Combat") }
            OutlinedButton(onClick = { vm.overlay(OverlayScreen.Missions) }, modifier = Modifier.weight(1f)) { Text("Missions") }
        }
    }
}
