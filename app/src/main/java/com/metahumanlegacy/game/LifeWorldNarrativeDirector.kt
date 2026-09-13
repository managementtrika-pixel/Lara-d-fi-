package com.metahumanlegacy.game

/**
 * Bridges the persistent life-simulation district state into post-awakening story scenes.
 * Free-time actions must not live in a disconnected submenu: the next major event should remember
 * whether the player made the home district safer, weakened criminal control, earned trust or
 * attracted too much media attention.
 */
internal object LifeWorldNarrativeDirector {
    fun enrich(c: Campaign, deep: DeepLifeState, base: EventNode): EventNode {
        if (!c.powerRevealed || base.kind == "FORMATIVE" || base.kind == "AWAKENING") return base
        val home = deep.lifeSimulation?.districts?.firstOrNull { it.id == "quartier" } ?: return base

        val notes = mutableListOf<String>()
        val extra = mutableListOf<Choice>()

        when {
            home.safety >= 72 -> notes += "Tes patrouilles ont changé le quartier : les rues sont plus sûres et les habitants ne réagissent plus à chaque sirène comme à une catastrophe annoncée."
            home.safety <= 32 -> notes += "Le quartier s'est dégradé entre deux grandes crises. Les habitants vivent déjà sous tension avant même que cette scène commence."
        }
        when {
            home.criminalControl >= 58 -> notes += "Les réseaux criminels ont repris de la place ici. Ils disposent de relais, d'informations et de gens prêts à détourner la situation à leur avantage."
            home.criminalControl <= 15 -> notes += "Le contrôle criminel local a nettement reculé. Certaines menaces n'ont plus le même réseau ni la même liberté de mouvement qu'autrefois."
        }
        when {
            home.localTrust >= 52 -> notes += "Tu as construit une vraie confiance locale. Des témoins te parlent plus vite et certains civils essaient même de t'aider sans attendre d'ordre."
            home.localTrust <= -20 -> notes += "Ici, ton nom ne suffit plus à rassurer. Même une bonne décision risque d'être interprétée comme une intrusion."
        }
        if (home.mediaHeat >= 48) {
            notes += "Les médias surveillent désormais presque chacune de tes apparitions dans le quartier. Une action spectaculaire laissera immédiatement une trace publique."
        }

        if (home.localTrust >= 55 && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "T'appuyer sur le réseau d'habitants qui te fait confiance",
                moral = 1,
                opinion = 2,
                impact = 1,
                risk = (base.stakes - 1).coerceAtLeast(1),
                approach = "CARE",
                stakes = base.stakes,
                sourceCategory = base.category,
                relationDelta = 1,
                identityDelta = if (home.mediaHeat >= 48) 1 else 0,
                flag = "life_world_local_trust"
            )
        }

        if (home.criminalControl >= 58 && base.choices.size + extra.size < 7) {
            extra += Choice(
                label = "Profiter de la crise pour frapper le réseau criminel local",
                prestige = 1,
                power = 1,
                impact = 2,
                risk = (base.stakes + 1).coerceAtMost(9),
                approach = "ORDER",
                stakes = base.stakes,
                sourceCategory = base.category,
                identityDelta = 1,
                deferredHook = true,
                flag = "life_world_criminal_network"
            )
        }

        if (notes.isEmpty() && extra.isEmpty()) return base
        return base.copy(
            text = buildString {
                append(base.text)
                if (notes.isNotEmpty()) {
                    append("\n\nMÉMOIRE DU QUARTIER\n")
                    append(notes.distinct().take(3).joinToString("\n"))
                }
            },
            choices = (base.choices + extra).distinctBy { it.label }.take(7)
        )
    }
}
