package com.metahumanlegacy.game

internal object MediaNarrativeDirector {
    fun headlines(c: Campaign, u: UltimateState, d: DeepLifeState, event: EventNode, choice: Choice): String {
        if (!c.powerRevealed || event.stakes < 3) return ""
        val savedAngle = when {
            choice.moral >= 2 || choice.approach == "CARE" -> "DES VIES SAUVÉES AU CŒUR DE L'INTERVENTION"
            choice.approach == "TRUTH" -> "UNE VERSION OFFICIELLE MISE EN DOUTE APRÈS DE NOUVEAUX ÉLÉMENTS"
            choice.approach == "ORDER" -> "INTERVENTION RAPIDE, MÉTHODE CONTESTÉE MAIS EFFICACE"
            else -> "DÉMONSTRATION DE PUISSANCE : LA VILLE COMPTE AUSSI LES DÉGÂTS"
        }
        val institutionAngle = when {
            u.legalStatus.contains("Recherché") -> "LES AUTORITÉS RAPPELLENT QUE LE VIGILANTE RESTE RECHERCHÉ"
            u.legalStatus.contains("surveillé") -> "LES AUTORITÉS DEMANDENT DES EXPLICATIONS SUR LES CONDITIONS D'INTERVENTION"
            d.perception.government >= 35 -> "LE GOUVERNEMENT SALUE LA COOPÉRATION, SANS ÉCARTER UN ENCADREMENT PLUS PRÉCIS"
            else -> "QUI CONTRÔLE LES MÉTAHUMAINS QUAND L'URGENCE RETOMBE ?"
        }
        val feedAngle = when {
            c.identityExposure >= 65 -> "LES INTERNAUTES PENSENT AVOIR TROUVÉ UN NOUVEL INDICE SUR SON IDENTITÉ"
            d.perception.civilianFear >= 45 -> "#MENACE OU #HÉROS ? LES IMAGES DIVISENT ENCORE"
            u.heroPresentation == "Flamboyant" -> "SON NOUVEAU LOOK FAIT PLUS PARLER QUE LA CONFÉRENCE DE PRESSE"
            else -> "LA VIDÉO DE L'INTERVENTION TOURNE DÉJÀ PARTOUT"
        }
        return "VOIX DE LA VILLE\n• Presse locale : $savedAngle\n• Institutionnel : $institutionAngle\n• Flux public : $feedAngle"
    }
}
