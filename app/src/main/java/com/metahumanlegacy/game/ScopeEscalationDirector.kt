package com.metahumanlegacy.game

/**
 * Turns career reach into a playable responsibility, not only a badge on the profile.
 * Authored arcs remain authoritative; this layer adds one scope-specific way to respond and a
 * short reminder of what the current scale means for the people depending on the player.
 */
internal object ScopeEscalationDirector {
    private const val FLAG_PREFIX = "scope_response_"

    fun enrich(c: Campaign, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind in setOf("FORMATIVE", "AWAKENING", "ENDING")) return base
        if (base.choices.any { it.flag?.startsWith(FLAG_PREFIX) == true }) return base
        if (base.choices.size >= 7) return base

        val choice = responseFor(c.scope, base)
        val context = contextFor(c.scope, c)
        return base.copy(
            text = base.text + "\n\nÉCHELLE ${c.scope.label.uppercase()}\n$context",
            choices = (base.choices + choice).distinctBy { it.label }.take(7)
        )
    }

    private fun responseFor(scope: Scope, event: EventNode): Choice = when (scope) {
        Scope.STREET -> Choice(
            label = "Rester au contact du terrain et résoudre d'abord ce qui est devant toi",
            moral = 1,
            opinion = 1,
            impact = 1,
            risk = event.stakes.coerceAtLeast(1),
            approach = "CARE",
            stakes = event.stakes,
            sourceCategory = event.category,
            relationDelta = 1,
            identityDelta = 1,
            flag = "${FLAG_PREFIX}street"
        )
        Scope.DISTRICT -> Choice(
            label = "Mobiliser les relais du quartier au lieu d'agir seul",
            moral = 1,
            opinion = 1,
            prestige = 1,
            impact = 2,
            risk = event.stakes.coerceAtLeast(1),
            approach = "CARE",
            stakes = event.stakes,
            sourceCategory = event.category,
            relationDelta = 2,
            flag = "${FLAG_PREFIX}district"
        )
        Scope.CITY -> Choice(
            label = "Coordonner secours, accès et infrastructures à l'échelle de la ville",
            prestige = 2,
            opinion = 1,
            impact = 3,
            risk = (event.stakes - 1).coerceAtLeast(1),
            approach = "ORDER",
            stakes = event.stakes,
            sourceCategory = event.category,
            flag = "${FLAG_PREFIX}city"
        )
        Scope.REGION -> Choice(
            label = "Prioriser les zones menacées et déléguer ce que tu ne peux pas couvrir",
            moral = 1,
            prestige = 2,
            impact = 4,
            risk = event.stakes,
            approach = "ORDER",
            stakes = event.stakes,
            sourceCategory = event.category,
            relationDelta = 1,
            flag = "${FLAG_PREFIX}region"
        )
        Scope.COUNTRY -> Choice(
            label = "Partager le commandement avec le réseau national plutôt que tout centraliser",
            opinion = 2,
            prestige = 2,
            impact = 5,
            risk = event.stakes,
            approach = "TRUTH",
            stakes = event.stakes,
            sourceCategory = event.category,
            identityDelta = 1,
            flag = "${FLAG_PREFIX}country"
        )
        Scope.WORLD -> Choice(
            label = "Synchroniser plusieurs réponses internationales et accepter de ne pas être partout",
            moral = 1,
            opinion = 2,
            prestige = 3,
            impact = 6,
            risk = event.stakes.coerceAtLeast(2),
            approach = "CARE",
            stakes = event.stakes,
            sourceCategory = event.category,
            relationDelta = 1,
            identityDelta = 1,
            flag = "${FLAG_PREFIX}world"
        )
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
