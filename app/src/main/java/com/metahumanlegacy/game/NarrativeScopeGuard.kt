package com.metahumanlegacy.game

/** Safety net for the authored arc selector: a fallback may repeat old content, but it must never
 * promote the player into an arc whose minimum career scope has not been earned yet. */
internal object NarrativeScopeGuard {
    fun enforce(c: Campaign, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind != "MAJOR") return base
        val beats = NarrativeCodec.beats()
        val selected = beats.firstOrNull { it.id == base.id } ?: return base
        if (c.scope.ordinal >= selected.minScope.ordinal) return base

        val eligible = beats.asSequence()
            .filter { it.stage == 1 }
            .filter { it.minAge <= c.age }
            .filter { c.scope.ordinal >= it.minScope.ordinal }
            .filter { it.requiresFlags.all(c.flags::contains) }
            .sortedWith(compareByDescending<MajorBeat> { it.minScope.ordinal }.thenByDescending { it.minAge }.thenBy { it.id })
            .toList()

        if (eligible.isNotEmpty()) {
            val index = positiveMod(mix(c.seed, c.turn * 733L + base.id.hashCode()), eligible.size)
            return NarrativeRepository.byId(eligible[index].id, c) ?: localFallback(c, base)
        }
        return localFallback(c, base)
    }

    private fun localFallback(c: Campaign, base: EventNode): EventNode = EventNode(
        id = "SCOPE_SAFE_${c.scope.name}_${c.turn}",
        title = "UNE URGENCE À TON ÉCHELLE",
        text = "L'affaire qui semblait devoir t'emporter plus loin dépasse encore ton réseau actuel. Une urgence plus proche réclame ta présence dans ${c.district}.",
        choices = listOf(
            Choice("Mettre les personnes en sécurité avant tout", moral = 1, opinion = 1, impact = 1, risk = base.stakes, approach = "CARE", stakes = base.stakes, sourceCategory = "CRISE"),
            Choice("Sécuriser la zone et répartir les rôles", prestige = 1, impact = 1, risk = base.stakes, approach = "ORDER", stakes = base.stakes, sourceCategory = "CRISE"),
            Choice("Comprendre ce qui a réellement déclenché l'urgence", opinion = 1, impact = 1, risk = base.stakes, approach = "TRUTH", stakes = base.stakes, sourceCategory = "CRISE", identityDelta = -1),
            Choice("Prendre l'initiative et imposer rapidement une issue", prestige = 1, fear = 1, power = 1, impact = 1, risk = base.stakes + 1, approach = "ASCEND", stakes = base.stakes, sourceCategory = "CRISE")
        ),
        category = "CRISE",
        provocation = "URGENCE LOCALE",
        stakes = base.stakes.coerceAtLeast(1),
        kind = "MAJOR"
    )
}
