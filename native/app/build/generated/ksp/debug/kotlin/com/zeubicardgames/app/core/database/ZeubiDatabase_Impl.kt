package com.zeubicardgames.app.core.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ZeubiDatabase_Impl : ZeubiDatabase() {
  private val _gameDao: Lazy<GameDao> = lazy {
    GameDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "491f5e0ea4c88131b1922431f525a49f", "2de318dac79628757663b554f9ddf225") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `owned_cards` (`canonicalId` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `selectedVariantId` TEXT, PRIMARY KEY(`canonicalId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `decks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `cardIdsCsv` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `campaign` (`opponentId` TEXT NOT NULL, `completed` INTEGER NOT NULL, `wins` INTEGER NOT NULL, PRIMARY KEY(`opponentId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `missions` (`missionId` TEXT NOT NULL, `progress` INTEGER NOT NULL, `claimed` INTEGER NOT NULL, PRIMARY KEY(`missionId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '491f5e0ea4c88131b1922431f525a49f')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `owned_cards`")
        connection.execSQL("DROP TABLE IF EXISTS `decks`")
        connection.execSQL("DROP TABLE IF EXISTS `campaign`")
        connection.execSQL("DROP TABLE IF EXISTS `missions`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsOwnedCards: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsOwnedCards.put("canonicalId", TableInfo.Column("canonicalId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOwnedCards.put("quantity", TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsOwnedCards.put("selectedVariantId", TableInfo.Column("selectedVariantId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysOwnedCards: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesOwnedCards: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoOwnedCards: TableInfo = TableInfo("owned_cards", _columnsOwnedCards, _foreignKeysOwnedCards, _indicesOwnedCards)
        val _existingOwnedCards: TableInfo = read(connection, "owned_cards")
        if (!_infoOwnedCards.equals(_existingOwnedCards)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |owned_cards(com.zeubicardgames.app.core.database.OwnedCardEntity).
              | Expected:
              |""".trimMargin() + _infoOwnedCards + """
              |
              | Found:
              |""".trimMargin() + _existingOwnedCards)
        }
        val _columnsDecks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDecks.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDecks.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDecks.put("cardIdsCsv", TableInfo.Column("cardIdsCsv", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsDecks.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDecks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDecks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoDecks: TableInfo = TableInfo("decks", _columnsDecks, _foreignKeysDecks, _indicesDecks)
        val _existingDecks: TableInfo = read(connection, "decks")
        if (!_infoDecks.equals(_existingDecks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |decks(com.zeubicardgames.app.core.database.DeckEntity).
              | Expected:
              |""".trimMargin() + _infoDecks + """
              |
              | Found:
              |""".trimMargin() + _existingDecks)
        }
        val _columnsCampaign: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCampaign.put("opponentId", TableInfo.Column("opponentId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCampaign.put("completed", TableInfo.Column("completed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCampaign.put("wins", TableInfo.Column("wins", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCampaign: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCampaign: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCampaign: TableInfo = TableInfo("campaign", _columnsCampaign, _foreignKeysCampaign, _indicesCampaign)
        val _existingCampaign: TableInfo = read(connection, "campaign")
        if (!_infoCampaign.equals(_existingCampaign)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |campaign(com.zeubicardgames.app.core.database.CampaignEntity).
              | Expected:
              |""".trimMargin() + _infoCampaign + """
              |
              | Found:
              |""".trimMargin() + _existingCampaign)
        }
        val _columnsMissions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMissions.put("missionId", TableInfo.Column("missionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMissions.put("progress", TableInfo.Column("progress", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMissions.put("claimed", TableInfo.Column("claimed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMissions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMissions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMissions: TableInfo = TableInfo("missions", _columnsMissions, _foreignKeysMissions, _indicesMissions)
        val _existingMissions: TableInfo = read(connection, "missions")
        if (!_infoMissions.equals(_existingMissions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |missions(com.zeubicardgames.app.core.database.MissionEntity).
              | Expected:
              |""".trimMargin() + _infoMissions + """
              |
              | Found:
              |""".trimMargin() + _existingMissions)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "owned_cards", "decks", "campaign", "missions")
  }

  public override fun clearAllTables() {
    super.performClear(false, "owned_cards", "decks", "campaign", "missions")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(GameDao::class, GameDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun gameDao(): GameDao = _gameDao.value
}
