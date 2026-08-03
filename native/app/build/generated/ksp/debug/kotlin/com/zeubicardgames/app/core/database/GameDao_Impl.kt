package com.zeubicardgames.app.core.database

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.EntityUpsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class GameDao_Impl(
  __db: RoomDatabase,
) : GameDao {
  private val __db: RoomDatabase

  private val __upsertAdapterOfOwnedCardEntity: EntityUpsertAdapter<OwnedCardEntity>

  private val __upsertAdapterOfDeckEntity: EntityUpsertAdapter<DeckEntity>

  private val __upsertAdapterOfCampaignEntity: EntityUpsertAdapter<CampaignEntity>

  private val __upsertAdapterOfMissionEntity: EntityUpsertAdapter<MissionEntity>
  init {
    this.__db = __db
    this.__upsertAdapterOfOwnedCardEntity = EntityUpsertAdapter<OwnedCardEntity>(object : EntityInsertAdapter<OwnedCardEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `owned_cards` (`canonicalId`,`quantity`,`selectedVariantId`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: OwnedCardEntity) {
        statement.bindText(1, entity.canonicalId)
        statement.bindLong(2, entity.quantity.toLong())
        val _tmpSelectedVariantId: String? = entity.selectedVariantId
        if (_tmpSelectedVariantId == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpSelectedVariantId)
        }
      }
    }, object : EntityDeleteOrUpdateAdapter<OwnedCardEntity>() {
      protected override fun createQuery(): String = "UPDATE `owned_cards` SET `canonicalId` = ?,`quantity` = ?,`selectedVariantId` = ? WHERE `canonicalId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: OwnedCardEntity) {
        statement.bindText(1, entity.canonicalId)
        statement.bindLong(2, entity.quantity.toLong())
        val _tmpSelectedVariantId: String? = entity.selectedVariantId
        if (_tmpSelectedVariantId == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpSelectedVariantId)
        }
        statement.bindText(4, entity.canonicalId)
      }
    })
    this.__upsertAdapterOfDeckEntity = EntityUpsertAdapter<DeckEntity>(object : EntityInsertAdapter<DeckEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `decks` (`id`,`name`,`cardIdsCsv`,`updatedAt`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DeckEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.cardIdsCsv)
        statement.bindLong(4, entity.updatedAt)
      }
    }, object : EntityDeleteOrUpdateAdapter<DeckEntity>() {
      protected override fun createQuery(): String = "UPDATE `decks` SET `id` = ?,`name` = ?,`cardIdsCsv` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DeckEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.cardIdsCsv)
        statement.bindLong(4, entity.updatedAt)
        statement.bindLong(5, entity.id)
      }
    })
    this.__upsertAdapterOfCampaignEntity = EntityUpsertAdapter<CampaignEntity>(object : EntityInsertAdapter<CampaignEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `campaign` (`opponentId`,`completed`,`wins`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CampaignEntity) {
        statement.bindText(1, entity.opponentId)
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        statement.bindLong(3, entity.wins.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<CampaignEntity>() {
      protected override fun createQuery(): String = "UPDATE `campaign` SET `opponentId` = ?,`completed` = ?,`wins` = ? WHERE `opponentId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CampaignEntity) {
        statement.bindText(1, entity.opponentId)
        val _tmp: Int = if (entity.completed) 1 else 0
        statement.bindLong(2, _tmp.toLong())
        statement.bindLong(3, entity.wins.toLong())
        statement.bindText(4, entity.opponentId)
      }
    })
    this.__upsertAdapterOfMissionEntity = EntityUpsertAdapter<MissionEntity>(object : EntityInsertAdapter<MissionEntity>() {
      protected override fun createQuery(): String = "INSERT INTO `missions` (`missionId`,`progress`,`claimed`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MissionEntity) {
        statement.bindText(1, entity.missionId)
        statement.bindLong(2, entity.progress.toLong())
        val _tmp: Int = if (entity.claimed) 1 else 0
        statement.bindLong(3, _tmp.toLong())
      }
    }, object : EntityDeleteOrUpdateAdapter<MissionEntity>() {
      protected override fun createQuery(): String = "UPDATE `missions` SET `missionId` = ?,`progress` = ?,`claimed` = ? WHERE `missionId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MissionEntity) {
        statement.bindText(1, entity.missionId)
        statement.bindLong(2, entity.progress.toLong())
        val _tmp: Int = if (entity.claimed) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        statement.bindText(4, entity.missionId)
      }
    })
  }

  public override suspend fun upsertOwned(entity: OwnedCardEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfOwnedCardEntity.upsert(_connection, entity)
  }

  public override suspend fun upsertDeck(entity: DeckEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __upsertAdapterOfDeckEntity.upsertAndReturnId(_connection, entity)
    _result
  }

  public override suspend fun upsertCampaign(entity: CampaignEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfCampaignEntity.upsert(_connection, entity)
  }

  public override suspend fun upsertMission(entity: MissionEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __upsertAdapterOfMissionEntity.upsert(_connection, entity)
  }

  public override fun observeOwned(): Flow<List<OwnedCardEntity>> {
    val _sql: String = "SELECT * FROM owned_cards"
    return createFlow(__db, false, arrayOf("owned_cards")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCanonicalId: Int = getColumnIndexOrThrow(_stmt, "canonicalId")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfSelectedVariantId: Int = getColumnIndexOrThrow(_stmt, "selectedVariantId")
        val _result: MutableList<OwnedCardEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: OwnedCardEntity
          val _tmpCanonicalId: String
          _tmpCanonicalId = _stmt.getText(_columnIndexOfCanonicalId)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpSelectedVariantId: String?
          if (_stmt.isNull(_columnIndexOfSelectedVariantId)) {
            _tmpSelectedVariantId = null
          } else {
            _tmpSelectedVariantId = _stmt.getText(_columnIndexOfSelectedVariantId)
          }
          _item = OwnedCardEntity(_tmpCanonicalId,_tmpQuantity,_tmpSelectedVariantId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun owned(id: String): OwnedCardEntity? {
    val _sql: String = "SELECT * FROM owned_cards WHERE canonicalId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfCanonicalId: Int = getColumnIndexOrThrow(_stmt, "canonicalId")
        val _columnIndexOfQuantity: Int = getColumnIndexOrThrow(_stmt, "quantity")
        val _columnIndexOfSelectedVariantId: Int = getColumnIndexOrThrow(_stmt, "selectedVariantId")
        val _result: OwnedCardEntity?
        if (_stmt.step()) {
          val _tmpCanonicalId: String
          _tmpCanonicalId = _stmt.getText(_columnIndexOfCanonicalId)
          val _tmpQuantity: Int
          _tmpQuantity = _stmt.getLong(_columnIndexOfQuantity).toInt()
          val _tmpSelectedVariantId: String?
          if (_stmt.isNull(_columnIndexOfSelectedVariantId)) {
            _tmpSelectedVariantId = null
          } else {
            _tmpSelectedVariantId = _stmt.getText(_columnIndexOfSelectedVariantId)
          }
          _result = OwnedCardEntity(_tmpCanonicalId,_tmpQuantity,_tmpSelectedVariantId)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeDecks(): Flow<List<DeckEntity>> {
    val _sql: String = "SELECT * FROM decks ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("decks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfCardIdsCsv: Int = getColumnIndexOrThrow(_stmt, "cardIdsCsv")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<DeckEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DeckEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpCardIdsCsv: String
          _tmpCardIdsCsv = _stmt.getText(_columnIndexOfCardIdsCsv)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = DeckEntity(_tmpId,_tmpName,_tmpCardIdsCsv,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeCampaign(): Flow<List<CampaignEntity>> {
    val _sql: String = "SELECT * FROM campaign"
    return createFlow(__db, false, arrayOf("campaign")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfOpponentId: Int = getColumnIndexOrThrow(_stmt, "opponentId")
        val _columnIndexOfCompleted: Int = getColumnIndexOrThrow(_stmt, "completed")
        val _columnIndexOfWins: Int = getColumnIndexOrThrow(_stmt, "wins")
        val _result: MutableList<CampaignEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CampaignEntity
          val _tmpOpponentId: String
          _tmpOpponentId = _stmt.getText(_columnIndexOfOpponentId)
          val _tmpCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfCompleted).toInt()
          _tmpCompleted = _tmp != 0
          val _tmpWins: Int
          _tmpWins = _stmt.getLong(_columnIndexOfWins).toInt()
          _item = CampaignEntity(_tmpOpponentId,_tmpCompleted,_tmpWins)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeMissions(): Flow<List<MissionEntity>> {
    val _sql: String = "SELECT * FROM missions"
    return createFlow(__db, false, arrayOf("missions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfMissionId: Int = getColumnIndexOrThrow(_stmt, "missionId")
        val _columnIndexOfProgress: Int = getColumnIndexOrThrow(_stmt, "progress")
        val _columnIndexOfClaimed: Int = getColumnIndexOrThrow(_stmt, "claimed")
        val _result: MutableList<MissionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MissionEntity
          val _tmpMissionId: String
          _tmpMissionId = _stmt.getText(_columnIndexOfMissionId)
          val _tmpProgress: Int
          _tmpProgress = _stmt.getLong(_columnIndexOfProgress).toInt()
          val _tmpClaimed: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfClaimed).toInt()
          _tmpClaimed = _tmp != 0
          _item = MissionEntity(_tmpMissionId,_tmpProgress,_tmpClaimed)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
