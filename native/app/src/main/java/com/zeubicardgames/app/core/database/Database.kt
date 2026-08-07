package com.zeubicardgames.app.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "owned_cards")
data class OwnedCardEntity(
    @PrimaryKey val canonicalId: String,
    val quantity: Int,
    val selectedVariantId: String? = null,
)

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val cardIdsCsv: String,
    val updatedAt: Long,
)

@Entity(tableName = "campaign")
data class CampaignEntity(
    @PrimaryKey val opponentId: String,
    val completed: Boolean,
    val wins: Int,
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val missionId: String,
    val progress: Int,
    val claimed: Boolean,
)

@Entity(tableName = "pack_openings")
data class PackOpeningEntity(
    @PrimaryKey val id: String,
    val setId: String,
    val cardIdsCsv: String,
    val createdAt: Long,
    val acknowledged: Boolean = false,
)

@Dao
interface GameDao {
    @Query("SELECT * FROM owned_cards")
    fun observeOwned(): Flow<List<OwnedCardEntity>>

    @Query("SELECT * FROM owned_cards")
    suspend fun ownedSnapshot(): List<OwnedCardEntity>

    @Query("SELECT * FROM owned_cards WHERE canonicalId = :id")
    suspend fun owned(id: String): OwnedCardEntity?

    @Upsert
    suspend fun upsertOwned(entity: OwnedCardEntity)

    @Query("SELECT * FROM decks ORDER BY updatedAt DESC")
    fun observeDecks(): Flow<List<DeckEntity>>

    @Upsert
    suspend fun upsertDeck(entity: DeckEntity): Long

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteDeck(id: Long)

    @Query("SELECT * FROM campaign")
    fun observeCampaign(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaign WHERE opponentId = :id LIMIT 1")
    suspend fun campaign(id: String): CampaignEntity?

    @Upsert
    suspend fun upsertCampaign(entity: CampaignEntity)

    @Query("SELECT * FROM missions")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Upsert
    suspend fun upsertMission(entity: MissionEntity)

    @Query("SELECT * FROM pack_openings WHERE acknowledged = 0 ORDER BY createdAt ASC LIMIT 1")
    suspend fun pendingPackOpening(): PackOpeningEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPackOpening(entity: PackOpeningEntity)

    @Query("UPDATE pack_openings SET acknowledged = 1 WHERE id = :id")
    suspend fun acknowledgePackOpening(id: String)
}

@Database(
    entities = [
        OwnedCardEntity::class,
        DeckEntity::class,
        CampaignEntity::class,
        MissionEntity::class,
        PackOpeningEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ZeubiDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
