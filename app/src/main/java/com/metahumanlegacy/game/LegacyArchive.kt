package com.metahumanlegacy.game

internal data class LegacyRecord(
    val name: String,
    val title: String,
    val score: Int,
    val scope: String,
    val city: String,
    val presentation: String,
    val nemesis: String,
    val powerFamily: String = "Non révélé",
    val morality: Int = 0,
    val opinion: Int = 0,
    val fear: Int = 0,
    val finalAge: Int = 0,
    val bodyBuild: String = "",
    val skinTone: String = "",
    val hair: String = "",
    val hairColor: String = "",
    val faceShape: String = "",
    val civilianStyle: String = "",
    val accessory: String = "",
    val identityId: String = "",
    val strongestRelation: String = "",
    val endingSummary: String = ""
) {
    fun encode(): String = listOf(
        name, title, score.toString(), scope, city, presentation, nemesis,
        powerFamily, morality.toString(), opinion.toString(), fear.toString(), finalAge.toString(),
        bodyBuild, skinTone, hair, hairColor, faceShape, civilianStyle, accessory,
        identityId, strongestRelation, endingSummary
    ).joinToString("|") { clean(it) }

    companion object {
        fun from(c: Campaign, s: UltimateState): LegacyRecord {
            val strongest = s.relations
                .maxByOrNull { it.trust + it.affection + it.admiration - it.grudge }
                ?.let { it.name + " · " + it.role }
                .orEmpty()
            return LegacyRecord(
                name = c.alias.ifBlank { c.name },
                title = GameEngine.legacyTitle(c),
                score = GameEngine.legacyScore(c),
                scope = c.scope.label,
                city = c.city,
                presentation = s.heroPresentation,
                nemesis = s.nemesis.ifBlank { "Aucune" },
                powerFamily = c.powerFamily,
                morality = c.morality,
                opinion = c.opinion,
                fear = c.fear,
                finalAge = c.age,
                bodyBuild = s.bodyBuild,
                skinTone = s.skinTone,
                hair = s.hair,
                hairColor = s.hairColor,
                faceShape = s.faceShape,
                civilianStyle = s.civilianStyle,
                accessory = s.accessory,
                identityId = stablePixelIdentityId(c, s),
                strongestRelation = strongest,
                endingSummary = UltimateDirector.legacySummary(c, s)
            )
        }

        fun decode(raw: String): LegacyRecord {
            val p = raw.split('|')
            fun s(i: Int, fallback: String = "") = p.getOrElse(i) { fallback }
            fun n(i: Int, fallback: Int = 0) = s(i).toIntOrNull() ?: fallback
            return LegacyRecord(
                name = s(0, "Inconnu"),
                title = s(1, "Legacy"),
                score = n(2),
                scope = s(3, "Rue"),
                city = s(4, "Ville inconnue"),
                presentation = s(5),
                nemesis = s(6, "Aucune"),
                powerFamily = s(7, "Non révélé"),
                morality = n(8),
                opinion = n(9),
                fear = n(10),
                finalAge = n(11),
                bodyBuild = s(12),
                skinTone = s(13),
                hair = s(14),
                hairColor = s(15),
                faceShape = s(16),
                civilianStyle = s(17),
                accessory = s(18),
                identityId = s(19),
                strongestRelation = s(20),
                endingSummary = s(21)
            )
        }

        private fun clean(value: String): String = value
            .replace('|', '·')
            .replace(";;", "—")
            .replace('\n', ' ')
            .trim()
    }
}

internal fun stablePixelIdentityId(c: Campaign, s: UltimateState): String {
    val source = listOf(
        c.name, s.skinTone, s.faceShape, s.bodyBuild, s.hair, s.hairColor,
        s.eyes, s.civilianStyle, s.accessory, c.city
    ).joinToString("|")
    val hex = source.hashCode().toUInt().toString(16).uppercase().takeLast(6).padStart(6, '0')
    return "ID-" + hex
}
