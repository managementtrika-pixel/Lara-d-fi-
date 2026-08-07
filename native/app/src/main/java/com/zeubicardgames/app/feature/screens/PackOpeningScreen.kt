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

@Composable
fun PackOpeningScreen(state: GameUiState, vm: GameViewModel) {
    val ext = state.extensions.firstOrNull { it.id == state.selectedExtension }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ouvrir un booster", style = MaterialTheme.typography.headlineSmall)

        if (state.packResult.isEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.extensions.forEach { extension ->
                    FilterChip(
                        selected = extension.id == state.selectedExtension,
                        onClick = { vm.chooseExtension(extension.id) },
                        label = { Text(extension.name) },
                    )
                }
            }

            AssetImage(
                ext?.boosterPath,
                ext?.name,
                Modifier.fillMaxWidth().weight(1f),
                ContentScale.Fit,
            )
            Text(
                "Le contenu est attribué et sauvegardé avant l’affichage des cartes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = vm::openPack,
                enabled = !state.openingPack,
                modifier = Modifier.fillMaxWidth().height(58.dp),
            ) {
                if (state.openingPack) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("ATTRIBUTION…")
                } else {
                    Text("OUVRIR LE BOOSTER")
                }
            }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } else {
            if (state.packRecovered) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(
                        "Ouverture récupérée : ces cartes avaient déjà été ajoutées à ta collection avant la fermeture de l’application.",
                        Modifier.padding(12.dp),
                    )
                }
            } else {
                Text("Booster attribué · doublons ajoutés à la collection")
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.packResult.forEach { card ->
                    CardTile(
                        card = card,
                        quantity = state.owned[card.canonicalId]?.quantity ?: 1,
                        onClick = { vm.inspect(card) },
                        modifier = Modifier.width(120.dp),
                    )
                }
            }

            Button(onClick = vm::openAnotherPack, Modifier.fillMaxWidth()) {
                Text("OUVRIR UN AUTRE")
            }
            OutlinedButton(onClick = vm::finishPack, Modifier.fillMaxWidth()) {
                Text("TERMINER")
            }
        }
    }
}
