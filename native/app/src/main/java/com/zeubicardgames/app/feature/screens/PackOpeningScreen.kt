package com.zeubicardgames.app.feature.screens

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

@Composable fun PackOpeningScreen(state: GameUiState, vm: GameViewModel) {
    val ext = state.extensions.firstOrNull { it.id == state.selectedExtension }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ouvrir un booster", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.extensions.forEach { e -> FilterChip(e.id == state.selectedExtension, { vm.chooseExtension(e.id) }, { Text(e.name) }) } }
        if (state.packResult.isEmpty()) {
            AssetImage(ext?.boosterPath, ext?.name, Modifier.fillMaxWidth().weight(1f), ContentScale.Fit)
            Text("Maintiens le booster, déchire puis révèle les cartes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = vm::openPack, Modifier.fillMaxWidth().height(58.dp)) { Text("MAINTENIR POUR OUVRIR") }
        } else {
            Text("Rare minimum garantie · doublons ajoutés à la collection")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) { state.packResult.take(5).forEach { card -> CardTile(card, state.owned[card.canonicalId]?.quantity ?: 1, { vm.inspect(card) }, Modifier.weight(1f)) } }
            Button(onClick = vm::openPack, Modifier.fillMaxWidth()) { Text("OUVRIR UN AUTRE") }
        }
    }
}
