package com.zeubicardgames.app.core.data

import androidx.room.withTransaction
import com.zeubicardgames.app.core.booster.PackGenerator
import com.zeubicardgames.app.core.database.*
import com.zeubicardgames.app.core.deck.DeckRules
import com.zeubicardgames.app.core.deck.DeckValidationResult
import com.zeubicardgames.app.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PackOpeningResult(
    val id: String,
    val setId: String,
    val cards: List<CardDefinition>,
    val recovered: Boolean,
)

@Singleton
class GameRepository @Inject constructor(
    private val loader: CatalogLoader,
    private val dao: GameDao,
    private val database: ZeubiDatabase,
) {
    val catalog get() = loader.load().first
    val extensions get() = loader.load().second

    val owned: Flow<Map<String, OwnedCard>> = dao.observeOwned().map { rows ->
        rows.associate { it.canonicalId to OwnedCard(it.canonicalId, it.quantity, it.selectedVariantId) }
    }

    val decks: Flow<List<Deck>> = dao.observeDecks().map { rows ->
        rows.map { Deck(it.id, it.name, it.cardIdsCsv.split('|').filter(String::isNotBlank)) }
    }

    val campaign: Flow<Map<String, CampaignEntity>> = dao.observeCampaign().map {
        it.associateBy(CampaignEntity::opponentId)
    }

    suspend fun pendingPack(): PackOpeningResult? =
        dao.pendingPackOpening()?.let { resolveOpening(it, recovered = true) }

    suspend fun openPack(setId: String, seed: Long = System.nanoTime()): PackOpeningResult? {
        pendingPack()?.let { return it }

        val pool = catalog.filter { it.setId == setId }
        if (pool.isEmpty()) return null

        val pulls = PackGenerator.generate(pool, seed)
        val opening = PackOpeningEntity(
            id = UUID.randomUUID().toString(),
            setId = setId,
            cardIdsCsv = pulls.joinToString("|") { it.canonicalId },
            createdAt = System.currentTimeMillis(),
        )

        database.withTransaction {
            pulls.groupingBy { it.canonicalId }.eachCount().forEach { (cardId, added) ->
                val card = pulls.first { it.canonicalId == cardId }
                val current = dao.owned(cardId)
                dao.upsertOwned(
                    OwnedCardEntity(
                        canonicalId = cardId,
                        quantity = (current?.quantity ?: 0) + added,
                        selectedVariantId = current?.selectedVariantId ?: card.variants.firstOrNull()?.variantId,
                    )
                )
            }
            dao.insertPackOpening(opening)
        }

        return PackOpeningResult(opening.id, setId, pulls, recovered = false)
    }

    suspend fun acknowledgePack(id: String) {
        dao.acknowledgePackOpening(id)
    }

    suspend fun selectVariant(cardId: String, variantId: String) {
        val current = dao.owned(cardId) ?: return
        dao.upsertOwned(current.copy(selectedVariantId = variantId))
    }

    fun validateDeckDetailed(
        ids: List<String>,
        ownedQuantities: Map<String, Int>? = null,
    ): DeckValidationResult = DeckRules.validate(ids, catalog, ownedQuantities)

    fun validateDeck(ids: List<String>): String? =
        validateDeckDetailed(ids).errors.firstOrNull()

    suspend fun saveDeck(name: String, ids: List<String>): Result<Long> =
        saveDeck(id = 0L, name = name, ids = ids)

    suspend fun saveDeck(id: Long, name: String, ids: List<String>): Result<Long> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Donne un nom au deck."))
        }

        val ownedQuantities = dao.ownedSnapshot().associate { it.canonicalId to it.quantity }
        val validation = validateDeckDetailed(ids, ownedQuantities)
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.errors.joinToString("\n")))
        }

        return runCatching {
            dao.upsertDeck(
                DeckEntity(
                    id = id,
                    name = cleanName,
                    cardIdsCsv = ids.joinToString("|"),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun deleteDeck(id: Long) {
        if (id > 0) dao.deleteDeck(id)
    }

    suspend fun duplicateDeck(deck: Deck): Result<Long> =
        saveDeck(id = 0L, name = "${deck.name} — Copie", ids = deck.cardIds)

    suspend fun completeOpponent(id: String) {
        val current = dao.campaign(id)
        dao.upsertCampaign(
            CampaignEntity(
                opponentId = id,
                completed = true,
                wins = (current?.wins ?: 0) + 1,
            )
        )
    }

    private fun resolveOpening(entity: PackOpeningEntity, recovered: Boolean): PackOpeningResult? {
        val byId = catalog.associateBy { it.canonicalId }
        val cards = entity.cardIdsCsv.split('|').filter(String::isNotBlank).mapNotNull(byId::get)
        if (cards.isEmpty()) return null
        return PackOpeningResult(entity.id, entity.setId, cards, recovered)
    }
}
