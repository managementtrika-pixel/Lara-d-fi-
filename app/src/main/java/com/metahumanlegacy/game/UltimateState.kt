package com.metahumanlegacy.game

import android.content.Context
import android.net.Uri
import java.util.Random

/**
 * Persistent state for the expanded life-simulation layer.
 * Campaign stays the authoritative deterministic narrative state; this store carries the richer
 * personalization/world biography while Campaign storage migrates legacy chronology explicitly.
 */
internal data class UltimateCreationDraft(
    val blueprint: CharacterBlueprint,
    val bodyBuild: String = "Athlétique",
    val stature: String = "Moyenne",
    val skinTone: String = "Moyen",
    val faceShape: String = "Ovale",
    val hair: String = "Court texturé",
    val hairColor: String = "Brun",
    val facialHair: String = "Aucune",
    val eyes: String = "Bruns",
    val civilianStyle: String = "Street sobre",
    val accessory: String = "Aucun",
    val cityArchetype: String = "Métropole verticale",
    val climate: String = "Quatre saisons",
    val architecture: String = "Contemporaine",
    val cityMood: String = "Contrastes sociaux",
    val libraryFaceIndex: Int = -1
)

internal data class UltimateRelation(
    val id: String,
    val name: String,
    val role: String,
    val ageOffset: Int = 0,
    val trust: Int = 50,
    val affection: Int = 50,
    val fear: Int = 0,
    val admiration: Int = 0,
    val grudge: Int = 0,
    val dependence: Int = 0,
    val status: String = "Présent",
    val knowsIdentity: Boolean = false,
    val lastSeenTurn: Int = 0
)

internal data class UltimateCase(
    val id: String,
    val title: String,
    val stage: Int = 0,
    val maxStage: Int = 4,
    val evidence: Int = 0,
    val reliability: Int = 50,
    val falseLead: Boolean = false,
    val solved: Boolean = false,
    val openedTurn: Int = 0
)

internal data class UltimateDistrict(
    val name: String,
    val sentiment: Int = 0,
    val damage: Int = 0,
    val crime: Int = 35,
    val reconstruction: Int = 0,
    val faction: String = "Aucune",
    val landmark: String = "",
    val restricted: Boolean = false
)

internal data class UltimateState(
    val seed: Long,
    val bodyBuild: String,
    val stature: String,
    val skinTone: String,
    val faceShape: String,
    val hair: String,
    val hairColor: String,
    val facialHair: String,
    val eyes: String,
    val civilianStyle: String,
    val accessory: String,
    val cityArchetype: String,
    val climate: String,
    val architecture: String,
    val cityMood: String,
    val heroPresentation: String = "À découvrir",
    val costumePalette: String = "Non définie",
    val maskStyle: String = "Aucun",
    val emblem: String = "Aucun",
    val signatureItem: String = "Aucun",
    val costumeEra: Int = 0,
    val baseStage: Int = 0,
    val baseType: String = "Logement civil",
    val credits: Int = 1800,
    val debt: Int = 0,
    val incomeTier: Int = 1,
    val sponsor: String = "Aucun",
    val legalStatus: String = "Civil",
    val mediaFrame: String = "Inconnu",
    val journalist: String = "",
    val mentor: String = "",
    val protege: String = "",
    val romance: String = "",
    val nemesis: String = "",
    val nemesisAdaptation: Int = 0,
    val combatStyle: String = "Instinctif",
    val powerBranch: String = "Non spécialisée",
    val powerStrain: Int = 0,
    val internationalAttention: Int = 0,
    val metaLaw: String = "Aucun cadre spécifique",
    val cityCondition: Int = 72,
    val cityTech: Int = 20,
    val generationOpinion: Int = 0,
    val mysteryStage: Int = 0,
    val retirementIntent: String = "Indécis",
    val relations: List<UltimateRelation> = emptyList(),
    val cases: List<UltimateCase> = emptyList(),
    val districts: List<UltimateDistrict> = emptyList(),
    val techniques: List<String> = emptyList(),
    val injuries: List<String> = emptyList(),
    val iconicItems: List<String> = emptyList(),
    val rareMarks: List<String> = emptyList(),
    val memories: List<String> = emptyList(),
    val snapshots: List<String> = emptyList(),
    val lastProcessedTurn: Int = -1,
    val libraryFaceIndex: Int = -1
) {
    fun relation(id: String): UltimateRelation? = relations.firstOrNull { it.id == id }
    fun district(name: String): UltimateDistrict? = districts.firstOrNull { it.name == name }

    fun ageAppearance(c: Campaign): String = when {
        c.age < 13 -> "Traits d'enfance"
        c.age < 18 -> "Traits adolescents"
        c.age < 25 -> "Jeune adulte"
        c.age < 35 -> "Traits adultes"
        c.age < 50 -> "Traits affirmés"
        c.age < 65 -> "Traits marqués"
        else -> "Traits vétérans"
    }

    fun homeLabel(): String = when (baseStage) {
        0 -> "Logement civil"
        1 -> "Logement sécurisé"
        2 -> "Refuge improvisé"
        3 -> "QG opérationnel"
        else -> "Sanctuaire de légende"
    }
}

internal object UltimateCatalog {
    val bodyBuilds = listOf("Fin", "Athlétique", "Massif", "Souple", "Robuste")
    val statures = listOf("Petite", "Moyenne", "Grande")
    val skinTones = listOf("Très clair", "Clair", "Moyen", "Mat", "Foncé", "Très foncé")
    val faceShapes = listOf("Ovale", "Carré", "Fin", "Rond", "Anguleux")
    val hairs = listOf("Court texturé", "Dégradé", "Boucles", "Tresses", "Long", "Rasé", "Undercut", "Attaché")
    val hairColors = listOf("Noir", "Brun", "Châtain", "Blond", "Roux", "Gris", "Blanc")
    val facialHairs = listOf("Aucune", "Barbe courte", "Barbe pleine", "Moustache", "Bouc")
    val eyes = listOf("Bruns", "Noisette", "Verts", "Bleus", "Gris", "Très sombres")
    val civilianStyles = listOf("Street sobre", "Sportif", "Classique", "Créatif", "Minimal", "Professionnel", "Vintage")
    val accessories = listOf("Aucun", "Lunettes", "Boucles", "Chaîne", "Montre", "Bonnet", "Casquette")

    val cityArchetypes = listOf("Métropole verticale", "Ville côtière", "Cité industrielle", "Capitale ancienne", "Ville universitaire", "Mégalopole technologique", "Ville en reconstruction")
    val climates = listOf("Quatre saisons", "Pluvieux", "Brouillard côtier", "Hiver rude", "Chaud et sec", "Orageux", "Pollué")
    val architectures = listOf("Contemporaine", "Art déco", "Brique industrielle", "Néo-classique", "Brutaliste", "Futur proche", "Mixte historique")
    val cityMoods = listOf("Contrastes sociaux", "Optimiste", "Sous tension", "Nocturne", "Ultra-connectée", "Méfiance métahumaine", "Culture héroïque")

    val heroPresentations = listOf("Sobre", "Tactique", "Flamboyant", "Intimidant", "Institutionnel", "Clandestin", "Mystérieux")
    val costumePalettes = listOf("Bleu / or", "Noir / argent", "Rouge / anthracite", "Blanc / cobalt", "Violet / noir", "Vert / cuivre", "Ivoire / or", "Personnalisée au pouvoir")
    val maskStyles = listOf("Aucun", "Demi-masque", "Masque intégral", "Visière", "Capuche", "Casque", "Masque minimal")
    val emblems = listOf("Étoile fracturée", "Œil stylisé", "Éclair", "Anneau", "Bouclier", "Flèche", "Comète", "Monogramme")
    val signatureItems = listOf("Aucun", "Manteau", "Gants", "Lunettes", "Capuche", "Bracelet", "Pendentif", "Ceinture technique")
    val baseTypes = listOf("Atelier", "Centre de surveillance", "Refuge", "Laboratoire", "Bunker", "Sanctuaire")

    fun randomDraft(seed: Long, blueprint: CharacterBlueprint): UltimateCreationDraft {
        val r = Random(seed xor 0x61C8864680B583EBL)
        fun <T> pick(list: List<T>) = list[r.nextInt(list.size)]
        return UltimateCreationDraft(
            blueprint = blueprint,
            bodyBuild = pick(bodyBuilds), stature = pick(statures), skinTone = pick(skinTones),
            faceShape = pick(faceShapes), hair = pick(hairs), hairColor = pick(hairColors),
            facialHair = pick(facialHairs), eyes = pick(eyes), civilianStyle = pick(civilianStyles),
            accessory = pick(accessories), cityArchetype = pick(cityArchetypes), climate = pick(climates),
            architecture = pick(architectures), cityMood = pick(cityMoods)
        )
    }
}

internal object UltimateStore {
    private const val PREFS = "mhl_ultimate_state_v1"

    fun create(c: Campaign, draft: UltimateCreationDraft): UltimateState {
        val r = Random(c.seed xor 0x6A09E667F3BCC909L)
        val first = listOf("Jessa", "Marek", "Nia", "Tomas", "Samira", "Ilan", "Noa", "Darius", "Mina", "Eden", "Kiran", "Sol").shuffled(r)
        val relations = listOf(
            UltimateRelation("family", first[0], "Famille", ageOffset = 24, trust = c.familyBond, affection = c.familyBond + 5),
            UltimateRelation("friend", first[1], "Ami·e", ageOffset = 0, trust = 58, affection = 62),
            UltimateRelation("journalist", first[2], "Journaliste local potentiel", ageOffset = 18, trust = 35, admiration = 10),
            UltimateRelation("rival", first[3], "Camarade / rival potentiel", ageOffset = 1, trust = 15, grudge = 15),
            UltimateRelation("mentor", first[4], "Mentor potentiel", ageOffset = 20, trust = 45, admiration = 20),
            UltimateRelation("peer", first[5], "Camarade métahumain potentiel", ageOffset = 2, trust = 40, admiration = 10)
        )
        val districts = GameEngine.districts.take(6).mapIndexed { i, d ->
            UltimateDistrict(
                name = d,
                sentiment = if (d == c.district) 12 else r.nextInt(15) - 7,
                damage = r.nextInt(8),
                crime = (26 + r.nextInt(25) + if (i == 1) 10 else 0).coerceIn(10, 80)
            )
        }
        return UltimateState(
            seed = c.seed,
            bodyBuild = draft.bodyBuild, stature = draft.stature, skinTone = draft.skinTone,
            faceShape = draft.faceShape, hair = draft.hair, hairColor = draft.hairColor,
            facialHair = draft.facialHair, eyes = draft.eyes, civilianStyle = draft.civilianStyle,
            accessory = draft.accessory, cityArchetype = draft.cityArchetype, climate = draft.climate,
            architecture = draft.architecture, cityMood = draft.cityMood,
            libraryFaceIndex = -1,
            journalist = relations.first { it.id == "journalist" }.name,
            relations = relations,
            districts = districts,
            snapshots = listOf("8|Enfance|${draft.hair}|${draft.civilianStyle}")
        )
    }

    fun fallback(c: Campaign): UltimateState {
        val draft = UltimateCatalog.randomDraft(c.seed, CharacterBlueprint(
            firstName = c.name.substringBefore(' '),
            lastName = c.name.substringAfter(' ', ""),
            pronouns = c.pronouns,
            city = c.city,
            district = c.district,
            socialBackground = c.socialBackground,
            motivation = c.motivation,
            civilianPath = c.civilianPath,
            temperament = c.temperament
        ))
        return create(c, draft)
    }

    fun load(context: Context, c: Campaign): UltimateState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(c.seed.toString(), null)
        return (raw?.let(::decode)?.takeIf { it.seed == c.seed } ?: fallback(c))
            .copy(libraryFaceIndex = -1)
    }

    fun save(context: Context, state: UltimateState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(state.seed.toString(), encode(state)).apply()
    }

    fun clear(context: Context, seed: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(seed.toString()).apply()
    }

    private fun encode(state: UltimateState): String {
        fun e(v: Any) = Uri.encode(v.toString())
        fun list(v: List<String>) = v.joinToString("~") { Uri.encode(it) }
        fun relations(v: List<UltimateRelation>) = v.joinToString("~") { r ->
            listOf(r.id, r.name, r.role, r.ageOffset, r.trust, r.affection, r.fear, r.admiration, r.grudge, r.dependence, r.status, r.knowsIdentity, r.lastSeenTurn)
                .joinToString(",") { Uri.encode(it.toString()) }
        }
        fun cases(v: List<UltimateCase>) = v.joinToString("~") { x ->
            listOf(x.id, x.title, x.stage, x.maxStage, x.evidence, x.reliability, x.falseLead, x.solved, x.openedTurn)
                .joinToString(",") { Uri.encode(it.toString()) }
        }
        fun districts(v: List<UltimateDistrict>) = v.joinToString("~") { d ->
            listOf(d.name, d.sentiment, d.damage, d.crime, d.reconstruction, d.faction, d.landmark, d.restricted)
                .joinToString(",") { Uri.encode(it.toString()) }
        }
        val scalar = listOf(
            state.seed, state.bodyBuild, state.stature, state.skinTone, state.faceShape, state.hair,
            state.hairColor, state.facialHair, state.eyes, state.civilianStyle, state.accessory,
            state.cityArchetype, state.climate, state.architecture, state.cityMood,
            state.heroPresentation, state.costumePalette, state.maskStyle, state.emblem, state.signatureItem,
            state.costumeEra, state.baseStage, state.baseType, state.credits, state.debt, state.incomeTier,
            state.sponsor, state.legalStatus, state.mediaFrame, state.journalist, state.mentor, state.protege,
            state.romance, state.nemesis, state.nemesisAdaptation, state.combatStyle, state.powerBranch,
            state.powerStrain, state.internationalAttention, state.metaLaw, state.cityCondition,
            state.cityTech, state.generationOpinion, state.mysteryStage, state.retirementIntent,
            state.lastProcessedTurn
        ).joinToString("|") { e(it) }
        return "U1|$scalar|${e(relations(state.relations))}|${e(cases(state.cases))}|${e(districts(state.districts))}|${e(list(state.techniques))}|${e(list(state.injuries))}|${e(list(state.iconicItems))}|${e(list(state.rareMarks))}|${e(list(state.memories))}|${e(list(state.snapshots))}|${e(state.libraryFaceIndex)}"
    }

    private fun decode(raw: String): UltimateState? = runCatching {
        if (!raw.startsWith("U1|")) return@runCatching null
        val p = raw.removePrefix("U1|").split('|').map(Uri::decode)
        var i = 0
        fun s() = p.getOrElse(i++) { "" }
        fun n(default: Int = 0) = s().toIntOrNull() ?: default
        fun b(value: String) = value.toBooleanStrictOrNull() ?: false
        val seed = s().toLong()
        val bodyBuild = s(); val stature = s(); val skinTone = s(); val faceShape = s(); val hair = s()
        val hairColor = s(); val facialHair = s(); val eyes = s(); val civilianStyle = s(); val accessory = s()
        val cityArchetype = s(); val climate = s(); val architecture = s(); val cityMood = s()
        val heroPresentation = s(); val costumePalette = s(); val maskStyle = s(); val emblem = s(); val signatureItem = s()
        val costumeEra = n(); val baseStage = n(); val baseType = s(); val credits = n(1800); val debt = n(); val incomeTier = n(1)
        val sponsor = s(); val legalStatus = s(); val mediaFrame = s(); val journalist = s(); val mentor = s(); val protege = s()
        val romance = s(); val nemesis = s(); val nemesisAdaptation = n(); val combatStyle = s(); val powerBranch = s()
        val powerStrain = n(); val internationalAttention = n(); val metaLaw = s(); val cityCondition = n(72)
        val cityTech = n(20); val generationOpinion = n(); val mysteryStage = n(); val retirementIntent = s(); val lastProcessedTurn = n(-1)

        fun decodeList(rawList: String): List<String> = rawList.split('~').filter { it.isNotBlank() }.map(Uri::decode)
        fun decodeRelations(rawList: String): List<UltimateRelation> = rawList.split('~').filter { it.isNotBlank() }.mapNotNull { item ->
            val q = item.split(',').map(Uri::decode)
            if (q.size < 13) null else UltimateRelation(q[0], q[1], q[2], q[3].toInt(), q[4].toInt(), q[5].toInt(), q[6].toInt(), q[7].toInt(), q[8].toInt(), q[9].toInt(), q[10], b(q[11]), q[12].toInt())
        }
        fun decodeCases(rawList: String): List<UltimateCase> = rawList.split('~').filter { it.isNotBlank() }.mapNotNull { item ->
            val q = item.split(',').map(Uri::decode)
            if (q.size < 9) null else UltimateCase(q[0], q[1], q[2].toInt(), q[3].toInt(), q[4].toInt(), q[5].toInt(), b(q[6]), b(q[7]), q[8].toInt())
        }
        fun decodeDistricts(rawList: String): List<UltimateDistrict> = rawList.split('~').filter { it.isNotBlank() }.mapNotNull { item ->
            val q = item.split(',').map(Uri::decode)
            if (q.size < 8) null else UltimateDistrict(q[0], q[1].toInt(), q[2].toInt(), q[3].toInt(), q[4].toInt(), q[5], q[6], b(q[7]))
        }

        val relations = decodeRelations(s())
        val cases = decodeCases(s())
        val districts = decodeDistricts(s())
        val techniques = decodeList(s()); val injuries = decodeList(s()); val iconicItems = decodeList(s())
        val rareMarks = decodeList(s()); val memories = decodeList(s()); val snapshots = decodeList(s())
        val libraryFaceIndex = s().toIntOrNull() ?: -1

        UltimateState(
            seed, bodyBuild, stature, skinTone, faceShape, hair, hairColor, facialHair, eyes,
            civilianStyle, accessory, cityArchetype, climate, architecture, cityMood,
            heroPresentation, costumePalette, maskStyle, emblem, signatureItem, costumeEra, baseStage,
            baseType, credits, debt, incomeTier, sponsor, legalStatus, mediaFrame, journalist, mentor,
            protege, romance, nemesis, nemesisAdaptation, combatStyle, powerBranch, powerStrain,
            internationalAttention, metaLaw, cityCondition, cityTech, generationOpinion, mysteryStage,
            retirementIntent, relations, cases, districts, techniques, injuries, iconicItems, rareMarks,
            memories, snapshots, lastProcessedTurn, libraryFaceIndex = libraryFaceIndex
        )
    }.getOrNull()
}

internal fun UltimateState.replaceRelation(next: UltimateRelation): UltimateState =
    copy(relations = relations.filterNot { it.id == next.id } + next)

internal fun UltimateState.replaceDistrict(next: UltimateDistrict): UltimateState =
    copy(districts = districts.map { if (it.name == next.name) next else it })

internal fun UltimateRelation.clamped(): UltimateRelation = copy(
    trust = trust.coerceIn(0, 100), affection = affection.coerceIn(0, 100), fear = fear.coerceIn(0, 100),
    admiration = admiration.coerceIn(0, 100), grudge = grudge.coerceIn(0, 100), dependence = dependence.coerceIn(0, 100)
)
