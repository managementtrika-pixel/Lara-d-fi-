package com.metahumanlegacy.game

internal data class GenerationalUpdate(
    val campaign: Campaign,
    val ultimate: UltimateState,
    val deep: DeepLifeState,
    val echo: String = ""
)

internal object GenerationalDirector {
    fun seedNewLife(c: Campaign, state: DeepLifeState, legacies: List<DeepLegacyRecord>): DeepLifeState {
        val legacy = legacies.firstOrNull() ?: return state
        val memory = CharacterMemory(
            id = "legacy_echo_${legacy.seed}", turn = 0, age = 8, eventId = "LEGACY_ECHO",
            summary = "Dans ce monde, on parle encore de ${legacy.alias.ifBlank { legacy.name }} : ${legacy.headline}",
            emotion = "HÉRITAGE", weight = 3, tags = setOf("LEGACY", "WORLD_HISTORY")
        )
        return state.copy(memories = listOf(memory) + state.memories)
    }

    fun afterChoice(c: Campaign, u: UltimateState, d: DeepLifeState, event: EventNode, choice: Choice): GenerationalUpdate {
        var campaign = c
        var ultimate = u
        var deep = d
        val echo = mutableListOf<String>()

        // Once per life, a lethal failure before old age can become a severe continuation instead of a hard stop.
        if (campaign.health <= 0 && campaign.age < 65 && "V2_CRITICAL_SURVIVAL" !in campaign.flags && event.kind != "ENDING") {
            campaign = campaign.copy(
                health = 9,
                flags = campaign.flags + "V2_CRITICAL_SURVIVAL",
                timeline = (campaign.timeline + "${campaign.age} ans — Mort clinique évitée de peu après ${event.title}.").takeLast(180)
            )
            ultimate = ultimate.copy(debt = ultimate.debt + 600, retirementIntent = if (campaign.age >= 50) "Réévaluer après la convalescence" else ultimate.retirementIntent)
            val injury = PersistentInjury(
                id = "critical_${campaign.turn}_${event.id}", bodyPart = "séquelles multiples", severity = 10,
                originEvent = event.title, originAge = campaign.age, chronic = true, recovery = 0
            )
            deep = deep.copy(
                injuries = (deep.injuries + injury).distinctBy { it.id },
                drama = deep.drama.copy(recoveryNeed = 100, personalPressure = (deep.drama.personalPressure + 25).coerceAtMost(100))
            )
            echo += "Tu ne gagnes pas cette scène. Tu y survis. Coma, dette médicale et séquelles transforment l'échec en nouvelle partie de ta vie au lieu d'effacer la partie."
        }

        val mentored = "V2_MENTOR_NEXT_GENERATION" in campaign.flags
        if (mentored && campaign.age >= 35) {
            deep = deep.copy(relationships = deep.relationships.map { r ->
                if (r.id != "peer") r else {
                    val nextPhase = when {
                        r.phase == RelationshipPhase.PROTEGE && r.trust >= 78 -> RelationshipPhase.SUCCESSOR
                        r.phase !in setOf(RelationshipPhase.SUCCESSOR, RelationshipPhase.RIVAL) -> RelationshipPhase.PROTEGE
                        else -> r.phase
                    }
                    r.copy(
                        phase = nextPhase,
                        trust = (r.trust + choice.relationDelta.coerceAtLeast(1)).coerceAtMost(100),
                        admiration = (r.admiration + 3).coerceAtMost(100)
                    )
                }
            })
            val protege = deep.relationships.firstOrNull { it.id == "peer" }
            if (protege != null && ultimate.protege != protege.name) {
                ultimate = ultimate.copy(protege = protege.name)
                echo += "${protege.name} cesse d'être seulement un pair : tu commences réellement à transmettre."
            }
        }

        if ("V2_RETIRE_NOW" in campaign.flags && campaign.age >= 68) {
            val successor = deep.relationships.firstOrNull { it.phase == RelationshipPhase.SUCCESSOR }
            campaign = campaign.copy(
                flags = campaign.flags + "V2_RETIRED",
                turn = maxOf(campaign.turn, 196),
                timeline = (campaign.timeline + "${campaign.age} ans — Tu choisis de quitter le terrain${successor?.let { " et de laisser ${it.name} prendre sa place" } ?: ""}.").takeLast(180)
            )
            ultimate = ultimate.copy(retirementIntent = successor?.let { "Retraité · symbole transmis à ${it.name}" } ?: "Retraité volontaire")
            echo += if (successor != null) {
                "Tu pourrais encore intervenir. Tu choisis pourtant de regarder ${successor.name} agir sans toi. Pour la première fois, le monde continue — et c'est précisément la preuve que ta carrière a servi à quelque chose."
            } else {
                "Tu quittes le terrain sans attendre que ton corps ou un ennemi prenne la décision à ta place. La retraite devient un acte, pas un écran de fin."
            }
        }

        return GenerationalUpdate(campaign, ultimate, deep, echo.joinToString("\n"))
    }
}
