package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun InterfaceCareer41(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    screen: String,
    outcome: String?,
    savePulse: Int,
    onScreen: (String) -> Unit,
    onContinue: () -> Unit,
    onChoice: (EventNode, Choice) -> Unit,
    onAction: (AnnualActionCard) -> AnnualActionResult?,
    onStateChange: (UltimateState) -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onRestart: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Recommencer cette destinée ?") },
        text = { Text("Cette vie sera effacée. Le Hall of Legacies restera conservé.") },
        confirmButton = { TextButton(onClick = { confirm = false; onRestart() }) { Text("RECOMMENCER") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } }
    )
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(bottom = 68.dp)) {
            when (screen) {
                "ACTIONS" -> UltimateActionsScreen(c, state, annual, onAction)
                "PERSONNAGE" -> UltimateCharacterScreen(c, state, onStateChange)
                "VILLE" -> UltimateCityScreen(c, state)
                "LIENS" -> UltimateLinksScreen(c, state)
                "CHRONIQUE" -> UltimateChronicleScreen(c, state)
                else -> UltimateDestinyScreen(c, state, annual, outcome, onContinue, onChoice)
            }
        }
        Box(Modifier.fillMaxWidth().height(94.dp).align(Alignment.TopCenter).background(Brush.verticalGradient(listOf(Color(0xE6000206), Color.Transparent))))
        Interface41TopHud(c, state, annual, savePulse, onHome, onSettings)
        Interface41Dock(screen, annual.synced(c).remaining, accent, onScreen)
    }
}
