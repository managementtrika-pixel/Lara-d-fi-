package com.metahumanlegacy.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class DeepLegacyRecord(
    val seed: Long,
    val name: String,
    val alias: String,
    val power: String,
    val finalAge: Int,
    val scope: String,
    val headline: String,
    val strongestRelationship: String,
    val nemesis: String,
    val lastingInjury: String,
    val personality: String,
    val memories: List<String>,
    val techniques: List<String>,
    val perception: String
)

internal object DeepLegacyArchive {
    private const val PREFS = "mhl_deep_legacy_v2"
    private const val KEY = "records"

    fun archive(context: Context, c: Campaign, u: UltimateState, d: DeepLifeState) {
        val existing = load(context).filterNot { it.seed == c.seed }
        val strongest = d.relationships
            .filter { it.alive }
            .maxByOrNull { it.trust + it.affection + it.admiration - it.grudge }
        val headline = legacyHeadline(c, d)
        val record = DeepLegacyRecord(
            seed = c.seed,
            name = c.name,
            alias = c.alias,
            power = c.powerFamily,
            finalAge = c.age,
            scope = c.scope.label,
            headline = headline,
            strongestRelationship = strongest?.let { "${it.name} — ${it.role}" }.orEmpty(),
            nemesis = u.nemesis,
            lastingInjury = d.injuries.filter { it.chronic }.maxByOrNull { it.severity }
                ?.let { "${it.bodyPart}, depuis ${it.originAge} ans" }.orEmpty(),
            personality = d.personality.entries.sortedByDescending { it.value }.take(4).joinToString(" · ") { it.key.lowercase() },
            memories = d.memories.sortedByDescending { it.weight }.take(8).sortedBy { it.turn }
                .map { "${it.age} ans — ${it.summary}" },
            techniques = d.powerEvolution?.unlockedTechniques.orEmpty().takeLast(6),
            perception = DeepLifeRuntime.perceptionSummary(d)
        )
        save(context, (listOf(record) + existing).take(60))
    }

    fun load(context: Context): List<DeepLegacyRecord> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(DeepLegacyRecord(
                        seed = o.optLong("seed"), name = o.optString("name"), alias = o.optString("alias"),
                        power = o.optString("power"), finalAge = o.optInt("finalAge"), scope = o.optString("scope"),
                        headline = o.optString("headline"), strongestRelationship = o.optString("strongestRelationship"),
                        nemesis = o.optString("nemesis"), lastingInjury = o.optString("lastingInjury"),
                        personality = o.optString("personality"), memories = o.optJSONArray("memories").strings(),
                        techniques = o.optJSONArray("techniques").strings(), perception = o.optString("perception")
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, records: List<DeepLegacyRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(JSONObject()
                .put("seed", r.seed).put("name", r.name).put("alias", r.alias).put("power", r.power)
                .put("finalAge", r.finalAge).put("scope", r.scope).put("headline", r.headline)
                .put("strongestRelationship", r.strongestRelationship).put("nemesis", r.nemesis)
                .put("lastingInjury", r.lastingInjury).put("personality", r.personality)
                .put("memories", JSONArray(r.memories)).put("techniques", JSONArray(r.techniques))
                .put("perception", r.perception))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    private fun legacyHeadline(c: Campaign, d: DeepLifeState): String {
        val strongestTrait = d.personality.maxByOrNull { it.value }?.key?.lowercase()
        return when {
            c.health <= 0 && c.age < 40 -> "Une carrière interrompue trop tôt, mais impossible à effacer."
            c.scope == Scope.WORLD && c.opinion >= 25 -> "Une figure mondiale dont l'époque porte encore la marque."
            c.scope == Scope.WORLD && c.fear >= 60 -> "Le monde a appris son nom avant d'apprendre à ne plus le prononcer à la légère."
            c.age >= 65 -> "Une vie assez longue pour voir le monde changer — et pour comprendre ce qu'elle y avait réellement laissé."
            strongestTrait != null -> "On se souvient surtout d'une personne $strongestTrait, avant même de se souvenir du pouvoir."
            else -> "Une destinée métahumaine devenue histoire."
        }
    }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }
}
