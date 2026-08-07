package com.zeubicardgames.app.core.data

import android.content.Context
import com.zeubicardgames.app.core.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogLoader @Inject constructor(@ApplicationContext private val context: Context) {
    @Volatile
    private var cache: Pair<List<CardDefinition>, List<ExtensionDefinition>>? = null

    fun load(): Pair<List<CardDefinition>, List<ExtensionDefinition>> = cache ?: synchronized(this) {
        cache ?: (readCards() to readExtensions()).also { cache = it }
    }

    private fun readText(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    private fun readCards(): List<CardDefinition> {
        val root = JSONArray(readText("catalog/cards.json"))
        val parsed = (0 until root.length()).map { i ->
            val o = root.getJSONObject(i)
            val attacks = o.optJSONArray("attacks") ?: JSONArray()
            val variants = o.optJSONArray("variants") ?: JSONArray()
            val typeRaw = o.optString("type").takeIf { it.isNotBlank() }
                ?: o.optString("kind", "personnage")
            val stageRaw = o.optString("evolutionStage").takeIf { it.isNotBlank() }
                ?: o.optString("stage", "base")

            CardDefinition(
                canonicalId = o.getString("canonicalId"),
                setId = o.getString("setId"),
                number = o.optString("number"),
                name = o.getString("name"),
                kind = typeRaw,
                stage = stageRaw,
                evolvesFrom = o.optString("evolvesFrom").takeIf { it.isNotBlank() },
                hp = o.optInt("hp"),
                retreat = o.optInt("retreat"),
                rarity = Rarity.from(o.optString("rarity")),
                attacks = (0 until attacks.length()).map { j ->
                    attacks.getJSONObject(j).run {
                        Attack(getString("name"), optInt("damage"), optInt("cost"))
                    }
                },
                effect = o.optString("effect").takeIf { it.isNotBlank() },
                variants = (0 until variants.length()).map { j ->
                    variants.getJSONObject(j).run {
                        CardVariant(getString("variantId"), getString("fullPath"), getString("thumbPath"))
                    }
                },
                evolvesFromId = o.optString("evolvesFromId").takeIf { it.isNotBlank() },
                schemaVersion = o.optInt("schemaVersion", 1),
            )
        }

        val bySetAndName = parsed.groupBy { it.setId }.mapValues { (_, cards) -> cards.associateBy { it.name } }
        return parsed.map { card ->
            if (card.evolvesFromId != null || card.evolvesFrom.isNullOrBlank()) {
                card
            } else {
                card.copy(evolvesFromId = bySetAndName[card.setId]?.get(card.evolvesFrom)?.canonicalId)
            }
        }
    }

    private fun readExtensions(): List<ExtensionDefinition> {
        val root = JSONArray(readText("catalog/extensions.json"))
        return (0 until root.length()).map { i ->
            root.getJSONObject(i).run {
                ExtensionDefinition(
                    id = getString("id"),
                    name = getString("name"),
                    subtitle = getString("subtitle"),
                    accent = getLong("accent"),
                    boosterPath = getString("boosterPath"),
                    cardCount = getInt("cardCount"),
                    schemaVersion = optInt("schemaVersion", 1),
                    order = optInt("order", i),
                    status = runCatching { ContentStatus.valueOf(optString("status", "ACTIVE").uppercase()) }
                        .getOrDefault(ContentStatus.ACTIVE),
                )
            }
        }.filter { it.status == ContentStatus.ACTIVE }
    }
}
