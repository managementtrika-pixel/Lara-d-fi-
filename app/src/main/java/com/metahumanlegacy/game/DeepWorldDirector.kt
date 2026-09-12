package com.metahumanlegacy.game

internal data class DeepWorldUpdate(
    val campaign: Campaign,
    val ultimate: UltimateState,
    val deep: DeepLifeState,
    val echo: String = ""
)

/** Applies V2 consequences to the existing playable world state, not just the V2 dossier. */
internal object DeepWorldDirector {
    fun afterChoice(
        campaign: Campaign,
        ultimate: UltimateState,
        deep: DeepLifeState,
        event: EventNode,
        choice: Choice
    ): DeepWorldUpdate {
        var c = campaign
        var u = ultimate
        var d = deep
        val echo = mutableListOf<String>()

        val districtIndex = u.districts.indexOfFirst { it.name == c.district }.let { if (it >= 0) it else 0 }
        if (u.districts.isNotEmpty()) {
            val old = u.districts[districtIndex]
            val damageDelta = when {
                event.stakes >= 4 && choice.approach == "ASCEND" -> 8 + choice.risk / 2
                event.stakes >= 4 && choice.approach == "CARE" -> 2
                event.category.contains("CATA", true) || event.category.contains("CRISE", true) -> 3
                else -> 0
            }
            val crimeDelta = when (choice.approach) {
                "ORDER" -> -2
                "TRUTH" -> -1
                "ASCEND" -> if (c.fear >= 45) -2 else 1
                else -> 0
            }
            val reconstructionDelta = if (choice.approach == "CARE" && event.stakes >= 3) 2 else 0
            val sentimentDelta = choice.opinion + choice.moral + when (choice.approach) {
                "CARE" -> 2
                "ASCEND" -> -1
                else -> 0
            }
            val next = old.copy(
                damage = (old.damage + damageDelta - reconstructionDelta).coerceIn(0, 100),
                crime = (old.crime + crimeDelta).coerceIn(0, 100),
                reconstruction = (old.reconstruction + reconstructionDelta).coerceIn(0, 100),
                sentiment = (old.sentiment + sentimentDelta).coerceIn(-100, 100),
                restricted = old.restricted || (c.governmentStanding <= -60 && c.scope >= Scope.CITY)
            )
            val districts = u.districts.toMutableList().also { it[districtIndex] = next }
            u = u.copy(
                districts = districts,
                cityCondition = (u.cityCondition - damageDelta + reconstructionDelta).coerceIn(0, 100)
            )
            if (old.damage < 55 && next.damage >= 55) echo += "${next.name} bascule dans une reconstruction visible : les prochaines scènes n'y auront plus le même décor ni la même patience collective."
            if (old.sentiment >= -30 && next.sentiment < -30) echo += "${next.name} cesse de te considérer comme une présence naturellement rassurante."
        }

        val frame = mediaFrame(c, d)
        val legal = legalStatus(c, d)
        if (frame != u.mediaFrame && c.powerRevealed) echo += "Le récit médiatique change : « $frame »."
        if (legal != u.legalStatus && c.powerRevealed) echo += "Ton statut légal évolue : $legal."
        u = u.copy(mediaFrame = frame, legalStatus = legal)

        val economy = updateEconomy(c, u, event, choice)
        u = economy.first
        echo += economy.second

        val faction = updateFaction(c, u, event, choice)
        u = faction.first
        c = faction.second

        val nemesis = updateNemesis(c, u, event, choice)
        u = nemesis.first
        echo += nemesis.second

        val aging = updateAging(c, u, d)
        c = aging.first
        u = aging.second
        d = aging.third
        echo += aging.fourth

        val retirement = updateRetirement(c, u, d)
        u = retirement.first
        echo += retirement.second

        return DeepWorldUpdate(c, u, d, echo.filter { it.isNotBlank() }.distinct().take(5).joinToString("\n"))
    }

    fun afterAnnualAction(
        campaign: Campaign,
        ultimate: UltimateState,
        deep: DeepLifeState,
        card: AnnualActionCard
    ): DeepWorldUpdate {
        var c = campaign
        var u = ultimate
        var d = deep
        val echo = mutableListOf<String>()

        when (card.category) {
            AnnualActionCategory.CIVIL -> {
                val pay = 90 + u.incomeTier * 60
                u = u.copy(credits = u.credits + pay)
                echo += "Ta vie civile rapporte $pay crédits. Être métahumain n'efface ni le travail ni les factures."
            }
            AnnualActionCategory.RELATION -> {
                val target = d.opportunities.firstOrNull { it.category == "RELATION" }?.personId
                    ?: d.relationships.filter { it.alive }.maxByOrNull { 100 - it.trust }?.id
                if (target != null) {
                    d = d.copy(relationships = d.relationships.map {
                        if (it.id == target) it.copy(trust = (it.trust + 4).coerceAtMost(100), affection = (it.affection + 3).coerceAtMost(100)) else it
                    })
                }
            }
            AnnualActionCategory.RECOVERY -> {
                val cost = if (c.age >= 50) 75 else 45
                u = u.copy(credits = (u.credits - cost).coerceAtLeast(0))
                echo += "La récupération a un coût concret ($cost crédits), mais elle protège les années qui viennent."
            }
            AnnualActionCategory.PUBLIC -> {
                u = u.copy(mediaFrame = if (d.perception.civilians >= 20) "Figure accessible" else "Figure sous observation")
            }
            AnnualActionCategory.TRAINING -> {
                val maintenance = if (u.baseStage >= 2) 30 else 12
                u = u.copy(credits = (u.credits - maintenance).coerceAtLeast(0))
            }
            AnnualActionCategory.INTERVENTION -> {
                val idx = u.districts.indexOfFirst { it.name == c.district }
                if (idx >= 0) {
                    val list = u.districts.toMutableList()
                    val district = list[idx]
                    list[idx] = district.copy(crime = (district.crime - 2).coerceAtLeast(0), sentiment = (district.sentiment + 1).coerceAtMost(100))
                    u = u.copy(districts = list)
                }
            }
            AnnualActionCategory.INVESTIGATION -> Unit
        }

        if (u.credits == 0 && u.debt > 0) echo += "Tes ressources deviennent une contrainte. Le costume, les soins et le QG ne se financent pas avec la réputation."
        return DeepWorldUpdate(c, u, d, echo.distinct().joinToString("\n"))
    }

    private fun mediaFrame(c: Campaign, d: DeepLifeState): String {
        if (!c.powerRevealed) return "Inconnu"
        val p = d.perception
        return when {
            p.civilians >= 55 && p.civilianFear < 25 -> "Protecteur populaire"
            p.criminalFear >= 75 && p.civilians >= 10 -> "Vigilante redouté"
            p.government <= -45 && p.civilians >= 20 -> "Héros hors système"
            p.government >= 45 && c.governmentStanding >= 30 -> "Figure institutionnelle"
            p.civilianFear >= 55 -> "Pouvoir jugé inquiétant"
            c.identityExposure >= 75 -> "Identité au cœur des spéculations"
            c.prestige >= 90 -> "Icône métahumaine"
            c.opinion <= -35 -> "Figure controversée"
            else -> "Présence métahumaine suivie"
        }
    }

    private fun legalStatus(c: Campaign, d: DeepLifeState): String {
        if (!c.powerRevealed) return "Civil"
        val evidencePressure = d.identityEvidence.sumOf { it.strength }.coerceAtMost(400)
        return when {
            c.governmentStanding <= -70 && c.fear >= 60 -> "Recherché prioritaire"
            c.governmentStanding <= -45 || (evidencePressure >= 240 && c.opinion < 0) -> "Vigilante surveillé"
            c.governmentStanding >= 55 && c.opinion >= 20 -> "Autorisé / coopérant"
            c.governmentStanding >= 25 -> "Toléré sous conditions"
            else -> "Non enregistré"
        }
    }

    private fun updateEconomy(c: Campaign, u: UltimateState, event: EventNode, choice: Choice): Pair<UltimateState, String> {
        if (!c.powerRevealed || c.turn % 4 != 0) return u to ""
        val upkeep = 30 + u.baseStage * 35 + u.injuries.size * 8 + if (u.costumeEra >= 2) 25 else 0
        val income = when {
            u.sponsor != "Aucun" -> 160 + u.incomeTier * 60
            c.civilianPath.contains("Commerce", true) || c.civilianPath.contains("Technologie", true) -> 130 + u.incomeTier * 45
            c.civilianPath.contains("Droit", true) || c.civilianPath.contains("scient", true) -> 120 + u.incomeTier * 40
            else -> 95 + u.incomeTier * 32
        }
        val collateral = if (event.stakes >= 4 && choice.approach == "ASCEND") 90 else 0
        val balance = income - upkeep - collateral
        val credits = u.credits + balance
        return if (credits >= 0) {
            u.copy(credits = credits) to if (kotlin.math.abs(balance) >= 100) "Ta double vie pèse sur les finances : ${if (balance >= 0) "+" else ""}$balance crédits sur cette période." else ""
        } else {
            u.copy(credits = 0, debt = u.debt + -credits) to "Les coûts dépassent ce que ta vie civile peut absorber. Ta dette atteint ${u.debt + -credits} crédits."
        }
    }

    private fun updateFaction(c: Campaign, u: UltimateState, event: EventNode, choice: Choice): Pair<UltimateState, Campaign> {
        if (!event.category.contains("FACTION", true) && !event.category.contains("GOUVER", true) && c.turn % 12 != 0) return u to c
        var standing = c.factionStanding
        standing += when (choice.approach) { "CARE" -> 1; "ORDER" -> 2; "TRUTH" -> -1; "ASCEND" -> -2; else -> 0 }
        val law = when {
            c.governmentStanding <= -60 -> "Pouvoirs métahumains soumis à mandat spécial"
            c.identityExposure >= 70 -> "Débat national sur l'enregistrement"
            c.governmentStanding >= 55 -> "Cadre de coopération métahumaine"
            else -> u.metaLaw
        }
        val factionName = when {
            standing >= 55 -> "Coalition civique"
            standing <= -55 -> "Bloc anti-vigilantes"
            else -> u.districts.firstOrNull()?.faction ?: "Aucune"
        }
        val districts = if (factionName == "Aucune" || u.districts.isEmpty()) u.districts else u.districts.mapIndexed { i, d ->
            if (i == positiveMod(mix(c.seed, c.turn * 41L), u.districts.size)) d.copy(faction = factionName) else d
        }
        return u.copy(metaLaw = law, districts = districts) to c.copy(factionStanding = standing.coerceIn(-100, 100))
    }

    private fun updateNemesis(c: Campaign, u: UltimateState, event: EventNode, choice: Choice): Pair<UltimateState, String> {
        if (!c.powerRevealed) return u to ""
        var name = u.nemesis
        var adaptation = u.nemesisAdaptation
        var echo = ""
        if (name.isBlank() && (event.category.contains("RIVAL", true) || choice.risk >= 7) && c.age >= 22) {
            val candidates = listOf("Vanta", "Morrow", "Iris Null", "Kestrel", "Le Témoin", "Helix", "Mantis", "Cendre")
            name = candidates[positiveMod(mix(c.seed, c.turn * 313L + event.id.hashCode()), candidates.size)]
            adaptation = 8
            echo = "$name cesse d'être un simple adversaire de circonstance. Quelque chose de personnel vient de commencer."
        } else if (name.isNotBlank()) {
            adaptation = (adaptation + if (choice.approach == c.lastApproach && choice.approach.isNotBlank()) 4 else 2).coerceIn(0, 100)
            if (adaptation in 50..53) echo = "$name a désormais observé assez de tes habitudes pour construire ses plans autour de tes réflexes les plus prévisibles."
        }
        return u.copy(nemesis = name, nemesisAdaptation = adaptation) to echo
    }

    private data class AgingResult(val campaign: Campaign, val ultimate: UltimateState, val deep: DeepLifeState, val fourth: String)

    private fun updateAging(c: Campaign, u: UltimateState, d: DeepLifeState): AgingResult {
        if (c.age < 40 || c.turn % 8 != 0) return AgingResult(c, u, d, "")
        val chronicCount = d.injuries.count { it.chronic && it.recovery < 100 }
        val wear = when {
            c.age >= 70 -> 3
            c.age >= 60 -> 2
            c.age >= 50 -> 1
            else -> 0
        } + chronicCount.coerceAtMost(2)
        val nextHealth = (c.health - wear).coerceAtLeast(0)
        val text = when {
            c.age >= 70 -> "Ton expérience continue de grandir, mais ton corps ne traite plus une mauvaise année comme il le faisait à trente ans."
            c.age >= 60 -> "La récupération devient une décision stratégique : ignorer les blessures coûte désormais des années, pas seulement des points."
            c.age >= 50 && chronicCount > 0 -> "Certaines blessures anciennes reviennent dans les gestes ordinaires. Ton histoire est aussi inscrite dans ton corps."
            else -> ""
        }
        val power = d.powerEvolution?.let { p ->
            p.copy(mastery = (p.mastery + 1).coerceAtMost(100), strain = (p.strain + wear * 2).coerceAtMost(100))
        }
        return AgingResult(c.copy(health = nextHealth), u, d.copy(powerEvolution = power), text)
    }

    private fun updateRetirement(c: Campaign, u: UltimateState, d: DeepLifeState): Pair<UltimateState, String> {
        if (c.age < 58) return u to ""
        val successor = d.relationships.firstOrNull { it.phase == RelationshipPhase.SUCCESSOR }
        val intent = when {
            successor != null && c.age >= 65 -> "Transmettre à ${successor.name}"
            c.health <= 35 -> "Envisage sérieusement la retraite"
            c.age >= 70 -> "Dernière phase de carrière"
            c.age >= 60 && u.retirementIntent == "Indécis" -> "Commence à penser à l'après"
            else -> u.retirementIntent
        }
        val echo = if (intent != u.retirementIntent) "Pour la première fois, la fin de carrière n'est plus une abstraction : $intent." else ""
        return u.copy(retirementIntent = intent) to echo
    }
}
