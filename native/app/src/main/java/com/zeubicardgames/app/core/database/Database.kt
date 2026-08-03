package com.zeubicardgames.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owned_cards")
data class OwnedCardEntity(@PrimaryKey val canonicalId: String, val quantity: Int, val selectedVariantId: String? = null)

@Entity(tableName = "decks")
data class DeckEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val cardIdsCsv: String, val updatedAt: Long)

@Entity(tableName = "campaign")
data class CampaignEntity(@PrimaryKey val opponentId: String, val completed: Boolean, val wins: Int)

@Entity(tableName = "missions")
data class MissionEntity(@PrimaryKey val missionId: String, val progress: Int, val claimed: Boolean)

@Dao
interface GameDao {
    @Query("SELECT * FROM owned_cards") fun observeOwned(): Flow<List<OwnedCardEntity>>
    @Query("SELECT * FROM owned_cards WHERE canonicalId = :id") suspend fun owned(id: String): OwnedCardEntity?
    @Upsert suspend fun upsertOwned(entity: OwnedCardEntity)
    @Query("SELECT * FROM decks ORDER BY updatedAt DESC") fun observeDecks(): Flow<List<DeckEntity>>
    @Upsert suspend fun upsertDeck(entity: DeckEntity): Long
    @Query("SELECT * FROM campaign") fun observeCampaign(): Flow<List<CampaignEntity>>
    @Upsert suspend fun upsertCampaign(entity: CampaignEntity)
    @Query("SELECT * FROM missions") fun observeMissions(): Flow<List<MissionEntity>>
    @Upsert suspend fun upsertMission(entity: MissionEntity)
}

@Database(entities = [OwnedCardEntity::class, DeckEntity::class, CampaignEntity::class, MissionEntity::class], version = 1, exportSchema = true)
abstract class ZeubiDatabase : RoomDatabase() { abstract fun gameDao(): GameDao }
