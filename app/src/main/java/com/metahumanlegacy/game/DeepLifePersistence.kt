package com.metahumanlegacy.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Structured, versioned persistence for the V2 simulation layer. */
internal object DeepLifePersistence {
    private const val PREFS = "mhl_deep_life_v2"
    private const val CURRENT_SCHEMA = 1

    fun load(context: Context, campaign: Campaign, ultimate: UltimateState): DeepLifeState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(campaign.seed.toString(), null)
        val decoded = raw?.let(::decode)?.takeIf { it.seed == campaign.seed }
        return migrate(decoded ?: DeepLifeDirector.bootstrap(campaign, ultimate))
    }

    fun save(context: Context, state: DeepLifeState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(state.seed.toString(), encode(migrate(state))).apply()
    }

    fun clear(context: Context, seed: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(seed.toString()).apply()
    }

    internal fun migrate(state: DeepLifeState): DeepLifeState = when {
        state.schemaVersion >= CURRENT_SCHEMA -> state
        else -> state.copy(schemaVersion = CURRENT_SCHEMA)
    }

    internal fun encode(state: DeepLifeState): String {
        val root = JSONObject()
        root.put("schemaVersion", state.schemaVersion)
        root.put("seed", state.seed)
        root.put("memories", JSONArray().apply { state.memories.forEach { put(memoryJson(it)) } })
        root.put("relationships", JSONArray().apply { state.relationships.forEach { put(relationJson(it)) } })
        root.put("perception", perceptionJson(state.perception))
        root.put("identityEvidence", JSONArray().apply { state.identityEvidence.forEach { put(evidenceJson(it)) } })
        root.put("injuries", JSONArray().apply { state.injuries.forEach { put(injuryJson(it)) } })
        root.put("deferred", JSONArray().apply { state.deferred.forEach { put(deferredJson(it)) } })
        root.put("opportunities", JSONArray().apply { state.opportunities.forEach { put(opportunityJson(it)) } })
        root.put("drama", dramaJson(state.drama))
        state.powerEvolution?.let { root.put("powerEvolution", powerJson(it)) }
        root.put("personality", JSONObject().apply { state.personality.forEach { (k, v) -> put(k, v) } })
        return root.toString()
    }

    internal fun decode(raw: String): DeepLifeState? = runCatching {
        val root = JSONObject(raw)
        val personalityJson = root.optJSONObject("personality") ?: JSONObject()
        val personality = buildMap {
            val keys = personalityJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, personalityJson.optInt(key, 0))
            }
        }
        DeepLifeState(
            schemaVersion = root.optInt("schemaVersion", 1),
            seed = root.getLong("seed"),
            memories = root.optJSONArray("memories").objects(::memoryFromJson),
            relationships = root.optJSONArray("relationships").objects(::relationFromJson),
            perception = root.optJSONObject("perception")?.let(::perceptionFromJson) ?: AudiencePerception(),
            identityEvidence = root.optJSONArray("identityEvidence").objects(::evidenceFromJson),
            injuries = root.optJSONArray("injuries").objects(::injuryFromJson),
            deferred = root.optJSONArray("deferred").objects(::deferredFromJson),
            opportunities = root.optJSONArray("opportunities").objects(::opportunityFromJson),
            drama = root.optJSONObject("drama")?.let(::dramaFromJson) ?: DramaState(),
            powerEvolution = root.optJSONObject("powerEvolution")?.let(::powerFromJson),
            personality = personality
        )
    }.getOrNull()

    private fun memoryJson(v: CharacterMemory) = JSONObject()
        .put("id", v.id).put("turn", v.turn).put("age", v.age).put("personId", v.personId)
        .put("eventId", v.eventId).put("summary", v.summary).put("emotion", v.emotion)
        .put("weight", v.weight).put("tags", stringArray(v.tags))

    private fun memoryFromJson(o: JSONObject) = CharacterMemory(
        id = o.getString("id"), turn = o.optInt("turn"), age = o.optInt("age"),
        personId = o.optStringOrNull("personId"), eventId = o.optString("eventId"),
        summary = o.optString("summary"), emotion = o.optString("emotion"),
        weight = o.optInt("weight", 1), tags = o.optJSONArray("tags").strings().toSet()
    )

    private fun relationJson(v: DeepRelationship) = JSONObject()
        .put("id", v.id).put("name", v.name).put("role", v.role)
        .put("core", JSONObject().put("values", stringArray(v.core.values)).put("fears", stringArray(v.core.fears))
            .put("ambitions", stringArray(v.core.ambitions)).put("boundaries", stringArray(v.core.boundaries))
            .put("secrets", stringArray(v.core.secrets)))
        .put("phase", v.phase.name).put("trust", v.trust).put("affection", v.affection)
        .put("fear", v.fear).put("admiration", v.admiration).put("grudge", v.grudge)
        .put("dependence", v.dependence).put("knowsIdentity", v.knowsIdentity).put("alive", v.alive)
        .put("memories", JSONArray().apply { v.memories.forEach { put(memoryJson(it)) } })

    private fun relationFromJson(o: JSONObject): DeepRelationship {
        val c = o.optJSONObject("core") ?: JSONObject()
        return DeepRelationship(
            id = o.getString("id"), name = o.optString("name"), role = o.optString("role"),
            core = PersonCore(c.optJSONArray("values").strings().toSet(), c.optJSONArray("fears").strings().toSet(),
                c.optJSONArray("ambitions").strings().toSet(), c.optJSONArray("boundaries").strings().toSet(),
                c.optJSONArray("secrets").strings().toSet()),
            phase = runCatching { RelationshipPhase.valueOf(o.optString("phase")) }.getOrDefault(RelationshipPhase.ACQUAINTANCE),
            trust = o.optInt("trust", 50), affection = o.optInt("affection", 50), fear = o.optInt("fear"),
            admiration = o.optInt("admiration"), grudge = o.optInt("grudge"), dependence = o.optInt("dependence"),
            knowsIdentity = o.optBoolean("knowsIdentity"), alive = o.optBoolean("alive", true),
            memories = o.optJSONArray("memories").objects(::memoryFromJson)
        )
    }

    private fun perceptionJson(v: AudiencePerception) = JSONObject()
        .put("district", v.district).put("city", v.city).put("national", v.national)
        .put("government", v.government).put("police", v.police).put("civilians", v.civilians)
        .put("metahumans", v.metahumans).put("youth", v.youth).put("criminalFear", v.criminalFear)
        .put("civilianFear", v.civilianFear).put("governmentFear", v.governmentFear)
        .put("metahumanFear", v.metahumanFear)

    private fun perceptionFromJson(o: JSONObject) = AudiencePerception(
        district = o.optInt("district"), city = o.optInt("city"), national = o.optInt("national"),
        government = o.optInt("government"), police = o.optInt("police"), civilians = o.optInt("civilians"),
        metahumans = o.optInt("metahumans"), youth = o.optInt("youth"), criminalFear = o.optInt("criminalFear"),
        civilianFear = o.optInt("civilianFear"), governmentFear = o.optInt("governmentFear"),
        metahumanFear = o.optInt("metahumanFear")
    )

    private fun evidenceJson(v: IdentityEvidence) = JSONObject().put("id", v.id).put("kind", v.kind)
        .put("strength", v.strength).put("holderId", v.holderId).put("discoveredTurn", v.discoveredTurn)
        .put("description", v.description)
    private fun evidenceFromJson(o: JSONObject) = IdentityEvidence(o.getString("id"), o.optString("kind"),
        o.optInt("strength"), o.optString("holderId"), o.optInt("discoveredTurn"), o.optString("description"))

    private fun injuryJson(v: PersistentInjury) = JSONObject().put("id", v.id).put("bodyPart", v.bodyPart)
        .put("severity", v.severity).put("originEvent", v.originEvent).put("originAge", v.originAge)
        .put("chronic", v.chronic).put("recovery", v.recovery)
    private fun injuryFromJson(o: JSONObject) = PersistentInjury(o.getString("id"), o.optString("bodyPart"),
        o.optInt("severity"), o.optString("originEvent"), o.optInt("originAge"), o.optBoolean("chronic"),
        o.optInt("recovery"))

    private fun deferredJson(v: DeferredConsequence) = JSONObject().put("id", v.id).put("sourceEvent", v.sourceEvent)
        .put("earliestTurn", v.earliestTurn).put("latestTurn", v.latestTurn).put("triggerTags", stringArray(v.triggerTags))
        .put("payload", v.payload).put("resolved", v.resolved)
    private fun deferredFromJson(o: JSONObject) = DeferredConsequence(o.getString("id"), o.optString("sourceEvent"),
        o.optInt("earliestTurn"), o.optInt("latestTurn"), o.optJSONArray("triggerTags").strings().toSet(),
        o.optString("payload"), o.optBoolean("resolved"))

    private fun opportunityJson(v: Opportunity) = JSONObject().put("id", v.id).put("title", v.title)
        .put("category", v.category).put("expiresTurn", v.expiresTurn).put("urgency", v.urgency)
        .put("personId", v.personId).put("district", v.district).put("ignoredPayload", v.ignoredPayload)
    private fun opportunityFromJson(o: JSONObject) = Opportunity(o.getString("id"), o.optString("title"),
        o.optString("category"), o.optInt("expiresTurn"), o.optInt("urgency"), o.optStringOrNull("personId"),
        o.optStringOrNull("district"), o.optString("ignoredPayload"))

    private fun dramaJson(v: DramaState) = JSONObject().put("tension", v.tension).put("recoveryNeed", v.recoveryNeed)
        .put("personalPressure", v.personalPressure).put("worldPressure", v.worldPressure)
        .put("relationshipPressure", v.relationshipPressure).put("recentMajorEvents", stringArray(v.recentMajorEvents))
    private fun dramaFromJson(o: JSONObject) = DramaState(o.optInt("tension", 20), o.optInt("recoveryNeed"),
        o.optInt("personalPressure"), o.optInt("worldPressure"), o.optInt("relationshipPressure"),
        o.optJSONArray("recentMajorEvents").strings())

    private fun powerJson(v: PowerEvolution) = JSONObject().put("architecture", v.architecture.name)
        .put("manifestation", v.manifestation).put("branch", v.branch).put("mastery", v.mastery)
        .put("strain", v.strain).put("unlockedTechniques", stringArray(v.unlockedTechniques))
        .put("mutations", stringArray(v.mutations))
    private fun powerFromJson(o: JSONObject) = PowerEvolution(
        architecture = runCatching { PowerArchitecture.valueOf(o.optString("architecture")) }.getOrDefault(PowerArchitecture.PROJECTOR),
        manifestation = o.optString("manifestation"), branch = o.optString("branch", "Primaire"),
        mastery = o.optInt("mastery"), strain = o.optInt("strain"),
        unlockedTechniques = o.optJSONArray("unlockedTechniques").strings(), mutations = o.optJSONArray("mutations").strings()
    )

    private fun stringArray(values: Iterable<String>) = JSONArray().apply { values.forEach { put(it) } }
    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }
    private fun <T> JSONArray?.objects(mapper: (JSONObject) -> T): List<T> = if (this == null) emptyList() else buildList {
        for (i in 0 until length()) optJSONObject(i)?.let { add(mapper(it)) }
    }
    private fun JSONObject.optStringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
