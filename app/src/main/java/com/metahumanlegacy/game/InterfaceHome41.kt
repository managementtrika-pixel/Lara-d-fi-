package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
internal fun InterfaceHome41(
    campaign: Campaign?, state: UltimateState?, hallCount: Int,
    onContinue: () -> Unit, onNew: () -> Unit, onHall: () -> Unit, onSettings: () -> Unit
) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Commencer une nouvelle vie ?") },
        text = { Text("La destinée en cours sera abandonnée. Le Hall of Legacies restera intact.") },
        confirmButton = { TextButton(onClick = { confirm = false; onNew() }) { Text("NOUVELLE VIE") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("ANNULER") } }
    )
    val accent = if (campaign?.powerRevealed == true) powerVisualProfile(campaign.powerFamily).accent else UltimateGold
    Box(Modifier.fillMaxSize().background(Interface41.ink)) {
        HomeCity41(campaign, state, accent, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x22000000), Color.Transparent, Color(0xD9000205)))))
        Box(Modifier.align(Alignment.TopEnd).padding(14.dp).size(48.dp).background(Interface41.glassSoft, RoundedCornerShape(16.dp)).clickable(onClick = onSettings).semantics { contentDescription = "Réglages" }, contentAlignment = Alignment.Center) {
            Text("⚙", color = Interface41.text, fontSize = 18.sp)
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)) {
            Text("METAHUMAN", color = Interface41.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 5.sp)
            Text("LEGACY", color = Interface41.text, fontWeight = FontWeight.Black, fontSize = 51.sp, lineHeight = 49.sp)
            Text("UNE VIE QUI LAISSE DES TRACES", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(18.dp))
            if (campaign != null && state != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UltimatePortrait(campaign, state, Modifier.width(72.dp).height(92.dp), heroMode = campaign.powerRevealed)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("REPRENDRE", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.4.sp)
                        Text(campaign.alias.ifBlank { campaign.name }.uppercase(), color = Interface41.text, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("${campaign.age} ans · ${campaign.city} · ${state.mediaFrame}", color = Interface41.muted, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                HomeAction41("Continuer cette vie", accent, true, onContinue)
                Spacer(Modifier.height(7.dp))
                HomeAction41("Nouvelle vie", Color.White, false) { confirm = true }
            } else {
                Text("Commence à 8 ans. Tes décisions construisent silencieusement la personne — et le pouvoir — qui naîtra plus tard.", color = Interface41.text.copy(alpha = .90f), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(Modifier.height(14.dp))
                HomeAction41("Commencer une vie", accent, true, onNew)
            }
            Spacer(Modifier.height(7.dp))
            HomeAction41("Hall of Legacies  ·  $hallCount", Color.White, false, onHall)
        }
    }
}

@Composable
private fun HomeAction41(label: String, accent: Color, primary: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).background(if (primary) accent.copy(alpha = .16f) else Interface41.glassSoft, RoundedCornerShape(17.dp))
            .border(1.dp, if (primary) accent.copy(alpha = .55f) else Color.White.copy(alpha = .10f), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Interface41.text, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text("›", color = accent, fontSize = 25.sp)
    }
}

@Composable
private fun HomeCity41(c: Campaign?, state: UltimateState?, accent: Color, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF071526), Color(0xFF101622), Color(0xFF030509))))
        val horizon = size.height * .58f
        val seed = c?.seed ?: 41L
        repeat(17) { i ->
            val v = ((seed shr (i % 13)) + i * 47L).toInt().absoluteValue
            val bw = size.width / 15f
            val bh = size.height * (.10f + (v % 31) / 100f)
            val x = i * size.width / 16f - bw * .4f
            drawRect(Color(0xFF09121D), Offset(x, horizon - bh), Size(bw, bh + size.height - horizon))
            if (i % 3 != 0) drawRect(accent.copy(alpha = .22f), Offset(x + bw * .30f, horizon - bh * .65f), Size(bw * .12f, 3f))
        }
        drawCircle(accent.copy(alpha = .08f), size.minDimension * .32f, Offset(size.width * .70f, size.height * .30f))
        drawRect(Color.Black.copy(alpha = .24f), Offset(0f, horizon), Size(size.width, size.height - horizon))
    }
}
