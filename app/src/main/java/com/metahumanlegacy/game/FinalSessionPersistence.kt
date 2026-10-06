package com.metahumanlegacy.game

import android.content.Context
import org.json.JSONObject

internal data class FinalSession(
    val campaign: Campaign,
    val ultimate: UltimateState,
    val annual: AnnualActionState,
    val deep: DeepLifeState,
    val outcome: String? = null
)

/** One atomic checkpoint includes the decision AND the consequence still awaiting the player. */
internal object FinalSessionPersistence {
    const val ACTIVE = "final_session_v5"
    const val PREVIOUS = "final_session_previous_v5"

    fun load(context: Context): FinalSession? {
        val prefs = context.getSharedPreferences("legacy", Context.MODE_PRIVATE)
        val checkpoint = prefs.getString(ACTIVE, null)?.let(::decode)
            ?: prefs.getString(PREVIOUS, null)?.let(::decode)
        if (checkpoint != null) return normalized(checkpoint)
        val c = loadCampaignV4(context) ?: return null
        val u = UltimateStore.load(context, c)
        val oldOutcome = context.getSharedPreferences("mhl_ultimate_session_v1", Context.MODE_PRIVATE)
            .getString("outcome_${c.seed}", null)
        val migrated = normalized(FinalSession(c, u, AnnualActionPersistence.load(context, c),
            DeepLifePersistence.load(context, c, u), oldOutcome))
        save(context, migrated)
        return migrated
    }

    fun normalized(s: FinalSession): FinalSession {
        val (annual, deep) = FinalGameRules.synchronize(s.campaign, s.annual, s.deep)
        return s.copy(annual = annual, deep = deep)
    }

    fun save(context: Context, session: FinalSession) {
        val s = normalized(session)
        val prefs = context.getSharedPreferences("legacy", Context.MODE_PRIVATE)
        val previous = prefs.getString(ACTIVE, null)
        val edit = prefs.edit().putString("campaign", encodeCampaignV4(s.campaign))
            .putString(ACTIVE, encode(s))
        if (previous != null && decode(previous) != null) edit.putString(PREVIOUS, previous)
        edit.apply()
    }

    fun encode(s: FinalSession): String = JSONObject()
        .put("format", "METAHUMAN_LEGACY_SESSION").put("version", 5)
        .put("campaign", encodeCampaignV4(s.campaign))
        .put("ultimate", UltimateStore.encode(s.ultimate))
        .put("annual", AnnualActionPersistence.encode(s.annual))
        .put("deep", DeepLifePersistence.encode(s.deep))
        .put("outcome", s.outcome ?: JSONObject.NULL).toString()

    fun decode(raw: String): FinalSession? = runCatching {
        val root = JSONObject(raw)
        require(root.getString("format") == "METAHUMAN_LEGACY_SESSION" && root.getInt("version") == 5)
        val c = requireNotNull(decodeCampaignV4(root.getString("campaign")))
        val u = requireNotNull(UltimateStore.decode(root.getString("ultimate")))
        val a = requireNotNull(AnnualActionPersistence.decode(root.getString("annual")))
        val d = requireNotNull(DeepLifePersistence.decode(root.getString("deep")))
        require(c.seed == u.seed && c.seed == a.seed && c.seed == d.seed)
        require(c.turn in 0..256 && c.health in 0..100 && a.turn == c.turn)
        FinalSession(c, u, a, d, if (root.isNull("outcome")) null else root.getString("outcome"))
    }.getOrNull()
}
