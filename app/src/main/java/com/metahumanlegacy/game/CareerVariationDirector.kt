package com.metahumanlegacy.game

/**
 * Second-pass narrative selector that makes accumulated history mechanically useful.
 * It never replaces the authored event. It only adds a restrained callback option when the
 * player's own history earns one, and it actively discourages repetitive story texture.
 */
internal object CareerVariationDirector {
    private const val PREFIX = "deep:"

    fun enrich(c: Campaign, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind == "FORMATIVE" || base.kind == "AWAKENING") return base

        val extra = mutableListOf<Choice>()
        val notes = mutableListOf<String>()
        val memories = memories(c)
        val samePast = memories
            .filter { c.turn - it.turn >= 6 && it.category == safe(base.category) }
            .minByOrNull { it.turn }

        if (samePast != null && base.choices.size < 6) {
            extra += Choice(
                label = "Réutiliser ce que l'épisode de tes ${samePast.age} ans t'a appris",
                moral = if (samePast.approach == "CARE") 1 else 0,
                prestige = if (samePast.approach == "ORDER") 1 else 0,
                opinion = if (samePast.approach == "CARE") 1 else 0,
                fear = if (samePast.approach == "ASCEND") 1 else 0,
                power = if (samePast.approach == "ASCEND") 1 else 0,
                impact = 1,
                risk = (base.stakes - 1).coerceAtLeast(1),
                approach = samePast.approach,
                stakes = base.stakes,
                sourceCategory = base.category,
                relationDelta = if (samePast.approach == "CARE") 1 else 0,
                identityDelta = if (samePast.approach == "TRUTH") -1 else 0,
                flag = "depth_past_callback"
            )
            notes += "Cette situation ressemble assez à un épisode ancien pour que ton expérience personnelle ouvre une réponse qui n'existait pas autrefois."
        }

        val recent = value(c.flags, "recent").split(',').filter { it.isNotBlank() }
        val categoryRepeats = recent.takeLast(4).count { it == safe(base.category) }
        if (categoryRepeats >= 3 && base.choices.size + extra.size < 6) {
            val alternative = leastUsedApproach(c.flags)
            extra += variationChoice(alternative, base)
            notes += "Ta carrière tourne beaucoup autour du même type de conflit. Une manière moins habituelle de l'aborder devient possible avant que l'histoire ne se répète à l'identique."
        }

        // If the player has repeatedly used one approach, a Nemesis can eventually read it.
        val dominant = mostUsedApproach(c.flags)
        val dominantUses = int(c.flags, "use_${dominant.lowercase()}")
        val adaptation = int(c.flags, "nemesis_adaptation")
        if (base.category == "RIVAL" && dominantUses >= 5 && adaptation >= 5) {
            notes += "Ton rival connaît désormais assez bien ta tendance à ${approachPhrase(dominant)}. Répéter exactement le même schéma est devenu une information exploitable contre toi."
        }

        if (extra.isEmpty() && notes.isEmpty()) return base
        return base.copy(
            text = if (notes.isEmpty()) base.text else base.text + "\n\n" + notes.joinToString("\n"),
            choices = (base.choices + extra).distinctBy { it.label }.take(6)
        )
    }

    private data class Memory(val turn: Int, val age: Int, val category: String, val approach: String)

    private fun memories(c: Campaign): List<Memory> = c.flags
        .filter { it.startsWith("${PREFIX}memory=") }
        .mapNotNull { raw ->
            val p = raw.substringAfter('=').split(',')
            if (p.size < 5) null else Memory(
                turn = p[0].toIntOrNull() ?: return@mapNotNull null,
                age = p[1].toIntOrNull() ?: 18,
                category = p[3],
                approach = p[4]
            )
        }

    private fun variationChoice(approach: String, event: EventNode): Choice = when (approach) {
        "CARE" -> Choice(
            "Changer de rythme et commencer par les personnes concernées",
            moral = 1, opinion = 1, impact = 1, risk = event.stakes.coerceAtLeast(1),
            approach = "CARE", stakes = event.stakes, sourceCategory = event.category,
            relationDelta = 2, flag = "depth_variation_care"
        )
        "ORDER" -> Choice(
            "Arrêter d'improviser et poser un cadre avant d'agir",
            prestige = 1, impact = 1, risk = (event.stakes - 1).coerceAtLeast(1),
            approach = "ORDER", stakes = event.stakes, sourceCategory = event.category,
            flag = "depth_variation_order"
        )
        "TRUTH" -> Choice(
            "Refuser le scénario évident et chercher ce qui ne colle pas",
            opinion = 1, impact = 1, risk = event.stakes.coerceAtLeast(1),
            approach = "TRUTH", stakes = event.stakes, sourceCategory = event.category,
            identityDelta = -1, flag = "depth_variation_truth"
        )
        else -> Choice(
            "Prendre une initiative que ta carrière évite habituellement",
            prestige = 1, fear = 1, power = 1, impact = 1, risk = event.stakes + 1,
            approach = "ASCEND", stakes = event.stakes, sourceCategory = event.category,
            flag = "depth_variation_ascend"
        )
    }

    private fun leastUsedApproach(flags: Set<String>): String = listOf("CARE", "ORDER", "TRUTH", "ASCEND")
        .minByOrNull { int(flags, "code_${it.lowercase()}") } ?: "CARE"

    private fun mostUsedApproach(flags: Set<String>): String = listOf("CARE", "ORDER", "TRUTH", "ASCEND")
        .maxByOrNull { int(flags, "use_${it.lowercase()}") } ?: "CARE"

    private fun approachPhrase(approach: String) = when (approach) {
        "CARE" -> "protéger avant de poursuivre"
        "ORDER" -> "verrouiller la situation"
        "TRUTH" -> "chercher l'information cachée"
        "ASCEND" -> "prendre l'ascendant par la puissance"
        else -> "agir de la même manière"
    }

    private fun value(flags: Set<String>, key: String): String = flags
        .firstOrNull { it.startsWith("$PREFIX$key=") }
        ?.substringAfter('=') ?: ""

    private fun int(flags: Set<String>, key: String): Int = value(flags, key).toIntOrNull() ?: 0

    private fun safe(raw: String): String = raw.uppercase()
        .replace('É', 'E').replace('È', 'E').replace('Ê', 'E')
        .replace('À', 'A').replace('Â', 'A').replace('Ç', 'C')
        .replace('Ù', 'U').replace('Û', 'U').replace('Ô', 'O').replace('Î', 'I')
        .map { if (it.isLetterOrDigit() || it == '_' || it == '-') it else '_' }
        .joinToString("")
        .replace(Regex("_+"), "_").trim('_')
}
