package com.metahumanlegacy.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The illustration, story and controls occupy separate space; effects never cover the text. */
@Composable
internal fun FinalDestinyScreen(
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
    val motion = LocalMetahumanMotion.current.settings
    val accent = when {
        c.turn == 10 && !c.powerRevealed -> UltimateViolet
        c.powerRevealed -> powerVisualProfile(c.powerFamily).accent
        else -> UltimateBlue
    }
    val event = remember(c, state, annual, deep, outcome) {
        if (outcome == null && !c.finished) FinalGameRules.event(c, state, annual, deep) else null
    }
    Column(Modifier.fillMaxSize().background(if (motion.highContrast) Color.Black else Interface41.ink)) {
        Interface41TopHud(c, state, annual, savePulse, onHome, onSettings)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val sceneHeight = if (maxHeight < 520.dp) 166.dp else 215.dp
            key(c.seed, c.turn, outcome != null) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("final_story_scroll")) {
                    Box(Modifier.fillMaxWidth().height(sceneHeight).clip(RoundedCornerShape(bottomStart = 24.dp))) {
                        CinematicLifeScene43(c, state, accent, Modifier.fillMaxSize())
                        if (c.powerRevealed && !motion.highContrast && !motion.reduceMotion) {
                            CinematicPowerVfx(c.powerFamily, .28f, Modifier.matchParentSize())
                        }
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp)) {
                        if (outcome != null) {
                            FinalConsequence(c, outcome, accent, onContinue)
                        } else if (event != null) {
                            val awakening = c.turn == 10 && !c.powerRevealed
                            Text(if (awakening) "18 ANS · L'ÉVEIL" else if (c.age < 18) "UNE ANNÉE QUI TE FAÇONNE" else c.scope.label.uppercase(),
                                color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(event.title, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 25.sp, lineHeight = 30.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(event.text, color = UltimateIvory, fontSize = 16.sp, lineHeight = 25.sp)
                            SceneContextDirector.participant(event, deep)?.let { person ->
                                Spacer(Modifier.height(14.dp))
                                Row(Modifier.fillMaxWidth().background(accent.copy(alpha = .08f), RoundedCornerShape(12.dp)).padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(person.name.take(1), color = accent, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(person.name, color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(person.role, color = UltimateMuted, fontSize = 12.sp)
                                    }
                                }
                            }
                            if (awakening) {
                                val memories = DeepLifeDirector.awakeningMemoryLines(deep, 3)
                                if (memories.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    Text("CE QUI REVIENT", color = UltimateGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    memories.forEach {
                                        Spacer(Modifier.height(7.dp))
                                        Text(it, color = UltimateMuted, fontSize = 14.sp, lineHeight = 21.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text("QUE FAIS-TU ?", color = accent, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.height(10.dp))
                            event.choices.forEachIndexed { index, choice ->
                                FinalChoice(index, choice, accent) { onChoice(event, choice) }
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
        Interface41Dock("DESTIN", annual.remaining, accent, onScreen)
    }
}

@Composable
private fun FinalChoice(index: Int, choice: Choice, accent: Color, onClick: () -> Unit) {
    val edge = when { choice.risk >= 7 -> UltimateRed; choice.risk >= 4 -> UltimateGold; else -> accent }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101B27)).border(1.dp, edge.copy(alpha = .45f), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick).testTag("final_choice_${index + 1}").padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(32.dp).background(edge.copy(alpha = .12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text("${index + 1}", color = edge, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(choice.label, color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp)
            val hint = finalChoiceHint(choice)
            if (hint.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(hint, color = UltimateMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun FinalConsequence(c: Campaign, outcome: String, accent: Color, onContinue: () -> Unit) {
    val blocks = outcome.split("\n\n").filter { it.isNotBlank() }
    Text("LES TRACES DE TON CHOIX", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    Text(blocks.firstOrNull().orEmpty(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 23.sp, lineHeight = 30.sp)
    blocks.drop(1).forEachIndexed { index, block ->
        Spacer(Modifier.height(if (index == 0) 12.dp else 18.dp))
        Text(block, color = if (index == 0) UltimateIvory else UltimateMuted,
            fontSize = if (index == 0) 16.sp else 14.sp, lineHeight = if (index == 0) 25.sp else 22.sp)
    }
    Spacer(Modifier.height(24.dp))
    MhlPrimaryButton(when {
        c.finished -> "Découvrir mon héritage"
        c.needsAlias -> "Construire mon identité"
        else -> "Continuer"
    }, onContinue, Modifier.fillMaxWidth().testTag("final_continue"))
}

private fun finalChoiceHint(choice: Choice): String = buildList {
    if (StoryTechniqueDirector.techniqueId(choice) != null) add("Utilise une technique")
    if (choice.relationDelta > 1) add("Peut renforcer un lien")
    if (choice.relationDelta < -1) add("Peut fragiliser un lien")
    if (choice.identityDelta > 0) add("Laisse des traces")
    if (choice.identityDelta < 0) add("Protège ton secret")
    if (choice.risk >= 7) add("Danger extrême") else if (choice.risk >= 4) add("Risque élevé")
    if (choice.deferredHook) add("Peut revenir plus tard")
}.take(2).joinToString(" · ")
