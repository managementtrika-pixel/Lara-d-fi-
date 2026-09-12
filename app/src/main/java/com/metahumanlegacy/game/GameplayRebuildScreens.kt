package com.metahumanlegacy.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
internal fun GameplayRebuildDestinyScreen(
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
    val accent = if (awakening) UltimateViolet else if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue

    MhlSceneFrame(
        "rebuild-destiny-${c.seed}-${c.turn}-${outcome?.hashCode()}",
        if (awakening) MotionBoard.AWAKENING else if (outcome == null) MotionBoard.PANEL_TRANSITION else MotionBoard.OUTCOME_REVEAL,
        if (awakening) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD,
        Modifier.fillMaxSize(),
        accent
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            LifeSceneBackdrop(c, state, accent)
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (outcome != null) {
                    ConsequenceScene(c, state, outcome, accent, onContinue)
                } else if (awakening && event != null) {
                    AwakeningSceneRebuild(c, state, deep, event, onChoice)
                } else if (event != null) {
                    HumanLifeScene(c, state, deep, event, onChoice)
                }
            }
        }
    }
}

@Composable
private fun LifeSceneBackdrop(c: Campaign, state: UltimateState, accent: Color) {
    Box(
        Modifier.fillMaxWidth().height(if (c.turn == 10) 205.dp else 168.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = if (c.turn == 10) .30f else .16f),
                        Color(0xFF07111C),
                        Color(0xFF05080D)
                    )
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val horizon = size.height * .63f
            drawRect(Color(0x33102030), size = androidx.compose.ui.geometry.Size(size.width, horizon))
            val buildings = 11
            val width = size.width / buildings
            repeat(buildings) { i ->
                val seed = ((c.seed shr (i % 8)) + c.turn * 31L + i * 17L).absoluteValue
                val h = size.height * (.13f + (seed % 29) / 100f)
                val top = horizon - h
                drawRect(
                    Color(0xFF0A1420),
                    topLeft = androidx.compose.ui.geometry.Offset(i * width, top),
                    size = androidx.compose.ui.geometry.Size(width - 2f, h)
                )
                if (i % 2 == c.turn % 2) {
                    drawRect(
                        accent.copy(alpha = .32f),
                        topLeft = androidx.compose.ui.geometry.Offset(i * width + width * .3f, top + h * .35f),
                        size = androidx.compose.ui.geometry.Size(width * .18f, 3f)
                    )
                }
            }
            drawLine(accent.copy(alpha = .45f), start = androidx.compose.ui.geometry.Offset(0f, horizon), end = androidx.compose.ui.geometry.Offset(size.width, horizon), strokeWidth = 2f)
            if (c.turn == 10) {
                drawCircle(accent.copy(alpha = .10f), radius = size.minDimension * .28f, center = center)
                drawCircle(accent.copy(alpha = .28f), radius = size.minDimension * .18f, center = center, style = Stroke(width = 3f))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Text("${c.age} ANS", color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.4.sp)
            Text(sceneLocation(c, state).uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(sceneAtmosphere(c, state), color = UltimateMuted, fontSize = 10.sp)
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
            UltimatePortrait(c, state, Modifier.size(82.dp).clip(CutCornerShape(12.dp)), heroMode = c.powerRevealed)
        }
    }
}

@Composable
private fun HumanLifeScene(c: Campaign, state: UltimateState, deep: DeepLifeState, event: EventNode, onChoice: (EventNode, Choice) -> Unit) {
    val person = SceneContextDirector.participant(event, deep)
    Text(if (c.turn < 10) "UN MOMENT QUI TE FAÇONNE" else "LE MONDE ATTEND TA RÉPONSE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.2.sp)
    Spacer(Modifier.height(5.dp))
    Text(event.title, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 29.sp)
    Spacer(Modifier.height(9.dp))

    if (person != null) {
        PersonPresenceCard(person)
        Spacer(Modifier.height(8.dp))
    }

    Text(event.text, color = UltimateIvory, lineHeight = 21.sp, fontSize = 14.sp)
    Spacer(Modifier.height(10.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ContextCard("CE QUE TU SAIS", knowledgeLine(c, state, event), UltimateBlue, Modifier.weight(1f))
        ContextCard("CE QUI PEUT BASCULER", stakesLine(event), if (event.stakes >= 4) UltimateRed else UltimateGold, Modifier.weight(1f))
    }

    if (c.turn < 10) {
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateViolet) {
            Text("RIEN N'ANNONCE ENCORE TON POUVOIR", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("Cette décision restera dans ta mémoire. Le jeu ne te révèle pas la formule derrière ce qu'elle construit.", color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("QU'EST-CE QUE TU FAIS ?", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(6.dp))
    event.choices.forEachIndexed { index, choice ->
        RebuildChoiceCard(index + 1, choice, onClick = { onChoice(event, choice) })
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun AwakeningSceneRebuild(c: Campaign, state: UltimateState, deep: DeepLifeState, event: EventNode, onChoice: (EventNode, Choice) -> Unit) {
    Text("18 ANS · L'ÉVEIL", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp)
    Text("Tout ce que tu as été revient d'un coup.", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp)
    Spacer(Modifier.height(9.dp))
    Text(event.text, color = UltimateIvory, lineHeight = 21.sp, fontSize = 14.sp)
    Spacer(Modifier.height(12.dp))

    UltimatePanel(accent = UltimateViolet) {
        Text("CONSTELLATION DE MÉMOIRES", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
        val memories = DeepLifeDirector.awakeningMemoryLines(deep, 5)
        if (memories.isEmpty()) {
            Text("Dix années de décisions convergent vers ce moment.", color = UltimateMuted, fontSize = 11.sp)
        } else {
            memories.forEachIndexed { index, memory ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(UltimateViolet.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                        Text((index + 1).toString(), color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
                    }
                    Spacer(Modifier.size(7.dp))
                    Text(memory, color = UltimateIvory, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    UltimatePanel(accent = UltimateGold) {
        Text("TU NE CHOISIS PAS TON POUVOIR", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
        Text("Tu choisis seulement comment tu traverses l'instant. La manifestation qui en sort appartient à la personne que ces dix années ont construite.", color = UltimateIvory, fontSize = 12.sp, lineHeight = 18.sp)
    }
    Spacer(Modifier.height(12.dp))
    event.choices.forEachIndexed { index, choice ->
        RebuildChoiceCard(index + 1, choice, awakening = true) { onChoice(event, choice) }
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun ConsequenceScene(c: Campaign, state: UltimateState, outcome: String, accent: Color, onContinue: () -> Unit) {
    val blocks = outcome.split("\n\n").filter { it.isNotBlank() }
    Text("APRÈS", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.4.sp)
    Text(consequenceHeadline(c, outcome), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 26.sp, lineHeight = 28.sp)
    Spacer(Modifier.height(10.dp))
    blocks.take(1).forEach { Text(it, color = UltimateIvory, lineHeight = 21.sp, fontSize = 14.sp) }
    blocks.drop(1).take(3).forEach { block ->
        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = if (block.contains("MONDE", true)) UltimateBlue else if (block.contains("TRACE", true)) UltimateGold else accent) {
            Text(block, color = UltimateMuted, lineHeight = 18.sp, fontSize = 11.sp)
        }
    }
    Spacer(Modifier.height(10.dp))
    if (c.powerRevealed) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            ContextCard("LA VILLE", if (state.cityCondition < 45) "Elle porte encore les dégâts." else "Elle continue de respirer autour de toi.", UltimateBlue, Modifier.weight(1f))
            ContextCard("TON CORPS", if (c.health < 45) "Cette fois, tu ne t'en sors pas indemne." else "Tu peux encore continuer.", if (c.health < 45) UltimateRed else UltimateGreen, Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(12.dp))
    MhlPrimaryButton(if (c.needsAlias) "Donner un nom à ce qui vient de naître" else "Continuer ta vie", onContinue, Modifier.fillMaxWidth())
}

@Composable
private fun PersonPresenceCard(person: DeepRelationship) {
    Row(
        Modifier.fillMaxWidth().clip(CutCornerShape(topEnd = 13.dp, bottomStart = 13.dp))
            .background(Color(0xC9121821)).border(1.dp, UltimateGold.copy(alpha = .35f), CutCornerShape(topEnd = 13.dp, bottomStart = 13.dp))
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(21.dp)).background(UltimateGold.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
            Text(person.name.take(1).uppercase(), color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Spacer(Modifier.size(9.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 13.sp)
            Text("${person.role} · ${relationshipHumanLabel(person)}", color = UltimateMuted, fontSize = 10.sp)
        }
        if (person.knowsIdentity) Text("SAIT", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 8.sp)
    }
}

@Composable
private fun ContextCard(title: String, text: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(CutCornerShape(topEnd = 11.dp, bottomStart = 11.dp)).background(Color(0xB9111720)).border(1.dp, accent.copy(alpha = .35f), CutCornerShape(topEnd = 11.dp, bottomStart = 11.dp)).padding(9.dp)) {
        Text(title, color = accent, fontWeight = FontWeight.Black, fontSize = 8.sp)
        Spacer(Modifier.height(3.dp))
        Text(text, color = UltimateIvory, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun RebuildChoiceCard(number: Int, choice: Choice, awakening: Boolean = false, onClick: () -> Unit) {
    val riskAccent = when {
        choice.risk >= 7 -> UltimateRed
        choice.risk >= 4 -> UltimateGold
        awakening -> UltimateViolet
        else -> UltimateBlue
    }
    Column(
        Modifier.fillMaxWidth().clip(CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xF8131922), riskAccent.copy(alpha = .08f))))
            .border(1.dp, riskAccent.copy(alpha = .42f), CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp))
            .padding(11.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(riskAccent.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Text(number.toString(), color = riskAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
            }
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(choice.label, color = UltimateIvory, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(choiceHumanConsequence(choice), color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        MhlPrimaryButton("Faire ce choix", onClick, Modifier.fillMaxWidth())
    }
}

private fun sceneLocation(c: Campaign, state: UltimateState): String = when {
    c.turn < 3 -> "${c.district} · près de chez toi"
    c.turn < 7 -> "${c.city} · ${state.cityMood.lowercase()}"
    c.turn < 10 -> "${c.city} · une nuit ordinaire"
    c.turn == 10 -> "${c.city} · là où tout bascule"
    c.scope >= Scope.CITY -> "${c.city} · secteur sous tension"
    else -> "${c.district} · ton territoire"
}

private fun sceneAtmosphere(c: Campaign, state: UltimateState): String = when {
    c.turn == 10 -> "Le bruit de la ville s'éteint une seconde. Quelque chose répond à l'intérieur."
    c.turn < 10 -> "${state.climate} · ${state.architecture.lowercase()} · tu n'es encore personne pour le reste du monde"
    c.health < 40 -> "Tu arrives déjà diminué. Chaque erreur coûte plus cher."
    else -> "${state.cityArchetype} · ${state.cityMood.lowercase()}"
}


private fun knowledgeLine(c: Campaign, state: UltimateState, event: EventNode): String = when {
    c.turn < 10 -> "Tu connais seulement ce que tu as vu et ce que les adultes veulent bien te dire."
    event.threadStage > 1 -> "Ce problème a déjà une histoire. Tes anciennes décisions pèsent sur cette scène."
    state.cases.any { !it.solved && it.evidence >= 45 } -> "Tu as des indices, mais pas toute la vérité."
    else -> "Tu n'as pas toutes les informations. Agir maintenant signifie accepter une part d'inconnu."
}

private fun stakesLine(event: EventNode): String = when (event.stakes) {
    1 -> "Surtout une relation ou une impression durable."
    2 -> "Quelqu'un peut se souvenir longtemps de ce que tu fais."
    3 -> "La situation peut changer une relation, une réputation ou ton corps."
    4 -> "Plusieurs personnes peuvent payer le prix de ta décision."
    else -> "Cette scène peut devenir un avant et un après dans ta vie."
}

private fun choiceHumanConsequence(choice: Choice): String {
    val hints = buildList {
        if (choice.relationDelta >= 2) add("Tu te rapproches de quelqu'un")
        if (choice.relationDelta <= -2) add("Tu risques de blesser une relation")
        if (choice.healthDelta <= -5 || choice.risk >= 6) add("Ton corps peut en payer le prix")
        if (choice.identityDelta > 0) add("Tu laisses des traces")
        if (choice.identityDelta < 0) add("Tu protèges ton secret")
        if (choice.deferredHook) add("Cette décision peut revenir plus tard")
        if (choice.impact >= 4) add("Le monde peut s'en souvenir")
    }
    return hints.take(2).joinToString(" · ").ifBlank { "Tu ne peux pas encore savoir exactement ce que ce choix déclenchera." }
}

private fun relationshipHumanLabel(person: DeepRelationship): String = when (person.phase) {
    RelationshipPhase.CLOSE, RelationshipPhase.TRUSTED -> "proche"
    RelationshipPhase.HURT -> "blessé par toi"
    RelationshipPhase.DISTANT -> "s'éloigne"
    RelationshipPhase.RIVAL -> "rivalité"
    RelationshipPhase.MENTOR -> "te guide"
    RelationshipPhase.PROTEGE -> "compte sur toi"
    RelationshipPhase.SUCCESSOR -> "porte déjà une part de ton héritage"
    else -> person.phase.name.lowercase().replace('_', ' ')
}

private fun consequenceHeadline(c: Campaign, outcome: String): String = when {
    c.turn == 11 && c.powerRevealed -> "Le monde ne te verra plus jamais de la même façon."
    outcome.contains("bless", true) || c.health < 40 -> "Tu continues, mais pas intact."
    outcome.contains("relation", true) -> "Quelqu'un s'en souviendra."
    c.age < 18 -> "Tu ne sais pas encore ce que ce moment vient de changer."
    else -> "La ville absorbe ton choix. Toi aussi."
}
