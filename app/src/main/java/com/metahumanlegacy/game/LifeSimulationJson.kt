package com.metahumanlegacy.game

import org.json.JSONArray
import org.json.JSONObject

/** JSON codec kept separate so DeepLifePersistence remains the single save entry point. */
internal object LifeSimulationJson {
    fun encode(state: LifeSimulationState): JSONObject = JSONObject().apply {
        put("schemaVersion", state.schemaVersion)
        put("calendarYear", state.calendarYear)
        put("civil", JSONObject().apply {
            put("employment", state.civil.employment.name)
            put("jobTitle", state.civil.jobTitle)
            put("monthlyIncome", state.civil.monthlyIncome)
            put("savings", state.civil.savings)
            put("housing", state.civil.housing.name)
            put("housingCost", state.civil.housingCost)
            put("stress", state.civil.stress)
            put("freeMoments", state.civil.freeMoments)
            put("education", state.civil.education)
            put("careerProgress", state.civil.careerProgress)
        })
        put("relationshipLives", JSONArray().apply {
            state.relationshipLives.forEach { rel ->
                put(JSONObject().apply {
                    put("personId", rel.personId)
                    put("bond", rel.bond.name)
                    put("attraction", rel.attraction)
                    put("closeness", rel.closeness)
                    put("availability", rel.availability)
                    put("secretKnowledge", rel.secretKnowledge.name)
                    put("sharedSecrets", strings(rel.sharedSecrets))
                    put("promises", strings(rel.promises))
                    put("lastContactTurn", rel.lastContactTurn)
                })
            }
        })
        put("districts", JSONArray().apply {
            state.districts.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("safety", d.safety)
                    put("damage", d.damage)
                    put("localTrust", d.localTrust)
                    put("criminalControl", d.criminalControl)
                    put("mediaHeat", d.mediaHeat)
                    put("unresolvedThreats", strings(d.unresolvedThreats))
                })
            }
        })
        put("powerRules", JSONObject().apply {
            put("range", state.powerRules.range)
            put("precision", state.powerRules.precision)
            put("maxLoad", state.powerRules.maxLoad)
            put("control", state.powerRules.control)
            put("fatigue", state.powerRules.fatigue)
            put("overload", state.powerRules.overload)
            put("techniques", JSONArray().apply {
                state.powerRules.techniques.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id); put("name", t.name); put("masteryRequired", t.masteryRequired)
                        put("unlocked", t.unlocked); put("proficiency", t.proficiency)
                        put("cooldownTurns", t.cooldownTurns); put("lastUsedTurn", t.lastUsedTurn)
                    })
                }
            })
        })
        put("secretIdentity", JSONObject().apply {
            put("exposure", state.secretIdentity.exposure)
            put("activeRumors", strings(state.secretIdentity.activeRumors))
            put("evidenceIds", strings(state.secretIdentity.evidenceIds))
            put("knownBy", JSONObject().apply {
                state.secretIdentity.knownBy.forEach { (id, knowledge) -> put(id, knowledge.name) }
            })
        })
        put("actionLog", strings(state.actionLog))
    }

    fun decode(root: JSONObject): LifeSimulationState {
        val civil = root.optJSONObject("civil") ?: JSONObject()
        val power = root.optJSONObject("powerRules") ?: JSONObject()
        val secret = root.optJSONObject("secretIdentity") ?: JSONObject()
        val known = buildMap {
            val knownJson = secret.optJSONObject("knownBy") ?: JSONObject()
            val keys = knownJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, enumValue(knownJson.optString(key), SecretKnowledge.UNAWARE))
            }
        }
        return LifeSimulationState(
            schemaVersion = root.optInt("schemaVersion", 1),
            civil = CivilLifeState(
                employment = enumValue(civil.optString("employment"), EmploymentStatus.STUDENT),
                jobTitle = civil.optString("jobTitle", "Élève"),
                monthlyIncome = civil.optInt("monthlyIncome"),
                savings = civil.optInt("savings"),
                housing = enumValue(civil.optString("housing"), HousingTier.FAMILY_HOME),
                housingCost = civil.optInt("housingCost"),
                stress = civil.optInt("stress", 10),
                freeMoments = civil.optInt("freeMoments", 3),
                education = civil.optInt("education"),
                careerProgress = civil.optInt("careerProgress")
            ),
            relationshipLives = root.optJSONArray("relationshipLives").objects { o ->
                RelationshipLifeState(
                    personId = o.optString("personId"),
                    bond = enumValue(o.optString("bond"), BondStatus.NONE),
                    attraction = o.optInt("attraction"),
                    closeness = o.optInt("closeness"),
                    availability = o.optInt("availability", 100),
                    secretKnowledge = enumValue(o.optString("secretKnowledge"), SecretKnowledge.UNAWARE),
                    sharedSecrets = o.optJSONArray("sharedSecrets").strings(),
                    promises = o.optJSONArray("promises").strings(),
                    lastContactTurn = o.optInt("lastContactTurn", -1)
                )
            },
            districts = root.optJSONArray("districts").objects { o ->
                DistrictLifeState(
                    id = o.optString("id"), safety = o.optInt("safety", 50), damage = o.optInt("damage"),
                    localTrust = o.optInt("localTrust"), criminalControl = o.optInt("criminalControl", 20),
                    mediaHeat = o.optInt("mediaHeat"), unresolvedThreats = o.optJSONArray("unresolvedThreats").strings()
                )
            },
            powerRules = PowerRulesState(
                range = power.optInt("range", 1), precision = power.optInt("precision", 10),
                maxLoad = power.optInt("maxLoad", 1), control = power.optInt("control", 10),
                fatigue = power.optInt("fatigue"), overload = power.optInt("overload"),
                techniques = power.optJSONArray("techniques").objects { o ->
                    TechniqueState(
                        id = o.optString("id"), name = o.optString("name"), masteryRequired = o.optInt("masteryRequired"),
                        unlocked = o.optBoolean("unlocked"), proficiency = o.optInt("proficiency"),
                        cooldownTurns = o.optInt("cooldownTurns"), lastUsedTurn = o.optInt("lastUsedTurn", -100)
                    )
                }
            ),
            secretIdentity = IdentitySecretState(
                exposure = secret.optInt("exposure"), activeRumors = secret.optJSONArray("activeRumors").strings(),
                knownBy = known, evidenceIds = secret.optJSONArray("evidenceIds").strings()
            ),
            calendarYear = root.optInt("calendarYear"),
            actionLog = root.optJSONArray("actionLog").strings()
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, fallback: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    private fun strings(values: Iterable<String>) = JSONArray().apply { values.forEach { put(it) } }
    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { optString(it) }
    private fun <T> JSONArray?.objects(mapper: (JSONObject) -> T): List<T> = if (this == null) emptyList() else buildList {
        for (i in 0 until length()) optJSONObject(i)?.let { add(mapper(it)) }
    }
}
