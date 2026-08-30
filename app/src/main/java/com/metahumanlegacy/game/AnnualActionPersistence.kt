package com.metahumanlegacy.game

import android.content.Context

internal object AnnualActionPersistence {
    private const val PREFS = "mhl_annual_actions_v2"

    fun load(context: Context, c: Campaign): AnnualActionState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(c.seed.toString(), null)
        val parsed = raw?.let(::decode) ?: AnnualActionState.fresh(c)
        val synced = parsed.synced(c)
        if (synced != parsed) save(context, synced)
        return synced
    }

    fun save(context: Context, state: AnnualActionState) {
        val raw = listOf(
            state.seed,
            state.turn,
            state.used,
            state.rescue,
            state.investigation,
            state.presence,
            state.discipline,
            state.usedIds.sorted().joinToString(",")
        ).joinToString("|")
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(state.seed.toString(), raw).apply()
    }

    fun clear(context: Context, seed: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(seed.toString()).apply()
    }

    private fun decode(raw: String): AnnualActionState? = runCatching {
        val p = raw.split('|')
        AnnualActionState(
            seed = p[0].toLong(),
            turn = p[1].toInt(),
            used = p[2].toInt().coerceIn(0, ANNUAL_ACTION_LIMIT),
            rescue = p[3].toInt().coerceIn(0, 100),
            investigation = p[4].toInt().coerceIn(0, 100),
            presence = p[5].toInt().coerceIn(0, 100),
            discipline = p[6].toInt().coerceIn(0, 100),
            usedIds = p.getOrElse(7) { "" }.split(',').filter { it.isNotBlank() }.toSet()
        )
    }.getOrNull()
}
