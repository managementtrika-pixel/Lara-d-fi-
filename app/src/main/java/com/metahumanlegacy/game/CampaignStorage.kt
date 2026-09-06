package com.metahumanlegacy.game

import android.content.Context
import android.net.Uri

internal fun saveCampaignV4(context: Context, c: Campaign) {
    fun e(v: Any) = Uri.encode(v.toString())
    fun map(v: Map<String, Int>) = v.entries.sortedBy { it.key }.joinToString(";") { "${it.key}:${it.value}" }
    val flags = c.flags.joinToString(";")
    val threads = c.threads.joinToString(";") {
        listOf(
            it.id, it.openedTurn, it.lastTurn, it.stage, it.lastApproach, it.intensity,
            it.care, it.order, it.truth, it.ascend
        ).joinToString(",")
    }
    val timeline = c.timeline.joinToString("\n")
    val fields = listOf(
        c.seed, c.name, c.alias, c.origin, c.powerFamily, c.weakness, c.modifier,
        c.pronouns, c.city, c.district, c.socialBackground, c.motivation, c.civilianPath,
        c.temperament, c.visualStyle,
        c.turn, c.morality, c.prestige, c.opinion, c.fear, c.power, c.control, c.influence,
        c.health, c.civilianCasualties, c.identityExposure,
        c.familyBond, c.rivalStanding, c.governmentStanding, c.factionStanding, c.mediaStanding,
        flags, threads, c.lastCategory, c.lastApproach, timeline,
        map(c.affinityScores), map(c.expressionScores), map(c.costScores), c.formativeRisk,
        c.powerRevealText, c.powerCostText, c.powerSignature
    ).joinToString("|") { e(it) }
    context.getSharedPreferences("legacy", Context.MODE_PRIVATE)
        .edit().putString("campaign", "V5|$fields").apply()
}

internal fun loadCampaignV4(context: Context): Campaign? {
    val raw = context.getSharedPreferences("legacy", Context.MODE_PRIVATE)
        .getString("campaign", null) ?: return null
    return runCatching {
        when {
            raw.startsWith("V5|") -> parseV4(raw.removePrefix("V5|"))
            raw.startsWith("V4|") -> migrateV4Chronology(parseV4(raw.removePrefix("V4|")))
            raw.startsWith("V3|") -> migrateV3(raw.removePrefix("V3|"))
            else -> null
        }
    }.getOrNull()
}

private fun parseV4(raw: String): Campaign {
    val p = raw.split('|').map(Uri::decode)
    fun map(rawMap: String): Map<String, Int> = rawMap.split(';').mapNotNull {
        val pair = it.split(':', limit = 2)
        if (pair.size == 2) pair[0] to (pair[1].toIntOrNull() ?: 0) else null
    }.toMap()
    val flags = p.getOrElse(31) { "" }.split(';').filter { it.isNotBlank() }.toSet()
    val threads = p.getOrElse(32) { "" }.split(';').mapNotNull { item ->
        val t = item.split(',')
        if (t.size < 10) null else StoryThread(
            t[0], t[1].toInt(), t[2].toInt(), t[3].toInt(), t[4], t[5].toInt(),
            t[6].toInt(), t[7].toInt(), t[8].toInt(), t[9].toInt()
        )
    }
    return Campaign(
        seed = p[0].toLong(), name = p[1], alias = p[2], origin = p[3],
        powerFamily = p[4], weakness = p[5], modifier = p[6],
        pronouns = p[7], city = p[8], district = p[9], socialBackground = p[10],
        motivation = p[11], civilianPath = p[12], temperament = p[13], visualStyle = p[14],
        turn = p[15].toInt(), morality = p[16].toInt(), prestige = p[17].toInt(),
        opinion = p[18].toInt(), fear = p[19].toInt(), power = p[20].toInt(),
        control = p[21].toInt(), influence = p[22].toInt(), health = p[23].toInt(),
        civilianCasualties = p[24].toInt(), identityExposure = p[25].toInt(),
        familyBond = p[26].toInt(), rivalStanding = p[27].toInt(),
        governmentStanding = p[28].toInt(), factionStanding = p[29].toInt(),
        mediaStanding = p[30].toInt(), flags = flags, threads = threads,
        lastCategory = p.getOrElse(33) { "" }, lastApproach = p.getOrElse(34) { "" },
        timeline = p.getOrElse(35) { "" }.lines().filter { it.isNotBlank() },
        affinityScores = map(p.getOrElse(36) { "" }),
        expressionScores = map(p.getOrElse(37) { "" }),
        costScores = map(p.getOrElse(38) { "" }),
        formativeRisk = p.getOrElse(39) { "0" }.toInt(),
        powerRevealText = p.getOrElse(40) { "" },
        powerCostText = p.getOrElse(41) { "" },
        powerSignature = p.getOrElse(42) { "" }
    )
}


private fun migrateV4Chronology(old: Campaign): Campaign {
    fun shiftAgeText(line: String): String {
        val match = Regex("""^(\d{1,3}) ans""").find(line) ?: return line
        val oldAge = match.groupValues[1].toIntOrNull() ?: return line
        if (oldAge < 18) return line
        val newAge = (oldAge - 10).coerceAtLeast(8)
        return line.replaceRange(match.range, "$newAge ans")
    }

    fun shiftDeepMemory(flag: String): String {
        if (!flag.startsWith("deep:memory=")) return flag
        val payload = flag.substringAfter('=')
        val parts = payload.split(',').toMutableList()
        if (parts.size < 2) return flag
        val age = parts[1].toIntOrNull() ?: return flag
        parts[1] = (age - 10).coerceAtLeast(8).toString()
        return "deep:memory=" + parts.joinToString(",")
    }

    val migratedFlags = old.flags
        .map(::shiftDeepMemory)
        .toSet() + setOf("MIGRATED_V4_TO_CHILDHOOD_CANON", "CHRONOLOGY_8_TO_18")

    val migratedTimeline = old.timeline
        .map(::shiftAgeText)
        .let { timeline ->
            (timeline + "↳ Sauvegarde migrée vers la chronologie canonique : années formatives de 8 à 17 ans, éveil à 18 ans.")
                .takeLast(180)
        }

    return old.copy(
        flags = migratedFlags,
        timeline = migratedTimeline
    )
}

private fun migrateV3(raw: String): Campaign {
    val p = raw.split('|').map(Uri::decode)
    val oldFlags = p.getOrElse(29) { "" }.split(';').filter { it.isNotBlank() }.toSet()
    val oldThreads = p.getOrElse(30) { "" }.split(';').mapNotNull { item ->
        val t = item.split(',')
        if (t.size < 6) null else StoryThread(
            t[0], t[1].toInt(), t[2].toInt(), t[3].toInt(), t[4], t[5].toInt()
        )
    }
    return Campaign(
        seed = p[0].toLong(), name = p[1], alias = p[2], origin = p[3],
        powerFamily = p[4], weakness = p[5], modifier = p[6],
        pronouns = p[7], city = p[8], district = p[9], socialBackground = p[10],
        motivation = p[11], civilianPath = "Ancienne destinée", temperament = "Inconnu",
        visualStyle = p[12],
        turn = p[13].toInt().coerceAtLeast(16),
        morality = p[14].toInt(), prestige = p[15].toInt(), opinion = p[16].toInt(),
        fear = p[17].toInt(), power = p[18].toInt(), control = p[19].toInt(),
        influence = p[20].toInt(), health = p[21].toInt(),
        civilianCasualties = p[22].toInt(), identityExposure = p[23].toInt(),
        familyBond = p[24].toInt(), rivalStanding = p[25].toInt(),
        governmentStanding = p[26].toInt(), factionStanding = p[27].toInt(),
        mediaStanding = p[28].toInt(),
        flags = oldFlags + setOf(
            "POWER_REVEALED", "FORMATIVE_DECADE_COMPLETE", "ALIAS_CHOSEN",
            "MIGRATED_V3", "CHRONOLOGY_8_TO_18"
        ),
        threads = oldThreads,
        lastCategory = p.getOrElse(31) { "" }, lastApproach = p.getOrElse(32) { "" },
        timeline = p.getOrElse(33) { "" }.lines().filter { it.isNotBlank() }
            .map { line ->
                val m = Regex("""^(\d{1,3}) ans""").find(line)
                if (m == null) line else {
                    val age = m.groupValues[1].toIntOrNull()
                    if (age == null || age < 18) line
                    else line.replaceRange(m.range, "${(age - 10).coerceAtLeast(8)} ans")
                }
            },
        powerRevealText = "Ton pouvoir s'était déjà manifesté avant cette version de la destinée.",
        powerCostText = "Ta faiblesse connue reste ${p[5]}.",
        powerSignature = "Le monde reconnaît déjà ta signature métahumaine."
    )
}

internal fun clearCampaignV4(context: Context) =
    context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit().remove("campaign").apply()

internal fun loadHallV4(context: Context): List<String> =
    context.getSharedPreferences("legacy", Context.MODE_PRIVATE)
        .getString("hall", "").orEmpty().split(";;").filter { it.isNotBlank() }

internal fun saveHallV4(context: Context, hall: List<String>) =
    context.getSharedPreferences("legacy", Context.MODE_PRIVATE)
        .edit().putString("hall", hall.joinToString(";;")).apply()
