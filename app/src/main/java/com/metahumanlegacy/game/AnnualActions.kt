package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Random

internal const val ANNUAL_ACTION_LIMIT = 3

internal enum class AnnualActionCategory(val label: String) {
    CIVIL("Vie civile"),
    INTERVENTION("Intervention"),
    TRAINING("Entraînement"),
    INVESTIGATION("Enquête"),
    RELATION("Liens"),
    RECOVERY("Récupération"),
    PUBLIC("Présence")
}

internal data class AnnualActionCard(
    val id: String,
    val title: String,
    val description: String,
    val category: AnnualActionCategory,
    val iconKey: String,
    val focus: String,
    val outcome: String,
    val requiresPower: Boolean = false,
    val rescue: Int = 0,
    val investigation: Int = 0,
    val presence: Int = 0,
    val discipline: Int = 0,
    val health: Int = 0,
    val familyBond: Int = 0,
    val prestige: Int = 0,
    val opinion: Int = 0,
    val fear: Int = 0,
    val power: Int = 0,
    val control: Int = 0,
    val influence: Int = 0,
    val identityExposure: Int = 0,
    val factionStanding: Int = 0,
    val governmentStanding: Int = 0,
    val mediaStanding: Int = 0
)

internal data class AnnualActionState(
    val seed: Long,
    val turn: Int,
    val used: Int = 0,
    val rescue: Int = 0,
    val investigation: Int = 0,
    val presence: Int = 0,
    val discipline: Int = 0,
    val usedIds: Set<String> = emptySet()
) {
    val remaining: Int get() = (ANNUAL_ACTION_LIMIT - used).coerceAtLeast(0)

    fun synced(c: Campaign): AnnualActionState = when {
        seed != c.seed -> fresh(c)
        turn != c.turn -> copy(turn = c.turn, used = 0, usedIds = emptySet())
        else -> this
    }

    companion object {
        fun fresh(c: Campaign) = AnnualActionState(seed = c.seed, turn = c.turn)
    }
}

internal data class AnnualActionResult(
    val campaign: Campaign,
    val state: AnnualActionState,
    val title: String,
    val text: String
)

internal object AnnualActionStore {
    private const val PREFS = "mhl_annual_actions_v1"

    fun load(context: Context, c: Campaign): AnnualActionState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(c.seed.toString(), null)
        val parsed = raw?.let(::decode) ?: AnnualActionState.fresh(c)
        val synced = parsed.synced(c)
        if (synced != parsed) save(context, synced)
        return synced
    }

    fun save(context: Context, state: AnnualActionState) {
        val ids = state.usedIds.sorted().joinToString(",")
        val raw = listOf(
            state.turn, state.used, state.rescue, state.investigation,
            state.presence, state.discipline, ids
        ).joinToString("|")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(state.seed.toString(), raw).apply()
    }

    fun clear(context: Context, seed: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(seed.toString()).apply()
    }

    private fun decode(raw: String): AnnualActionState? = runCatching {
        val p = raw.split('|')
        // Seed is restored by synced() from the campaign key; -1 forces that safe reset path.
        AnnualActionState(
            seed = -1L,
            turn = p.getOrElse(0) { "0" }.toInt(),
            used = p.getOrElse(1) { "0" }.toInt().coerceIn(0, ANNUAL_ACTION_LIMIT),
            rescue = p.getOrElse(2) { "0" }.toInt().coerceIn(0, 100),
            investigation = p.getOrElse(3) { "0" }.toInt().coerceIn(0, 100),
            presence = p.getOrElse(4) { "0" }.toInt().coerceIn(0, 100),
            discipline = p.getOrElse(5) { "0" }.toInt().coerceIn(0, 100),
            usedIds = p.getOrElse(6) { "" }.split(',').filter { it.isNotBlank() }.toSet()
        )
    }.getOrNull()
}

internal object AnnualActionEngine {
    private val civilian = listOf(
        AnnualActionCard("civil_barber", "Passer chez le coiffeur", "Changer de coupe, soigner son apparence, prendre une heure pour soi.", AnnualActionCategory.CIVIL, "alt_02", "Présence", "Un détail banal, mais tu te reconnais un peu mieux dans le miroir.", presence = 2),
        AnnualActionCard("civil_first_aid", "Formation premiers secours", "Apprendre les gestes qui comptent avant même d'avoir quoi que ce soit d'extraordinaire.", AnnualActionCategory.TRAINING, "relation_family", "Secours · Discipline", "Tu apprends à agir vite sans transformer la panique en spectacle.", rescue = 4, discipline = 1),
        AnnualActionCard("civil_training", "T'entraîner sérieusement", "Course, renforcement, mobilité : rien de surnaturel, seulement de la régularité.", AnnualActionCategory.TRAINING, "alt_04", "Discipline · Santé", "Ton corps encaisse mieux l'effort et ta routine devient plus solide.", discipline = 3, health = 2),
        AnnualActionCard("civil_family", "Passer du temps avec les proches", "Dîner, appel, promenade ou simplement être là sans urgence autour.", AnnualActionCategory.RELATION, "relation_family", "Liens · Présence", "Cette fois, tu n'étais pas ailleurs quand ils avaient du temps pour toi.", familyBond = 4, presence = 1),
        AnnualActionCard("civil_volunteer", "Donner un coup de main au quartier", "Association, collecte, aide ponctuelle : une petite utilité sans costume ni caméra.", AnnualActionCategory.CIVIL, "scope_district", "Secours · Présence", "Personne n'en fera une légende. Quelques personnes auront juste eu une journée plus facile.", rescue = 2, presence = 2),
        AnnualActionCard("civil_walk", "Explorer la ville à pied", "Prendre des rues que tu ne prends jamais et observer ce qu'on ne voit pas depuis un écran.", AnnualActionCategory.INVESTIGATION, "scope_city", "Enquête", "Tu commences à lire la ville comme un ensemble de détails plutôt qu'un décor.", investigation = 3),
        AnnualActionCard("civil_study", "Te former sur un sujet", "Lire, pratiquer, comprendre quelque chose que tu ne savais pas faire hier.", AnnualActionCategory.TRAINING, "alt_03", "Enquête · Discipline", "Tu n'as rien gagné de spectaculaire, juste une compétence qui restera.", investigation = 2, discipline = 2),
        AnnualActionCard("civil_rest", "Prendre vraiment du repos", "Couper le bruit, dormir, récupérer et ne rien devoir prouver pendant un moment.", AnnualActionCategory.RECOVERY, "alt_07", "Santé · Discipline", "Tu récupères avant que la fatigue ne devienne une personnalité.", health = 4, discipline = 1)
    )

    private val metahuman = listOf(
        AnnualActionCard("meta_cat", "Un chat coincé sur un toit", "Une intervention minuscule à l'échelle du monde. Pas forcément à l'échelle de son propriétaire.", AnnualActionCategory.INTERVENTION, "scope_street", "Secours · Opinion", "Le sauvetage dure moins longtemps que les remerciements.", requiresPower = true, rescue = 1, opinion = 1),
        AnnualActionCard("meta_accident", "Répondre à un accident de la route", "Tôle froissée, circulation bloquée, témoins paniqués : agir avant les secours ou avec eux.", AnnualActionCategory.INTERVENTION, "alt_05", "Secours · Prestige", "Quelques minutes bien utilisées évitent que la scène empire.", requiresPower = true, rescue = 3, prestige = 1, opinion = 1),
        AnnualActionCard("meta_collapse", "Intervenir sur un immeuble qui s'effondre", "Structure instable, personnes coincées, secondes précieuses. Ce n'est pas un duel, c'est du triage.", AnnualActionCategory.INTERVENTION, "danger_08", "Secours · Maîtrise", "Tu repars couvert de poussière avec la sensation très concrète d'avoir pesé sur l'issue.", requiresPower = true, rescue = 5, prestige = 2, opinion = 2, influence = 2),
        AnnualActionCard("meta_fire", "Entrer dans un incendie", "Fumée, chaleur et visibilité nulle. Ton pouvoir aide, mais la méthode compte autant.", AnnualActionCategory.INTERVENTION, "danger_07", "Secours · Discipline", "Tu apprends une nouvelle différence entre être puissant et être utile.", requiresPower = true, rescue = 4, control = 1, prestige = 2),
        AnnualActionCard("meta_missing", "Chercher une personne disparue", "Pas de grand méchant. Des horaires, des traces, des témoins et une famille qui attend.", AnnualActionCategory.INVESTIGATION, "alt_03", "Enquête · Secours", "Les petits indices finissent par former une direction.", requiresPower = true, investigation = 4, rescue = 1, opinion = 1),
        AnnualActionCard("meta_patrol", "Faire une ronde discrète", "Voir ce qui se passe quand aucune catastrophe n'a encore gagné les gros titres.", AnnualActionCategory.INVESTIGATION, "scope_district", "Enquête · Portée", "Tu apprends les rythmes du quartier et les endroits où quelque chose cloche avant qu'on t'appelle.", requiresPower = true, investigation = 2, prestige = 1, influence = 1, identityExposure = 1),
        AnnualActionCard("meta_control", "Travailler la maîtrise du pouvoir", "Répéter les gestes simples, réduire les écarts et rendre l'extraordinaire reproductible.", AnnualActionCategory.TRAINING, "alt_04", "Discipline · Maîtrise", "Le pouvoir répond un peu moins comme un accident et un peu plus comme une compétence.", requiresPower = true, discipline = 3, control = 3),
        AnnualActionCard("meta_limits", "Tester tes limites", "Pousser un peu plus loin, puis apprendre où t'arrêter avant de payer l'expérience trop cher.", AnnualActionCategory.TRAINING, "power_energy", "Discipline · Puissance", "Tu trouves une marge supplémentaire, avec le rappel que chaque marge a un prix.", requiresPower = true, discipline = 2, power = 2, health = -1),
        AnnualActionCard("meta_watch_faction", "Observer une faction", "Pas d'infiltration grandiose : suivre des habitudes, recouper des noms, comprendre leurs priorités.", AnnualActionCategory.INVESTIGATION, "alt_06", "Enquête", "Tu obtiens surtout du contexte, souvent plus utile qu'une confrontation.", requiresPower = true, investigation = 4),
        AnnualActionCard("meta_family", "Réserver du temps aux proches", "Refuser qu'une vie métahumaine mange automatiquement toute la vie d'avant.", AnnualActionCategory.RELATION, "relation_family", "Liens", "Pendant quelques heures, tu n'es ni une menace ni un symbole. Juste quelqu'un qu'ils connaissent.", requiresPower = true, familyBond = 5, presence = 1),
        AnnualActionCard("meta_physio", "Faire de la récupération", "Soins, mobilité, sommeil, rééducation : prendre les blessures au sérieux avant qu'elles décident à ta place.", AnnualActionCategory.RECOVERY, "alt_07", "Santé · Discipline", "Tu traites ton corps comme quelque chose à préserver, pas comme une ressource infinie.", requiresPower = true, health = 6, discipline = 1),
        AnnualActionCard("meta_charity", "Participer à une action publique", "Être présent sans crise : association, collecte, rencontre locale ou soutien visible.", AnnualActionCategory.PUBLIC, "relation_media", "Présence · Opinion", "Le public te voit faire autre chose que réagir à une catastrophe.", requiresPower = true, presence = 4, opinion = 3, prestige = 1, identityExposure = 2),
        AnnualActionCard("meta_media_silence", "Disparaître des radars un moment", "Refuser les demandes, éviter les caméras et laisser l'actualité respirer sans toi.", AnnualActionCategory.PUBLIC, "public_fear", "Présence · Identité", "Ton absence devient elle aussi un message, mais au moins ton visage circule un peu moins.", requiresPower = true, presence = 2, identityExposure = -3, mediaStanding = -1),
        AnnualActionCard("meta_cleanup", "Aider après une intervention", "Déblayer, sécuriser, orienter les habitants. La partie qui n'apparaît presque jamais dans les images héroïques.", AnnualActionCategory.INTERVENTION, "scope_city", "Secours · Présence", "Tu restes après l'effet spectaculaire, quand le travail le moins photogénique commence.", requiresPower = true, rescue = 2, presence = 2, opinion = 2),
        AnnualActionCard("meta_night_call", "Répondre à un appel nocturne", "Une alerte locale, peu d'informations et personne de certain que ça mérite ton niveau d'attention.", AnnualActionCategory.INTERVENTION, "scope_street", "Secours · Enquête", "Ce n'était pas la fin du monde. Pour quelqu'un sur place, c'était suffisant.", requiresPower = true, rescue = 2, investigation = 2),
        AnnualActionCard("meta_tip", "Vérifier un signalement", "Une information trop mince pour devenir un arc, mais assez crédible pour mériter une heure.", AnnualActionCategory.INVESTIGATION, "alt_03", "Enquête", "La plupart des pistes meurent vite. Celle-ci t'apprend tout de même à mieux filtrer le bruit.", requiresPower = true, investigation = 3, governmentStanding = 1)
    )

    fun available(c: Campaign, rawState: AnnualActionState): List<AnnualActionCard> {
        val state = rawState.synced(c)
        if (state.remaining <= 0) return emptyList()
        // The awakening itself remains a singular story beat: no errands are inserted inside it.
        if (!c.powerRevealed && c.turn >= 10) return emptyList()
        val eligible = (if (c.powerRevealed) civilian + metahuman else civilian)
            .filterNot { it.id in state.usedIds }
        if (!c.powerRevealed) return deterministicOrder(eligible, c).take(8)
        return deterministicOrder(eligible, c).take(10)
    }

    fun perform(c: Campaign, rawState: AnnualActionState, card: AnnualActionCard): AnnualActionResult? {
        val state = rawState.synced(c)
        if (state.remaining <= 0 || card.id in state.usedIds) return null
        if (card.requiresPower && !c.powerRevealed) return null
        if (!c.powerRevealed && c.turn >= 10) return null

        var riskNote = ""
        var healthDelta = card.health
        when (card.id) {
            "meta_collapse" -> if (state.rescue < 25 || c.control < 30) {
                healthDelta -= 3
                riskNote = " La structure te rappelle brutalement que l'expérience compte : tu repars aussi avec quelques blessures."
            }
            "meta_fire" -> if (state.discipline < 20 || c.control < 35) {
                healthDelta -= 2
                riskNote = " Ton manque de routine te coûte un peu physiquement, sans annuler ce que tu as réussi à faire."
            }
        }

        val nextState = state.copy(
            used = (state.used + 1).coerceAtMost(ANNUAL_ACTION_LIMIT),
            rescue = (state.rescue + card.rescue).coerceIn(0, 100),
            investigation = (state.investigation + card.investigation).coerceIn(0, 100),
            presence = (state.presence + card.presence).coerceIn(0, 100),
            discipline = (state.discipline + card.discipline).coerceIn(0, 100),
            usedIds = state.usedIds + card.id
        )

        var nextCampaign = c.copy(
            health = (c.health + healthDelta).coerceIn(0, 100),
            familyBond = (c.familyBond + card.familyBond).coerceIn(0, 100),
            prestige = (c.prestige + card.prestige).coerceIn(0, 100),
            opinion = (c.opinion + card.opinion).coerceIn(-100, 100),
            fear = (c.fear + card.fear).coerceIn(0, 100),
            power = (c.power + if (c.powerRevealed) card.power else 0).coerceIn(0, 100),
            control = (c.control + if (c.powerRevealed) card.control else 0).coerceIn(0, 100),
            influence = (c.influence + if (c.powerRevealed) card.influence else 0).coerceAtLeast(0),
            identityExposure = (c.identityExposure + if (c.powerRevealed) card.identityExposure else 0).coerceIn(0, 100),
            factionStanding = (c.factionStanding + if (c.powerRevealed) card.factionStanding else 0).coerceIn(-100, 100),
            governmentStanding = (c.governmentStanding + if (c.powerRevealed) card.governmentStanding else 0).coerceIn(-100, 100),
            mediaStanding = (c.mediaStanding + if (c.powerRevealed) card.mediaStanding else 0).coerceIn(-100, 100)
        )
        if (card.id == "civil_barber") {
            nextCampaign = nextCampaign.copy(visualStyle = "Style civil rafraîchi")
        }
        val note = "${c.age} ans — Interlude : ${card.title}."
        nextCampaign = nextCampaign.copy(timeline = (nextCampaign.timeline + note).takeLast(180))

        return AnnualActionResult(
            campaign = nextCampaign,
            state = nextState,
            title = card.title,
            text = card.outcome + riskNote
        )
    }

    private fun deterministicOrder(cards: List<AnnualActionCard>, c: Campaign): List<AnnualActionCard> {
        val random = Random(c.seed xor ((c.turn + 1L) * 0x5DEECE66DL))
        return cards.map { it to random.nextLong() }.sortedBy { it.second }.map { it.first }
    }
}

@Composable
internal fun ProductionAnnualActionsScreen(
    c: Campaign,
    state: AnnualActionState,
    onAction: (AnnualActionCard) -> AnnualActionResult?,
    onBack: () -> Unit
) {
    val synced = state.synced(c)
    val haptics = LocalHapticFeedback.current
    var result by remember(c.seed, c.turn) { mutableStateOf<AnnualActionResult?>(null) }
    val available = remember(c.seed, c.turn, c.powerRevealed, synced.usedIds, synced.used) {
        AnnualActionEngine.available(c, synced)
    }
    val categories = remember(available) { available.map { it.category }.distinct() }
    var filter by remember(c.turn) { mutableStateOf<AnnualActionCategory?>(null) }
    val shown = available.filter { filter == null || it.category == filter }

    MhlSceneFrame("annual-actions-${c.seed}-${c.turn}-${synced.used}", MotionBoard.PANEL_TRANSITION, MetahumanMotionLevel.MOTION_STANDARD, Modifier.fillMaxSize(), MetahumanColors.ElectricBlue) {
        Column(Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AGIR CETTE ANNÉE", color = MetahumanColors.Gold, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text("${c.age} ANS · ${synced.remaining}/$ANNUAL_ACTION_LIMIT MOMENTS LIBRES", color = MetahumanColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                TextButtonCompat("RETOUR", onBack)
            }
            Spacer(Modifier.height(8.dp))
            MhlComicPanel(accent = MetahumanColors.DeepBlue) {
                Text("LE TEMPS ENTRE LES GRANDES DÉCISIONS", fontWeight = FontWeight.Black)
                Text("Ces actions ne font pas passer l'année. Elles développent des aptitudes visibles, entretiennent tes liens ou te permettent de petites interventions. La grande décision du Destin reste la seule qui fait avancer ta vie.", color = MetahumanColors.Muted, lineHeight = 20.sp)
                if (!c.powerRevealed) {
                    Spacer(Modifier.height(6.dp))
                    Text("Avant l'éveil, ces activités restent strictement civiles et ne touchent jamais aux calculs cachés qui déterminent ton futur pouvoir.", color = MetahumanColors.Gold, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            AnnualSkillStrip(synced)

            result?.let {
                Spacer(Modifier.height(10.dp))
                MhlComicPanel(accent = MetahumanColors.Green, fill = Color(0xE511201B)) {
                    Text(it.title.uppercase(), color = MetahumanColors.Green, fontWeight = FontWeight.Black)
                    Text(it.text, color = MetahumanColors.Ivory, lineHeight = 20.sp)
                    Text("Il te reste ${it.state.remaining} moment${if (it.state.remaining == 1) "" else "s"} libre${if (it.state.remaining == 1) "" else "s"} cette année.", color = MetahumanColors.Muted, fontSize = 11.sp)
                }
            }

            if (!c.powerRevealed && c.turn >= 10) {
                Spacer(Modifier.height(14.dp))
                MhlComicPanel(accent = MetahumanColors.Violet) {
                    Text("QUELQUE CHOSE EST SUR LE POINT D'ARRIVER", color = MetahumanColors.Violet, fontWeight = FontWeight.Black)
                    Text("Cette période n'accepte pas d'interlude. Reviens au Destin.", color = MetahumanColors.Muted)
                }
                return@Column
            }
            if (synced.remaining <= 0) {
                Spacer(Modifier.height(14.dp))
                MhlComicPanel(accent = MetahumanColors.Gold) {
                    Text("ANNÉE BIEN REMPLIE", color = MetahumanColors.Gold, fontWeight = FontWeight.Black)
                    Text("Tes trois moments libres sont utilisés. La prochaine grande décision fera avancer le temps et rouvrira de nouvelles possibilités.", color = MetahumanColors.Muted, lineHeight = 20.sp)
                }
                return@Column
            }

            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("TOUT") })
                categories.forEach { category ->
                    FilterChip(selected = filter == category, onClick = { filter = category }, label = { Text(category.label) })
                }
            }
            Spacer(Modifier.height(10.dp))
            shown.forEach { card ->
                AnnualActionCardView(card) {
                    val resolved = onAction(card)
                    if (resolved != null) {
                        result = resolved
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AnnualSkillStrip(state: AnnualActionState) {
    MhlComicPanel(accent = MetahumanColors.ElectricBlue, fill = Color(0xE5161E29)) {
        Text("APTITUDES", color = MetahumanColors.ElectricBlue, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
        AnnualSkillLine("Secours", state.rescue, MetahumanColors.Green)
        AnnualSkillLine("Enquête", state.investigation, MetahumanColors.ElectricBlue)
        AnnualSkillLine("Présence", state.presence, MetahumanColors.Gold)
        AnnualSkillLine("Discipline", state.discipline, MetahumanColors.Violet)
    }
}

@Composable
private fun AnnualSkillLine(label: String, value: Int, accent: Color) {
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), color = MetahumanColors.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(78.dp))
        LinearProgressIndicator(progress = { value.coerceIn(0, 100) / 100f }, modifier = Modifier.weight(1f).height(4.dp), color = accent, trackColor = Color(0xFF29313A))
        Spacer(Modifier.width(8.dp))
        Text(value.toString(), color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.width(26.dp))
    }
}

@Composable
private fun AnnualActionCardView(card: AnnualActionCard, onClick: () -> Unit) {
    MhlComicPanel(accent = annualAccent(card.category), fill = Color(0xF5161D27)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MhlProductionAsset(card.iconKey, card.title, size = 56.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(card.category.label.uppercase(), color = annualAccent(card.category), fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = .8.sp)
                Text(card.title, color = MetahumanColors.Ivory, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(card.description, color = MetahumanColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text("DÉVELOPPE · ${card.focus.uppercase()}", color = MetahumanColors.Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        MhlPrimaryButton("Faire cette action", onClick, Modifier.fillMaxWidth())
    }
}

@Composable
private fun TextButtonCompat(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}

private fun annualAccent(category: AnnualActionCategory): Color = when (category) {
    AnnualActionCategory.CIVIL -> Color(0xFF718092)
    AnnualActionCategory.INTERVENTION -> MetahumanColors.Red
    AnnualActionCategory.TRAINING -> MetahumanColors.Violet
    AnnualActionCategory.INVESTIGATION -> MetahumanColors.ElectricBlue
    AnnualActionCategory.RELATION -> MetahumanColors.Gold
    AnnualActionCategory.RECOVERY -> MetahumanColors.Green
    AnnualActionCategory.PUBLIC -> MetahumanColors.WarmGold
}
