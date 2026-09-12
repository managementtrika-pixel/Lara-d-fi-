package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GameplayRebuildCharacterScreen(
    c: Campaign,
    state: UltimateState,
    deep: DeepLifeState,
    onStateChange: (UltimateState) -> Unit,
    onBack: () -> Unit
) {
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Text("QUI TU ES DEVENU", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.2.sp)
        Text(c.alias.ifBlank { c.name }, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 29.sp, lineHeight = 31.sp)
        Text("${c.age} ans · ${humanIdentityLine(c, deep)}", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            UltimatePortrait(c, state, Modifier.size(width = 210.dp, height = 278.dp).clip(CutCornerShape(18.dp)), heroMode = c.powerRevealed)
        }
        Spacer(Modifier.height(10.dp))

        val traits = deep.personality.entries.sortedByDescending { kotlin.math.abs(it.value) }.take(4)
        UltimatePanel(accent = accent) {
            Text("CE QUE TES ANNÉES ONT FAIT DE TOI", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
            if (traits.isEmpty()) {
                Text(c.temperament, color = UltimateIvory, fontWeight = FontWeight.Black)
            } else {
                Text(traits.joinToString(" · ") { it.key.lowercase().replaceFirstChar(Char::uppercase) }, color = UltimateIvory, fontWeight = FontWeight.Black, lineHeight = 19.sp)
            }
            val public = deep.perception.civilians
            val government = deep.perception.government
            Text("Les civils te voient ${perceptionWord(public)}. Les institutions te voient ${perceptionWord(government)}.", color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = if (deep.injuries.isNotEmpty() || state.injuries.isNotEmpty()) UltimateRed else UltimateGreen) {
            Text("TON CORPS", color = if (deep.injuries.isNotEmpty() || state.injuries.isNotEmpty()) UltimateRed else UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text(state.ageAppearance(c), color = UltimateIvory, fontWeight = FontWeight.Black)
            Text("${state.bodyBuild} · ${state.stature} · ${state.hair} · yeux ${state.eyes.lowercase()}", color = UltimateMuted, fontSize = 11.sp)
            val injuries = deep.injuries.map { "${it.bodyPart} (${it.originAge} ans)" } + state.injuries
            if (injuries.isNotEmpty()) Text("Traces : ${injuries.distinct().take(4).joinToString(" · ")}", color = UltimateRed, fontSize = 10.sp, lineHeight = 15.sp)
        }

        if (c.powerRevealed) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = accent) {
                Text("TON POUVOIR N'EST PLUS LE MÊME QU'À 18 ANS", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text(c.powerFamily, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 18.sp)
                deep.powerEvolution?.let { power ->
                    Text("${power.architecture.name.lowercase().replaceFirstChar(Char::uppercase)} · ${power.branch} · maîtrise ${power.mastery}", color = UltimateMuted, fontSize = 11.sp)
                    if (power.unlockedTechniques.isNotEmpty()) Text(power.unlockedTechniques.takeLast(4).joinToString(" · "), color = UltimateGold, fontSize = 10.sp)
                    if (power.mutations.isNotEmpty()) Text("Évolution : ${power.mutations.takeLast(2).joinToString(" · ")}", color = UltimateViolet, fontSize = 10.sp)
                }
                Text("Coût : ${c.weakness}", color = UltimateRed, fontSize = 10.sp)
            }

            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateGold) {
                Text("TON IDENTITÉ PUBLIQUE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text("${state.heroPresentation} · ${state.maskStyle}", color = UltimateIvory, fontWeight = FontWeight.Black)
                Text("${state.costumePalette} · ère ${state.costumeEra.coerceAtLeast(1)} · ${state.emblem}", color = UltimateMuted, fontSize = 10.sp)
                Text("Ce costume influence la manière dont on te reconnaît, te craint et remonte jusqu'à ta vie civile.", color = UltimateMuted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("TON APPARENCE CONTINUE DE T'APPARTENIR", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f)) {
                UltimateActionTile("Coiffure", state.hair, UltimateBlue, onClick = {
                    onStateChange(state.copy(hair = nextValue(UltimateCatalog.hairs, state.hair)))
                })
            }
            Box(Modifier.weight(1f)) {
                UltimateActionTile("Style civil", state.civilianStyle, UltimateMuted, onClick = {
                    onStateChange(state.copy(civilianStyle = nextValue(UltimateCatalog.civilianStyles, state.civilianStyle)))
                })
            }
        }
        if (c.powerRevealed) {
            Spacer(Modifier.height(6.dp))
            LibraryCostumePresetStrip(state, state.costumeEra.coerceAtLeast(1), onStateChange)
        }
        Spacer(Modifier.height(10.dp))
        MhlSecondaryButton("Retour à ta vie", onBack, Modifier.fillMaxWidth())
    }
}

private fun humanIdentityLine(c: Campaign, deep: DeepLifeState): String {
    val strongest = deep.personality.maxByOrNull { kotlin.math.abs(it.value) }?.key?.lowercase()?.replaceFirstChar(Char::uppercase)
    return listOfNotNull(strongest, if (c.powerRevealed) c.scope.label else "vie civile").joinToString(" · ")
}

private fun perceptionWord(value: Int): String = when {
    value >= 45 -> "comme quelqu'un sur qui compter"
    value >= 15 -> "plutôt favorablement"
    value <= -45 -> "comme une menace"
    value <= -15 -> "avec méfiance"
    else -> "sans avis clair"
}

private fun nextValue(values: List<String>, current: String): String {
    if (values.isEmpty()) return current
    val index = values.indexOf(current)
    return values[(if (index < 0) 0 else index + 1) % values.size]
}
