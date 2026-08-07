package com.zeubicardgames.app.core.booster

import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.Rarity
import kotlin.random.Random

object PackGenerator {
    const val DEFAULT_CARD_COUNT = 5

    fun generate(
        pool: List<CardDefinition>,
        seed: Long,
        cardCount: Int = DEFAULT_CARD_COUNT,
    ): List<CardDefinition> {
        require(pool.isNotEmpty()) { "Le pool du booster ne peut pas être vide" }
        require(cardCount > 0) { "Le nombre de cartes doit être positif" }

        val random = Random(seed)
        val pulls = MutableList(cardCount) { pool.random(random) }

        if (pulls.none { it.rarity.rank >= Rarity.R.rank }) {
            val rarePool = pool.filter { it.rarity.rank >= Rarity.R.rank }.ifEmpty { pool }
            pulls[pulls.lastIndex] = rarePool.random(random)
        }

        return pulls
    }
}
