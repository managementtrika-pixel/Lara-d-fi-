package com.metahumanlegacy.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background

@Composable
internal fun GameplayRebuildEarlyShell(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    deep: DeepLifeState,
    outcome: String?,
    savePulse: Int,
    onScreen: (String) -> Unit,
    onContinue: () -> Unit,
    onChoice: (EventNode, Choice) -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit
) {
    val accent = if (c.turn == 10) UltimateViolet else if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Box(Modifier.fillMaxSize()) {
        if (c.powerRevealed && c.turn > 10 && outcome == null) {
            GameplayStoryTechniqueDestinyScreen(c, state, annual, deep, onChoice)
        } else {
            GameplayRebuildDestinyScreen(c, state, annual, deep, outcome, onContinue, onChoice)
        }

        Box(
            Modifier.fillMaxWidth().height(92.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xD9000307), Color.Transparent)))
        )
        Interface41TopHud(c, state, annual, savePulse, onHome, onSettings)

        Box(Modifier.align(Alignment.BottomCenter)) {
            Interface41Dock("DESTIN", annual.synced(c).remaining, accent, onScreen)
        }
    }
}
