package com.metahumanlegacy.game

internal object ScopeEscalationDirector {
    private const val FLAG_PREFIX = "scope_response_"

    fun enrich(c: Campaign, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind in setOf("FORMATIVE", "AWAKENING", "ENDING")) return base
        if (base.choices.any { it.flag?.startsWith(FLAG_PREFIX) == true }) return base

        val protected = base.choices.filter { isProtectedChoice(it) }
        val ordinary = base.choices.filterNot { isProtectedChoice(it) }
        val retained = ordinary.take((6 - protected.size).coerceAtLeast(0)) + protected
        return base.copy(
            text = base.text + "\n\nÉCHELLE ${c.scope.label.uppercase()}\n${contextFor(c.scope, c)}",
            choices = (retained + responseFor(c.scope, base)).distinctBy { it.label }.take(7)
        )
    }

    private fun isProtectedChoice(choice: Choice): Boolean =
        choice.flag?.startsWith("identity_pressure:") == true ||
            choice.flag?.startsWith("story_technique:") == true ||
            choice.flag?.startsWith(FLAG_PREFIX) == true

    private fun responseFor(scope: Scope, event: EventNode): Choice = when (scope) {
        Scope.STREET -> Choice("Rester au contact du terrain et résoudre d'abord ce qui est devant toi", moral = 1, opinion = 1, impact = 1, risk = event.stakes.coerceAtLeast(1), approach = "CARE", stakes = event.stakes, sourceCategory = event.category, relationDelta = 1, identityDelta = 1, flag = "${FLAG_PREFIX}street")
        Scope.DISTRICT -> Choice("Mobiliser les relais du quartier au lieu d'agir seul", moral = 1, opinion = 1, prestige = 1, impact = 2, risk = event.stakes.coerceAtLeast(1), approach = "CARE", stakes = event.stakes, sourceCategory = event.category, relationDelta = 2, flag = "${FLAG_PREFIX}district")
        Scope.CITY -> Choice("Coordonner secours, accès et infrastructures à l'échelle de la ville", prestige = 2, opinion = 1, impact = 3, risk = (event.stakes - 1).coerceAtLeast(1), approach = "ORDER", stakes = event.stakes, sourceCategory = event.category, flag = "${FLAG_PREFIX}city")
        Scope.REGION -> Choice("Prioriser les zones menacées et déléguer ce que tu ne peux pas couvrir", moral = 1, prestige = 2, impact = 4, risk = event.stakes, approach = "ORDER", stakes = event.stakes, sourceCategory = event.category, relationDelta = 1, flag = "${FLAG_PREFIX}region")
        Scope.COUNTRY -> Choice("Partager le commandement avec le réseau national plutôt que tout centraliser", opinion = 2, prestige = 2, impact = 5, risk = event.stakes, approach = "TRUTH", stakes = event.stakes, sourceCategory = event.category, identityDelta = 1, flag = "${FLAG_PREFIX}country")
        Scope.WORLD -> Choice("Synchroniser plusieurs réponses internationales et accepter de ne pas être partout", moral = 1, opinion = 2, prestige = 3, impact = 6, risk = event.stakes.coerceAtLeast(2), approach = "CARE", stakes = event.stakes, sourceCategory = event.category, relationDelta = 1, identityDelta = 1, flag = "${FLAG_PREFIX}world")
    }

    private fun contextFor(scope: Scope, c: Campaign): String = when (scope) {
        Scope.STREET -> "À ce stade, une intervention se gagne encore visage par visage. Une mauvaise décision touche des gens que tu peux presque tous regarder dans les yeux."
        Scope.DISTRICT -> "${c.district} commence à compter sur ta présence. Les habitants, les témoins et les réseaux locaux peuvent désormais amplifier autant tes réussites que tes erreurs."
        Scope.CITY -> "${c.city} ne te traite plus comme un incident isolé. Les secours, les infrastructures, les médias et plusieurs quartiers réagissent désormais à tes décisions en même temps."
        Scope.REGION -> "Une seule scène peut maintenant déplacer des moyens loin d'une autre urgence. Choisir où intervenir devient aussi important que réussir l'intervention elle-même."
        Scope.COUNTRY -> "Tes décisions peuvent modifier des protocoles et la manière dont d'autres équipes réagissent. Une victoire locale peut créer un précédent beaucoup plus large."
        Scope.WORLD -> "Ton absence dans une zone devient elle aussi une décision. À cette échelle, gagner signifie coordonner, déléguer et accepter que personne ne puisse sauver chaque endroit seul."
    }
}
