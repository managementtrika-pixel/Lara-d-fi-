package com.metahumanlegacy.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DeepLifeChronicleScreen(c: Campaign, u: UltimateState, d: DeepLifeState) {
    val accent = if (c.powerRevealed) powerVisualProfile(c.powerFamily).accent else UltimateBlue
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Chronique vivante", "La personne derrière la légende", "Pas seulement des statistiques : ce que tu es devenu, ce que les autres retiennent et ce que tu laisses derrière toi.", accent)
        Spacer(Modifier.height(10.dp))

        UltimatePanel(accent = accent) {
            Text("IDENTITÉ ACTUELLE", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("${c.alias.ifBlank { c.name }} · ${c.age} ans · ${c.scope.label}", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("${u.mediaFrame} · ${u.legalStatus}", color = UltimateMuted, fontSize = 11.sp)
            if (u.retirementIntent != "Indécis") Text("Après : ${u.retirementIntent}", color = UltimateGold, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))
        val traits = d.personality.entries.sortedByDescending { it.value }.take(6)
        UltimatePanel(accent = UltimateViolet) {
            Text("CE QUE TES CHOIX ONT FAIT DE TOI", color = UltimateViolet, fontWeight = FontWeight.Black, fontSize = 9.sp)
            if (traits.isEmpty()) Text("Ton caractère est encore en train de se dessiner.", color = UltimateMuted)
            traits.forEach { (name, value) ->
                Text("${name.lowercase().replaceFirstChar { it.uppercase() }} · ${value.coerceIn(-100, 100)}", color = UltimateIvory, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateGold) {
            Text("PERCEPTIONS — PAS UNE MORALE UNIVERSELLE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            val p = d.perception
            Text("Quartier ${signed(p.district)} · ville ${signed(p.city)} · national ${signed(p.national)}", color = UltimateIvory, fontSize = 11.sp)
            Text("Gouvernement ${signed(p.government)} · police ${signed(p.police)} · civils ${signed(p.civilians)}", color = UltimateIvory, fontSize = 11.sp)
            Text("Peur criminelle ${p.criminalFear.coerceIn(0,100)} · peur civile ${p.civilianFear.coerceIn(0,100)}", color = UltimateMuted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateGreen) {
            Text("LIENS QUI ONT UNE MÉMOIRE", color = UltimateGreen, fontWeight = FontWeight.Black, fontSize = 9.sp)
            d.relationships.filter { it.alive }.sortedByDescending { it.trust + it.affection }.take(8).forEach { r ->
                Text("${r.name} · ${r.role} · ${phaseLabel(r.phase)}", color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Confiance ${r.trust} · affection ${r.affection} · rancune ${r.grudge}${if (r.knowsIdentity) " · connaît ton identité" else ""}", color = UltimateMuted, fontSize = 10.sp)
                r.memories.maxByOrNull { it.weight }?.let { Text("↳ ${it.age} ans — ${it.summary}", color = UltimateGold, fontSize = 10.sp) }
                Spacer(Modifier.height(5.dp))
            }
        }

        if (c.powerRevealed) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = accent) {
                Text("POUVOIR EN DEVENIR", color = accent, fontWeight = FontWeight.Black, fontSize = 9.sp)
                val power = d.powerEvolution
                if (power == null) Text(c.powerFamily, color = UltimateIvory) else {
                    Text("${power.manifestation} · ${power.architecture.name.lowercase()} · ${power.branch}", color = UltimateIvory, fontWeight = FontWeight.Black)
                    Text("Maîtrise ${power.mastery}% · surcharge ${power.strain}% · faiblesse : ${c.weakness}", color = UltimateMuted, fontSize = 11.sp)
                    power.unlockedTechniques.takeLast(6).forEach { Text("• $it", color = UltimateIvory, fontSize = 11.sp) }
                }
            }
        }

        if (d.opportunities.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateBlue) {
                Text("CE QUE TU NE PEUX PAS TOUT FAIRE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
                d.opportunities.sortedByDescending { it.urgency }.forEach { o ->
                    val turns = (o.expiresTurn - c.turn).coerceAtLeast(0)
                    Text("${o.title} · expire dans ${if (turns == 0) "ce chapitre" else "$turns chapitre(s)"}", color = if (o.urgency >= 4) UltimateRed else UltimateIvory, fontSize = 11.sp)
                }
            }
        }

        if (d.injuries.isNotEmpty() || d.identityEvidence.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            UltimatePanel(accent = UltimateRed) {
                Text("CE QUE LA VIE LAISSE", color = UltimateRed, fontWeight = FontWeight.Black, fontSize = 9.sp)
                d.injuries.takeLast(6).forEach { i ->
                    Text("${i.bodyPart.replaceFirstChar { it.uppercase() }} · ${i.originAge} ans · ${if (i.chronic) "chronique" else "récupération ${i.recovery}%"}", color = UltimateIvory, fontSize = 11.sp)
                }
                if (d.identityEvidence.isNotEmpty()) {
                    val pressure = d.identityEvidence.sumOf { it.strength }
                    Text("Indices d'identité : ${d.identityEvidence.size} · pression cumulée $pressure", color = UltimateMuted, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = Color(0xFFFFA64D)) {
            Text("RYTHME DE TA VIE", color = Color(0xFFFFA64D), fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("Tension ${d.drama.tension}% · besoin de récupération ${d.drama.recoveryNeed}% · pression personnelle ${d.drama.personalPressure}% · monde ${d.drama.worldPressure}%", color = UltimateIvory, fontSize = 11.sp)
        }

        Spacer(Modifier.height(8.dp))
        UltimatePanel(accent = UltimateMuted) {
            Text("SOUVENIRS FONDATEURS", color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 9.sp)
            d.memories.sortedByDescending { it.weight }.take(12).sortedBy { it.turn }.forEach { m ->
                Text("${m.age} ans — ${m.summary}", color = if (m.weight >= 7) UltimateGold else UltimateMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
internal fun DeepLegacyHallScreen(oldHallCount: Int, records: List<DeepLegacyRecord>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(13.dp).verticalScroll(rememberScrollState())) {
        UltimateSectionHeader("Hall of Legacies", "Les vies que le monde n'a pas oubliées", "Chaque fin conserve désormais les personnes, blessures, techniques et souvenirs qui ont réellement défini la carrière.", UltimateGold)
        Spacer(Modifier.height(8.dp))
        Text("$oldHallCount legacy(s) classique(s) · ${records.size} archive(s) profonde(s)", color = UltimateMuted, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        if (records.isEmpty()) {
            UltimatePanel(accent = UltimateBlue) {
                Text("Le musée attend sa première vie 2.0.", color = UltimateIvory, fontWeight = FontWeight.Black)
                Text("Termine une destinée : ses souvenirs les plus lourds survivront au personnage.", color = UltimateMuted)
            }
        }
        records.forEach { r ->
            UltimatePanel(Modifier.fillMaxWidth(), accent = UltimateGold) {
                Text(r.alias.ifBlank { r.name }.uppercase(), color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("${r.power} · ${r.finalAge} ans · portée ${r.scope}", color = UltimateIvory, fontSize = 11.sp)
                Text(r.headline, color = UltimateIvory, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                if (r.personality.isNotBlank()) Text("Traits : ${r.personality}", color = UltimateMuted, fontSize = 10.sp)
                if (r.strongestRelationship.isNotBlank()) Text("Lien majeur : ${r.strongestRelationship}", color = UltimateGreen, fontSize = 10.sp)
                if (r.nemesis.isNotBlank()) Text("Némésis : ${r.nemesis}", color = UltimateRed, fontSize = 10.sp)
                if (r.lastingInjury.isNotBlank()) Text("Trace physique : ${r.lastingInjury}", color = UltimateMuted, fontSize = 10.sp)
                if (r.techniques.isNotEmpty()) Text("Techniques : ${r.techniques.joinToString(" · ")}", color = UltimateViolet, fontSize = 10.sp)
                if (r.memories.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text("MOMENTS RETENUS", color = UltimateGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    r.memories.take(5).forEach { Text("• $it", color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp) }
                }
                Text(r.perception, color = UltimateBlue, fontSize = 9.sp)
            }
            Spacer(Modifier.height(8.dp))
        }
        MhlSecondaryButton("Retour", onBack, Modifier.fillMaxWidth())
        Spacer(Modifier.height(40.dp))
    }
}

private fun phaseLabel(p: RelationshipPhase) = when (p) {
    RelationshipPhase.STRANGER -> "inconnu"
    RelationshipPhase.ACQUAINTANCE -> "connaissance"
    RelationshipPhase.FRIEND -> "ami"
    RelationshipPhase.CLOSE -> "proche"
    RelationshipPhase.TRUSTED -> "confiance absolue"
    RelationshipPhase.HURT -> "blessé"
    RelationshipPhase.DISTANT -> "distant"
    RelationshipPhase.RIVAL -> "rival"
    RelationshipPhase.MENTOR -> "mentor"
    RelationshipPhase.EQUAL -> "égal"
    RelationshipPhase.PROTEGE -> "protégé"
    RelationshipPhase.SUCCESSOR -> "successeur"
}
