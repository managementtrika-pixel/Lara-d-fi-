package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object CinematicAdult44 {
    fun powerIntensity(c: Campaign, state: UltimateState, event: EventNode): Float {
        val base = .22f + (c.power.coerceIn(0, 100) / 100f) * .35f
        val strain = (state.powerStrain.coerceIn(0, 100) / 100f) * .18f
        val stakes = (event.stakes.coerceIn(0, 8) / 8f) * .25f
        return (base + strain + stakes).coerceIn(.18f, 1f)
    }

    fun sceneLabel(c: Campaign, event: EventNode): String = when {
        event.stakes >= 7 -> "ALERTE MAJEURE"
        c.scope >= Scope.WORLD -> "PORTÉE MONDIALE"
        c.scope >= Scope.CITY -> "VILLE EN MOUVEMENT"
        else -> "TON TERRITOIRE"
    }

    fun choiceKind(choice: Choice): String = when {
        StoryTechniqueDirector.techniqueId(choice) != null -> "TECHNIQUE"
        IdentityNarrativeDirector.isIdentityChoice(choice) -> "IDENTITÉ"
        choice.risk >= 7 -> "DANGER"
        else -> "CHOIX"
    }
}

@Composable
internal fun CinematicAdultDestiny44(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    deep: DeepLifeState,
    onChoice: (EventNode, Choice) -> Unit
) {
    val life = deep.lifeSimulation
    val identityState = life?.let { IdentityPressureDirector.sync(c, deep, it) }
    val lifeKey = listOf(
        life?.powerRules?.techniques?.hashCode() ?: 0,
        life?.districts?.hashCode() ?: 0,
        identityState?.secretIdentity?.hashCode() ?: 0,
        deep.identityEvidence.hashCode(),
        deep.relationships.filter { it.knowsIdentity }.hashCode()
    ).hashCode()
    val event = remember(c.seed, c.turn, state.hashCode(), annual.hashCode(), lifeKey) {
        val base = UltimateGameEngine.event(c, state, annual)
        val worldAware = LifeWorldNarrativeDirector.enrich(c, deep, base)
        val identityAware = IdentityNarrativeDirector.enrich(c, deep, worldAware)
        StoryTechniqueDirector.enrich(c, deep, identityAware)
    }
    val accent = powerVisualProfile(c.powerFamily).accent
    val techniqueChoice = event.choices.firstOrNull { StoryTechniqueDirector.techniqueId(it) != null }
    val techniqueId = techniqueChoice?.let(StoryTechniqueDirector::techniqueId)
    val learnedTechnique = life?.powerRules?.techniques?.firstOrNull { it.id == techniqueId }
    val identity = identityState?.secretIdentity
    val home = life?.districts?.firstOrNull { it.id == "quartier" }
    val intensity = CinematicAdult44.powerIntensity(c, state, event)

    MhlSceneFrame(
        "adult-cinematic-44-${c.seed}-${c.turn}-${event.id}",
        MotionBoard.PANEL_TRANSITION,
        if (event.stakes >= 5) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD,
        Modifier.fillMaxSize(),
        accent
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF03070C))) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 76.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    CinematicLifeScene43(c, state, accent)
                    CinematicPowerVfx(c.powerFamily, intensity, Modifier.matchParentSize())
                    AdultSceneStatus44(c, state, event, accent, Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 14.dp))
                }

                Column(Modifier.fillMaxWidth().offset(y = (-16).dp).padding(horizontal = 14.dp)) {
                    Interface41NarrativeGlass(
                        eyebrow = CinematicAdult44.sceneLabel(c, event),
                        title = event.title,
                        body = event.text,
                        accent = accent,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AdultContextStrip44(home, identity, identityState, deep, learnedTechnique, accent)

                    Spacer(Modifier.height(13.dp))
                    Text("TA RÉPONSE", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(7.dp))
                    event.choices.forEachIndexed { index, choice ->
                        val kind = CinematicAdult44.choiceKind(choice)
                        val choiceAccent = when (kind) {
                            "TECHNIQUE" -> accent
                            "IDENTITÉ" -> UltimateGold
                            "DANGER" -> UltimateRed
                            else -> UltimateBlue
                        }
                        Interface41Choice(
                            number = index + 1,
                            label = choice.label,
                            consequence = adultChoiceHint44(choice),
                            risk = choice.risk,
                            accent = choiceAccent,
                            onClick = { onChoice(event, choice) }
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultSceneStatus44(c: Campaign, state: UltimateState, event: EventNode, accent: Color, modifier: Modifier) {
    Column(modifier) {
        Text("${c.age} ANS · ${c.phaseLabel.uppercase()}", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.1.sp)
        Text("${state.mediaFrame} · ${state.legalStatus}", color = UltimateIvory.copy(alpha = .82f), fontSize = 9.sp)
        if (event.stakes >= 6) Text("RISQUE ${event.stakes}/8", color = UltimateRed, fontWeight = FontWeight.Black, fontSize = 8.sp)
    }
}

@Composable
private fun AdultContextStrip44(
    home: DistrictLifeState?,
    identity: SecretIdentityState?,
    identityState: LifeSimulationState?,
    deep: DeepLifeState,
    learned: LearnedTechnique?,
    accent: Color
) {
    val known = identityState?.let(IdentityPressureDirector::knownCount) ?: 0
    if (home == null && identity == null && learned == null) return
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (home != null) {
            UltimatePanel(Modifier.weight(1f), accent = UltimateBlue) {
                Text("QUARTIER", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 8.sp)
                Text("Confiance ${home.localTrust}", color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("Crime ${home.criminalControl} · médias ${home.mediaHeat}", color = UltimateMuted, fontSize = 8.sp)
            }
        }
        if (identity != null && (identity.exposure > 20 || identity.activeRumors.isNotEmpty() || identity.evidenceIds.isNotEmpty())) {
            val identityAccent = if (identity.exposure >= 70 || identity.knownBy.values.any { it == SecretKnowledge.THREATENS }) UltimateRed else UltimateGold
            UltimatePanel(Modifier.weight(1f), accent = identityAccent) {
                Text("SECRET", color = identityAccent, fontWeight = FontWeight.Black, fontSize = 8.sp)
                Text("Exposition ${identity.exposure}%", color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Text("${identity.evidenceIds.size} preuve(s) · $known au courant", color = UltimateMuted, fontSize = 8.sp)
            }
        }
    }
    if (learned != null) {
        Spacer(Modifier.height(7.dp))
        UltimatePanel(Modifier.fillMaxWidth(), accent = accent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TECHNIQUE DISPONIBLE", color = accent, fontWeight = FontWeight.Black, fontSize = 8.sp, letterSpacing = 1.1.sp)
                    Text(learned.name, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Text("${learned.proficiency}/100", color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

private fun adultChoiceHint44(choice: Choice): String {
    val signals = mutableListOf<String>()
    when {
        StoryTechniqueDirector.techniqueId(choice) != null -> signals += "emploie une technique réellement apprise"
        IdentityNarrativeDirector.isIdentityChoice(choice) -> signals += "agit directement sur ton identité secrète"
    }
    if (choice.power > 0) signals += "engage ton pouvoir"
    if (choice.identityDelta > 0) signals += "augmente ton exposition"
    if (choice.identityDelta < 0) signals += "protège ton secret"
    if (choice.healthDelta < 0) signals += "coût physique"
    if (choice.relationDelta > 0) signals += "renforce un lien"
    if (choice.relationDelta < 0) signals += "fragilise un lien"
    if (choice.deferredHook) signals += "peut revenir plus tard"
    signals += when {
        choice.risk >= 7 -> "risque extrême"
        choice.risk >= 4 -> "risque élevé"
        choice.risk >= 2 -> "risque réel"
        else -> "risque contenu"
    }
    return signals.distinct().take(3).joinToString(" · ")
}
