package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Career-only scene renderer that exposes techniques actually learned in LifeSimulationState. */
@Composable
internal fun GameplayStoryTechniqueDestinyScreen(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    deep: DeepLifeState,
    onChoice: (EventNode, Choice) -> Unit
) {
    val life = deep.lifeSimulation
    val lifeKey = listOf(
        life?.powerRules?.techniques?.hashCode() ?: 0,
        life?.districts?.hashCode() ?: 0,
        life?.secretIdentity?.exposure ?: 0
    ).hashCode()
    val event = remember(c.seed, c.turn, state.hashCode(), annual.hashCode(), lifeKey) {
        val base = UltimateGameEngine.event(c, state, annual)
        val worldAware = LifeWorldNarrativeDirector.enrich(c, deep, base)
        StoryTechniqueDirector.enrich(c, deep, worldAware)
    }
    val accent = powerVisualProfile(c.powerFamily).accent
    val technique = event.choices.firstOrNull { StoryTechniqueDirector.techniqueId(it) != null }
    val home = life?.districts?.firstOrNull { it.id == "quartier" }

    MhlSceneFrame(
        "story-technique-${c.seed}-${c.turn}-${event.id}",
        MotionBoard.PANEL_TRANSITION,
        if (event.stakes >= 4) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD,
        Modifier.fillMaxSize(),
        accent
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            Text("${c.age} ANS · ${c.phaseLabel}", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(event.title, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 30.sp)
            Spacer(Modifier.height(9.dp))
            Text(event.text, color = UltimateIvory, fontSize = 14.sp, lineHeight = 21.sp)

            if (home != null) {
                Spacer(Modifier.height(9.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color(0xB90C1620), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
                        .border(1.dp, UltimateBlue.copy(alpha = .45f), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
                        .padding(10.dp)
                ) {
                    Text("MÉMOIRE DU QUARTIER", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    Text(
                        "Sécurité ${home.safety} · crime ${home.criminalControl} · confiance ${home.localTrust} · médias ${home.mediaHeat}",
                        color = UltimateIvory,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text("Ces valeurs viennent de tes actions libres précédentes et peuvent modifier cette scène.", color = UltimateMuted, fontSize = 10.sp)
                }
            }

            if (technique != null) {
                val id = StoryTechniqueDirector.techniqueId(technique)
                val learned = life?.powerRules?.techniques?.firstOrNull { it.id == id }
                Spacer(Modifier.height(9.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color(0xB9101520), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
                        .border(1.dp, accent.copy(alpha = .55f), CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
                        .padding(10.dp)
                ) {
                    Text("TECHNIQUE APPRISE", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    Text(learned?.name ?: technique.label.removePrefix("Utiliser « ").removeSuffix(" »"), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    if (learned != null) {
                        Text(
                            "Proficiency ${learned.proficiency}/100 · maîtrise requise ${learned.masteryRequired} · ${if (learned.cooldownTurns == 0) "prête" else "récupération"}",
                            color = UltimateMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text("Ce choix existe parce que ton personnage a réellement appris cette technique.", color = UltimateMuted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("QU'EST-CE QUE TU FAIS ?", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
            event.choices.forEachIndexed { index, choice ->
                val isTechnique = StoryTechniqueDirector.techniqueId(choice) != null
                val choiceAccent = if (isTechnique) accent else when {
                    choice.risk >= 7 -> UltimateRed
                    choice.risk >= 4 -> UltimateGold
                    else -> UltimateBlue
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(Color(0xE8121821), CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp))
                        .border(1.dp, choiceAccent.copy(alpha = .45f), CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp))
                        .padding(10.dp)
                ) {
                    Text("${index + 1} · ${if (isTechnique) "TECHNIQUE" else "CHOIX"}", color = choiceAccent, fontWeight = FontWeight.Black, fontSize = 8.sp)
                    Text(choice.label, color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
                    Text(storyChoiceConsequence(choice), color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
                    Spacer(Modifier.height(7.dp))
                    MhlPrimaryButton(if (isTechnique) "Employer cette technique" else "Faire ce choix", { onChoice(event, choice) }, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}

private fun storyChoiceConsequence(choice: Choice): String = buildString {
    val signals = mutableListOf<String>()
    if (choice.moral > 0) signals += "protège davantage"
    if (choice.moral < 0) signals += "sacrifie la prudence morale"
    if (choice.power > 0) signals += "engage ton pouvoir"
    if (choice.identityDelta > 0) signals += "laisse des traces sur ton identité"
    if (choice.identityDelta < 0) signals += "protège ton secret"
    if (choice.healthDelta < 0) signals += "coûte physiquement"
    if (choice.relationDelta > 0) signals += "renforce un lien"
    if (choice.relationDelta < 0) signals += "fragilise un lien"
    signals += when {
        choice.risk >= 7 -> "risque extrême"
        choice.risk >= 4 -> "risque élevé"
        choice.risk >= 2 -> "risque réel"
        else -> "risque contenu"
    }
    append(signals.distinct().take(3).joinToString(" · ").replaceFirstChar { it.uppercase() })
}
