package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CinematicDestiny43(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    deep: DeepLifeState,
    outcome: String?,
    onContinue: () -> Unit,
    onChoice: (EventNode, Choice) -> Unit
) {
    val event = remember(c.seed, c.turn, state.hashCode(), annual.rescue, annual.investigation, annual.presence, annual.discipline) {
        if (outcome == null) UltimateGameEngine.event(c, state, annual) else null
    }
    val awakening = c.turn == 10 && !c.powerRevealed && outcome == null
    val accent = when {
        awakening -> UltimateViolet
        c.powerRevealed -> powerVisualProfile(c.powerFamily).accent
        else -> UltimateBlue
    }

    MhlSceneFrame(
        "cinematic-43-${c.seed}-${c.turn}-${outcome?.hashCode()}",
        if (awakening) MotionBoard.AWAKENING else if (outcome == null) MotionBoard.PANEL_TRANSITION else MotionBoard.OUTCOME_REVEAL,
        if (awakening) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD,
        Modifier.fillMaxSize(),
        accent
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF03070C))) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 72.dp)) {
                CinematicLifeScene43(c, state, accent)
                Box(
                    Modifier.fillMaxWidth().offset(y = (-18).dp)
                        .background(Brush.verticalGradient(listOf(Color(0x0003070C), Color(0xFF03070C))))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    when {
                        outcome != null -> CinematicConsequence43(c, state, outcome, accent, onContinue)
                        awakening && event != null -> CinematicAwakening43(c, deep, event, onChoice)
                        event != null -> CinematicHumanMoment43(c, deep, event, accent, onChoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun CinematicHumanMoment43(
    c: Campaign,
    deep: DeepLifeState,
    event: EventNode,
    accent: Color,
    onChoice: (EventNode, Choice) -> Unit
) {
    val person = SceneContextDirector.participant(event, deep)
    Interface41NarrativeGlass(
        eyebrow = if (c.age < 18) "Un moment qui te façonne" else "La ville attend ta réponse",
        title = event.title,
        body = event.text,
        accent = accent,
        modifier = Modifier.fillMaxWidth()
    )
    if (person != null) {
        Spacer(Modifier.height(9.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp)) {
            Column(Modifier.weight(1f)) {
                Text(person.name.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Text(person.role, color = UltimateMuted, fontSize = 10.sp)
            }
            if (person.knowsIdentity) Text("CONNAÎT TON SECRET", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 8.sp)
        }
    }
    Spacer(Modifier.height(14.dp))
    Text("QU'EST-CE QUE TU FAIS ?", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.4.sp)
    Spacer(Modifier.height(7.dp))
    event.choices.forEachIndexed { index, choice ->
        Interface41Choice(
            number = index + 1,
            label = choice.label,
            consequence = cinematicChoiceHint43(choice),
            risk = choice.risk,
            accent = accent,
            onClick = { onChoice(event, choice) }
        )
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun CinematicAwakening43(
    c: Campaign,
    deep: DeepLifeState,
    event: EventNode,
    onChoice: (EventNode, Choice) -> Unit
) {
    Interface41NarrativeGlass(
        eyebrow = "18 ans · L'éveil",
        title = "Tout ce que tu as été revient d'un coup.",
        body = event.text,
        accent = UltimateViolet,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    val memories = DeepLifeDirector.awakeningMemoryLines(deep, 4)
    if (memories.isNotEmpty()) {
        UltimatePanel(accent = UltimateViolet) {
            Text("MÉMOIRES QUI REMONTENT", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.1.sp)
            memories.forEach { memory ->
                Spacer(Modifier.height(5.dp))
                Text("• $memory", color = UltimateIvory, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(9.dp))
    }
    Text("TU NE CHOISIS PAS TON POUVOIR. TU CHOISIS CE QUE TU FAIS MAINTENANT.", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp, lineHeight = 13.sp)
    Spacer(Modifier.height(8.dp))
    event.choices.forEachIndexed { index, choice ->
        Interface41Choice(
            number = index + 1,
            label = choice.label,
            consequence = cinematicChoiceHint43(choice),
            risk = choice.risk,
            accent = UltimateViolet,
            onClick = { onChoice(event, choice) }
        )
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun CinematicConsequence43(
    c: Campaign,
    state: UltimateState,
    outcome: String,
    accent: Color,
    onContinue: () -> Unit
) {
    val blocks = outcome.split("\n\n").filter { it.isNotBlank() }
    Interface41NarrativeGlass(
        eyebrow = "Après",
        title = cinematicOutcomeTitle43(c, outcome),
        body = blocks.firstOrNull().orEmpty(),
        accent = accent,
        modifier = Modifier.fillMaxWidth()
    )
    blocks.drop(1).take(2).forEach { block ->
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = if (block.contains("MONDE", true)) UltimateBlue else if (block.contains("TRACE", true)) UltimateGold else accent) {
            Text(block, color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
    if (c.powerRevealed) {
        Spacer(Modifier.height(9.dp))
        Text(
            "VILLE ${state.cityCondition}%  ·  SANTÉ ${c.health}%  ·  EXPOSITION ${c.identityExposure}%",
            color = UltimateMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
    Spacer(Modifier.height(12.dp))
    MhlPrimaryButton(
        if (c.needsAlias) "Nommer ce qui vient de naître" else "Continuer",
        onContinue,
        Modifier.fillMaxWidth()
    )
}

private fun cinematicChoiceHint43(choice: Choice): String {
    val hints = buildList {
        if (choice.relationDelta >= 2) add("rapproche une relation")
        if (choice.relationDelta <= -2) add("peut casser un lien")
        if (choice.healthDelta <= -5 || choice.risk >= 6) add("risque physique")
        if (choice.identityDelta > 0) add("laisse des traces")
        if (choice.identityDelta < 0) add("protège le secret")
        if (choice.deferredHook) add("peut revenir plus tard")
        if (choice.impact >= 4) add("impact durable")
    }
    return hints.take(2).joinToString(" · ").ifBlank { "conséquences incertaines" }
}

private fun cinematicOutcomeTitle43(c: Campaign, outcome: String): String = when {
    c.turn == 11 && c.powerRevealed -> "Le monde ne te verra plus comme avant."
    outcome.contains("bless", true) || c.health < 40 -> "Tu continues, mais pas intact."
    outcome.contains("relation", true) -> "Quelqu'un s'en souviendra."
    c.age < 18 -> "Tu ignores encore ce que ce moment a construit."
    else -> "La ville absorbe ton choix. Toi aussi."
}
