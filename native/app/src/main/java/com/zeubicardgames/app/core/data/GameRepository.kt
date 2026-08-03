package com.zeubicardgames.app.core.data

import com.zeubicardgames.app.core.database.*
import com.zeubicardgames.app.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class GameRepository @Inject constructor(private val loader: CatalogLoader, private val dao: GameDao) {
    val catalog get() = loader.load().first
    val extensions get() = loader.load().second
    val owned: Flow<Map<String, OwnedCard>> = dao.observeOwned().map { rows -> rows.associate { it.canonicalId to OwnedCard(it.canonicalId, it.quantity, it.selectedVariantId) } }
    val decks: Flow<List<Deck>> = dao.observeDecks().map { rows -> rows.map { Deck(it.id, it.name, it.cardIdsCsv.split('|').filter(String::isNotBlank)) } }
    val campaign: Flow<Map<String, CampaignEntity>> = dao.observeCampaign().map { it.associateBy(CampaignEntity::opponentId) }

    suspend fun openPack(setId: String, seed: Long = System.nanoTime()): List<CardDefinition> {
        val pool = catalog.filter { it.setId == setId }
        if (pool.isEmpty()) return emptyList()
        val random = Random(seed)
        val pulls = MutableList(5) { pool.random(random) }
        if (pulls.none { it.rarity.rank >= Rarity.R.rank }) {
            val rare = pool.filter { it.rarity.rank >= Rarity.R.rank }.ifEmpty { pool }
            pulls[pulls.lastIndex] = rare.random(random)
        }
        pulls.forEach { card ->
            val current = dao.owned(card.canonicalId)
            dao.upsertOwned(OwnedCardEntity(card.canonicalId, (current?.quantity ?: 0) + 1, current?.selectedVariantId ?: card.variants.firstOrNull()?.variantId))
        }
        return pulls
    }

    suspend fun selectVariant(cardId: String, variantId: String) {
        val current = dao.owned(cardId) ?: return
        dao.upsertOwned(current.copy(selectedVariantId = variantId))
    }

    suspend fun saveDeck(name: String, ids: List<String>): Result<Long> {
        val validation = validateDeck(ids)
        if (validation != null) return Result.failure(IllegalArgumentException(validation))
        return Result.success(dao.upsertDeck(DeckEntity(name = name, cardIdsCsv = ids.joinToString("|"), updatedAt = System.currentTimeMillis())))
    }

    fun validateDeck(ids: List<String>): String? {
        if (ids.size != 20) return "${ids.size}/20 cartes"
        if (ids.groupingBy { it }.eachCount().values.any { it > 2 }) return "Plus de 2 exemplaires"
        val map = catalog.associateBy { it.canonicalId }
        if (ids.none { map[it]?.kind == "pokemon" && map[it]?.stage == "base" }) return "Aucun personnage de base"
        return null
    }

    suspend fun completeOpponent(id: String) {
        val current = campaignFirst(id)
        dao.upsertCampaign(CampaignEntity(id, true, (current?.wins ?: 0) + 1))
    }

    private suspend fun campaignFirst(id: String): CampaignEntity? = null
}
