package com.metahumanlegacy.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun DeepLifeChronicleScreen(
    c: Campaign,
    u: UltimateState,
    d: DeepLifeState,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { DeepLifeChronicleScreen(c, u, d) }
        MhlSecondaryButton("Retour au destin", onBack, Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 6.dp))
    }
}
