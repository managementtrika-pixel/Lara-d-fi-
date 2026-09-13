package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xF205090E)).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp)) {
                UltimatePortrait(c, state, Modifier.fillMaxSize(), heroMode = c.powerRevealed)
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("${c.age} ANS · ${c.phaseLabel}", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            if (savePulse > 0) Text("SAUVÉ", color = UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
            TextButton(onClick = onHome, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) { Text("ACCUEIL", fontSize = 10.sp) }
            TextButton(
                onClick = onSettings,
                modifier = Modifier.semantics { contentDescription = "Réglages" },
                contentPadding = PaddingValues(10.dp)
            ) { Text("⚙", fontSize = 15.sp) }
        }
        Box(Modifier.weight(1f)) {
            if (c.powerRevealed && c.turn > 10 && outcome == null) {
                GameplayStoryTechniqueDestinyScreen(c, state, annual, deep, onChoice)
            } else {
                GameplayRebuildDestinyScreen(c, state, annual, deep, outcome, onContinue, onChoice)
            }
        }
        val items = listOf(
            Triple("DESTIN", "alt_01", "Vie"),
            Triple("ACTIONS", "alt_04", "Temps ${annual.synced(c).remaining}"),
            Triple("PERSONNAGE", "alt_02", "Moi"),
            Triple("VILLE", "scope_city", "Ville"),
            Triple("LIENS", "relation_family", "Liens"),
            Triple("CHRONIQUE", "alt_03", "Mémoire")
        )
        NavigationBar(containerColor = Color(0xFF060A0F), tonalElevation = 0.dp) {
            items.forEach { (id, icon, label) ->
                NavigationBarItem(
                    selected = id == "DESTIN",
                    onClick = { onScreen(id) },
                    modifier = Modifier.semantics { contentDescription = label },
                    icon = { MhlProductionAsset(icon, label, size = 24.dp) },
                    label = { Text(label, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = accent.copy(alpha = .16f),
                        selectedTextColor = UltimateGold,
                        unselectedTextColor = UltimateMuted
                    )
                )
            }
        }
    }
}
