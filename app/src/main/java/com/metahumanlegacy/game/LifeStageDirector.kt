package com.metahumanlegacy.game

/** Adds age-specific decisions without replacing authored events. */
internal object LifeStageDirector {
    fun enrich(c: Campaign, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind in setOf("FORMATIVE", "AWAKENING", "ENDING")) return base
        val extra = mutableListOf<Choice>()
        val notes = mutableListOf<String>()

        if (c.age >= 35 && base.choices.size < 7 && c.turn % 9 == 0) {
            extra += Choice(
                label = "Faire une place à la génération qui arrive au lieu de tout porter seul",
                moral = 1, prestige = 1, impact = 1, risk = (base.stakes - 1).coerceAtLeast(1),
                approach = "CARE", stakes = base.stakes, sourceCategory = base.category,
                relationDelta = 2, flag = "V2_MENTOR_NEXT_GENERATION"
            )
            notes += "Tu as désormais assez d'expérience pour transmettre. Continuer à tout faire toi-même est aussi un choix."
        }

        if (c.age >= 58 && base.choices.size + extra.size < 7 && c.turn % 7 == 0) {
            extra += Choice(
                label = "Préparer sérieusement l'après : réseau, successeur et sortie de terrain",
                prestige = 1, opinion = 1, impact = 1, risk = 1,
                approach = "ORDER", stakes = base.stakes, sourceCategory = base.category,
                relationDelta = 2, flag = "V2_RETIREMENT_PATH"
            )
            notes += "La question n'est plus seulement de savoir si tu peux continuer, mais ce qui arrivera au monde quand tu arrêteras."
        }

        if (c.age >= 68 && "V2_RETIREMENT_PATH" in c.flags && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Accepter que cette crise puisse être la dernière que tu prends en charge",
                moral = 1, opinion = 1, prestige = 2, impact = 1, risk = 1,
                approach = "CARE", stakes = base.stakes, sourceCategory = base.category,
                relationDelta = 3, flag = "V2_RETIRE_NOW"
            )
            notes += "Tu peux encore intervenir. Pour la première fois, ne pas être l'unique réponse peut aussi être une victoire."
        }

        if (extra.isEmpty()) return base
        return base.copy(
            text = base.text + "\n\n" + notes.joinToString("\n"),
            choices = (base.choices + extra).distinctBy { it.label }.take(7)
        )
    }
}
