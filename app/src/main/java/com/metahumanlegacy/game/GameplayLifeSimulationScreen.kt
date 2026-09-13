package com.metahumanlegacy.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GameplayRebuildActionsHub(
    c: Campaign,
    state: UltimateState,
    annual: AnnualActionState,
    deep: DeepLifeState,
    onAnnualAction: (AnnualActionCard) -> AnnualActionResult?,
    onLifeAction: (LifeAction) -> LifeActionResult?,
    onBack: () -> Unit
) {
    var classic by remember(c.seed) { mutableStateOf(false) }
    if (classic) {
        GameplayRebuildActionsScreen(
            c = c,
            state = state,
            annual = annual,
            deep = deep,
            onAction = onAnnualAction,
            onBack = { classic = false }
        )
    } else {
        GameplayLifeSimulationScreen(
            c = c,
            deep = deep,
            onAction = onLifeAction,
            onCareerActions = { classic = true },
            onBack = onBack
        )
    }
}

@Composable
private fun GameplayLifeSimulationScreen(
    c: Campaign,
    deep: DeepLifeState,
    onAction: (LifeAction) -> LifeActionResult?,
    onCareerActions: () -> Unit,
    onBack: () -> Unit
) {
    val raw = deep.lifeSimulation ?: LifeSimulationDirector.bootstrap(c, deep)
    val simulation = LifeSimulationDirector.synced(c, deep, raw)
    var feedback by remember(c.seed, c.turn) { mutableStateOf<LifeActionResult?>(null) }
    val peopleById = deep.relationships.associateBy { it.id }
    val actions = LifeSimulationDirector.availableActions(c, simulation)

    Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
        Text("TA VIE, PAS JUSTE TA LÉGENDE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
        Text("${simulation.civil.freeMoments} moment${if (simulation.civil.freeMoments > 1) "s" else ""} à choisir à ${c.age} ans.", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 25.sp)
        Text("Travail, proches, logement, récupération et pouvoir se disputent le même temps.", color = UltimateMuted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))

        UltimatePanel(accent = UltimateBlue) {
            Text("VIE CIVILE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text(simulation.civil.jobTitle, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text("Économies ${simulation.civil.savings} · logement ${housingLabel(simulation.civil.housing)}", color = UltimateMuted, fontSize = 11.sp)
            Text("Stress ${lifeBand(simulation.civil.stress)} · progression ${lifeBand(simulation.civil.careerProgress)}", color = UltimateMuted, fontSize = 11.sp)
        }

        val closePeople = simulation.relationshipLives.sortedByDescending { it.closeness }.take(4)
        if (closePeople.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            UltimatePanel(accent = UltimateGold) {
                Text("TES LIENS", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                closePeople.forEach { rel ->
                    val person = peopleById[rel.personId]
                    val secret = when (rel.secretKnowledge) {
                        SecretKnowledge.KNOWS, SecretKnowledge.PROTECTS -> " · connaît ton identité"
                        SecretKnowledge.SUSPECTS -> " · soupçonne quelque chose"
                        SecretKnowledge.THREATENS -> " · identité sous tension"
                        else -> ""
                    }
                    Text(
                        "${person?.name ?: rel.personId} · ${relationshipBand(rel.closeness)}$secret",
                        color = UltimateIvory,
                        fontSize = 11.sp
                    )
                }
                Text("Consacrer du temps à quelqu'un renforce réellement le lien et ouvre de nouvelles actions.", color = UltimateMuted, fontSize = 10.sp)
            }
        }

        if (c.powerRevealed) {
            Spacer(Modifier.height(7.dp))
            UltimatePanel(accent = UltimateViolet) {
                Text("MAÎTRISE DU POUVOIR", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text("Contrôle ${lifeBand(simulation.powerRules.control)} · précision ${lifeBand(simulation.powerRules.precision)}", color = UltimateIvory, fontSize = 11.sp)
                Text("Fatigue ${lifeBand(simulation.powerRules.fatigue)} · surcharge ${lifeBand(simulation.powerRules.overload)}", color = if (simulation.powerRules.overload >= 70) UltimateRed else UltimateMuted, fontSize = 11.sp)
                Spacer(Modifier.height(5.dp))
                val unlocked = simulation.powerRules.techniques.filter { it.unlocked }
                if (unlocked.isEmpty()) {
                    Text("Aucune technique stabilisée pour l'instant. L'entraînement peut en faire émerger une.", color = UltimateMuted, fontSize = 10.sp)
                } else {
                    Text("TECHNIQUES", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 8.sp)
                    unlocked.take(4).forEach { technique ->
                        val cooldown = if (technique.cooldownTurns > 0) " · récupération" else " · prête"
                        Text(
                            "${technique.name} · maîtrise ${technique.proficiency}%$cooldown",
                            color = if (technique.cooldownTurns > 0) UltimateMuted else UltimateIvory,
                            fontSize = 10.sp
                        )
                    }
                }
                simulation.powerRules.techniques.firstOrNull { !it.unlocked }?.let { next ->
                    Text("Prochaine : ${next.name} · seuil ${next.masteryRequired}", color = UltimateMuted, fontSize = 9.sp)
                }
            }
        }

        val district = simulation.districts.firstOrNull { it.id == "quartier" }
        if (district != null) {
            Spacer(Modifier.height(7.dp))
            UltimatePanel(accent = UltimateGreen) {
                Text("TON QUARTIER", color = UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text("Sécurité ${lifeBand(district.safety)} · emprise criminelle ${lifeBand(district.criminalControl)}", color = UltimateIvory, fontSize = 11.sp)
                Text("Confiance locale ${lifeBand(district.localTrust)} · attention média ${lifeBand(district.mediaHeat)}", color = UltimateMuted, fontSize = 11.sp)
            }
        }

        feedback?.let { result ->
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateGold) {
                Text(result.headline.uppercase(), color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
                Text(result.detail, color = UltimateIvory, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("QUE FAIS-TU DE TON TEMPS ?", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
        Spacer(Modifier.height(6.dp))
        actions.forEach { action ->
            val name = peopleById[action.targetId]?.name ?: "un proche"
            val closeness = simulation.relationshipLives.firstOrNull { it.personId == action.targetId }?.closeness
            val label = when (action.type) {
                LifeActionType.VISIT_PERSON -> "Voir $name${closeness?.let { " · ${relationshipBand(it)}" } ?: ""}"
                LifeActionType.ASK_HELP -> "Demander de l'aide à $name"
                LifeActionType.REVEAL_IDENTITY -> "Révéler ton identité à $name"
                LifeActionType.DISTANCE_PERSON -> "Prendre de la distance avec $name"
                LifeActionType.USE_TECHNIQUE -> action.label
                else -> action.label
            }
            MhlSecondaryButton(
                label,
                {
                    val result = onAction(action)
                    if (result != null) feedback = result
                },
                Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(5.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MhlSecondaryButton("Actions de carrière", onCareerActions, Modifier.weight(1f))
            Spacer(Modifier.padding(3.dp))
            MhlSecondaryButton("Retour", onBack, Modifier.weight(1f))
        }
    }
}

private fun housingLabel(value: HousingTier): String = when (value) {
    HousingTier.FAMILY_HOME -> "chez les proches"
    HousingTier.ROOM -> "chambre"
    HousingTier.STUDIO -> "studio"
    HousingTier.APARTMENT -> "appartement"
    HousingTier.HOUSE -> "maison"
    HousingTier.BASE -> "base"
}

private fun relationshipBand(value: Int): String = when {
    value >= 75 -> "confiance profonde"
    value >= 50 -> "très proche"
    value >= 35 -> "proche"
    value >= 20 -> "lien réel"
    value < 0 -> "distant"
    else -> "connaissance"
}

private fun lifeBand(value: Int): String = when {
    value >= 85 -> "extrême"
    value >= 65 -> "élevé"
    value >= 40 -> "marqué"
    value >= 20 -> "modéré"
    else -> "faible"
}
