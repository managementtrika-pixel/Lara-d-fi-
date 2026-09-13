package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object Interface41 {
    val glass = Color(0xE6080D14)
    val glassSoft = Color(0xB30A111A)
    val ink = Color(0xFF020409)
    val text = Color(0xFFF5F2EA)
    val muted = Color(0xFF9BA8B7)
    const val minTouchDp = 48

    fun compactNavIds() = listOf("DESTIN", "ACTIONS", "PERSONNAGE", "VILLE", "LIENS", "CHRONIQUE")
    fun narrativeSceneRatio(): Float = .58f
}

@Composable
internal fun Interface41TopHud(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    savePulse: Int,
    onHome: () -> Unit,
    onSettings: () -> Unit
) {
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(c.alias.ifBlank { c.name }.uppercase(), color = Interface41.text, fontWeight = FontWeight.Black, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${c.age} ANS  ·  ${c.phaseLabel.uppercase()}", color = accent, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = .8.sp)
        }
        if (savePulse > 0) Text("●", color = UltimateGreen, fontSize = 9.sp, modifier = Modifier.padding(end = 6.dp).semantics { contentDescription = "Sauvegardé" })
        Interface41IconButton("⌂", "Accueil", onHome)
        Spacer(Modifier.width(6.dp))
        Interface41IconButton("⚙", "Réglages", onSettings)
    }
}

@Composable
private fun Interface41IconButton(glyph: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Interface41.glassSoft)
            .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { Text(glyph, color = Interface41.text, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun Interface41Dock(
    current: String,
    remaining: Int,
    accent: Color,
    onScreen: (String) -> Unit
) {
    val items = listOf(
        Triple("DESTIN", "◆", "Vie"),
        Triple("ACTIONS", "＋", "Agir $remaining"),
        Triple("PERSONNAGE", "◉", "Moi"),
        Triple("VILLE", "▥", "Ville"),
        Triple("LIENS", "∞", "Liens"),
        Triple("CHRONIQUE", "≡", "Mémoire")
    )
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp)).background(Interface41.glass)
            .border(1.dp, Color.White.copy(alpha = .10f), RoundedCornerShape(22.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (id, glyph, label) ->
            val selected = current == id || (id == "DESTIN" && current !in items.map { it.first })
            Column(
                Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(17.dp))
                    .background(if (selected) accent.copy(alpha = .16f) else Color.Transparent)
                    .clickable { onScreen(id) }.semantics { contentDescription = label }.padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(glyph, color = if (selected) accent else Interface41.muted, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(label, color = if (selected) Interface41.text else Interface41.muted, fontSize = 8.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun Interface41NarrativeGlass(
    eyebrow: String,
    title: String,
    body: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.clip(CutCornerShape(topEnd = 26.dp)).background(
            Brush.verticalGradient(listOf(Color(0xD9060A10), Color(0xF0060A10)))
        ).border(1.dp, accent.copy(alpha = .28f), CutCornerShape(topEnd = 26.dp)).padding(16.dp)
    ) {
        Text(eyebrow.uppercase(), color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(5.dp))
        Text(title, color = Interface41.text, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 27.sp)
        Spacer(Modifier.height(9.dp))
        Text(body, color = Interface41.text.copy(alpha = .92f), fontSize = 14.sp, lineHeight = 21.sp)
    }
}

@Composable
internal fun Interface41Choice(
    number: Int,
    label: String,
    consequence: String,
    risk: Int,
    accent: Color,
    onClick: () -> Unit
) {
    val edge = when {
        risk >= 7 -> UltimateRed
        risk >= 4 -> UltimateGold
        else -> accent
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xF20A1018), edge.copy(alpha = .12f))))
            .border(1.dp, edge.copy(alpha = .40f), CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(edge.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Text(number.toString(), color = edge, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Interface41.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
            if (consequence.isNotBlank()) Text(consequence, color = Interface41.muted, fontSize = 10.sp, lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = edge, fontSize = 24.sp, fontWeight = FontWeight.Light)
    }
}
